package com.jing.cloud.proxy.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.forward.server.ForwardServer;
import com.jing.cloud.proxy.handler.ServerHandler;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class NettyConfig {
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean("onlineProxyClient")
	public Map<String,Channel> getOnlineProxyClient(){
		return Collections.synchronizedMap(new HashMap<String,Channel>());
	}
	
	@Bean("appHandlerMap")
	public Map<String,ForwardHandler> appHandlerMap(){
		return Collections.synchronizedMap(new HashMap<String,ForwardHandler>());
	}
	
	@Bean("forwardServerMap")
	public Map<Integer,ForwardServer> getForwardServer(){
		return Collections.synchronizedMap(new HashMap<Integer,ForwardServer>());
	}
	
	@Bean("serverHandlerMap")
	public Map<String,ServerHandler> getServerHandler(){
		return Collections.synchronizedMap(new HashMap<String,ServerHandler>());
	}
}
