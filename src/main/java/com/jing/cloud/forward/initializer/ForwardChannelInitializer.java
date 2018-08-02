package com.jing.cloud.forward.initializer;

import com.jing.cloud.forward.handler.ForwardHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;

public class ForwardChannelInitializer extends ChannelInitializer<SocketChannel> {

	private ChannelGroup channelGroup;
	
	public ForwardChannelInitializer(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		// 增加任务处理
		ChannelPipeline p = sc.pipeline();
		p.addLast(new ForwardHandler(channelGroup));
	}

}
