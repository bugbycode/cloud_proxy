package com.jing.cloud.forward.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.jing.cloud.forward.initializer.ForwardChannelInitializer;
import com.jing.cloud.forward.thread.ForwardStartupRunnable;

import io.netty.channel.group.ChannelGroup;

@Component
public class ForwardServer implements ApplicationRunner {

	@Autowired
	private ChannelGroup channelGroup;
	
	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		ForwardStartupRunnable run = new ForwardStartupRunnable(5001, 2, new ForwardChannelInitializer(channelGroup));
		new Thread(run).start();
	}

}
