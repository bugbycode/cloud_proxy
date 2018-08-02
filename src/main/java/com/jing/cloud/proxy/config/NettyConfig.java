package com.jing.cloud.proxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class NettyConfig {
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
}
