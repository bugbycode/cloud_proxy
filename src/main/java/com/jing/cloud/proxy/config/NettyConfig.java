package com.jing.cloud.proxy.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.util.pool.WaitConnectionThreadPool;

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
	
	@Bean("onlineUserClient")
	public Map<String,Channel> getOnlineUserClient(){
		return Collections.synchronizedMap(new HashMap<String,Channel>());
	}
	
	@Bean("connectionMap")
	public Map<String,WaitConnectionThreadPool> getWaitConnectionThreadPool(){
		return Collections.synchronizedMap(new HashMap<String,WaitConnectionThreadPool>());
	}
	
	
}
