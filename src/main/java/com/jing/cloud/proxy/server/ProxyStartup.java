package com.jing.cloud.proxy.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.jing.cloud.proxy.thread.StartupRunnable;

import io.netty.channel.ChannelHandler;

@Component
@Configuration
public class ProxyStartup implements ApplicationRunner {

	@Value("${spring.netty.port}")
	private int serverPort;
	
	@Value("${spring.netty.so_backlog}")
	private int so_backlog;
	
	@Autowired
	private ChannelHandler serverChannelInitializer;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		StartupRunnable startup = new StartupRunnable(serverPort, so_backlog, serverChannelInitializer);
		new Thread(startup).start();
	}

}
