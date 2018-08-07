package com.jing.cloud.proxy.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jing.cloud.forward.handler.ForwardHandler;

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
	
}
