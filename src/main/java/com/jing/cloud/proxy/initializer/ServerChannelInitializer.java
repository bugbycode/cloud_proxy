package com.jing.cloud.proxy.initializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jing.cloud.config.IdleConfig;
import com.jing.cloud.handler.MessageDecoder;
import com.jing.cloud.handler.MessageEncoder;
import com.jing.cloud.proxy.handler.ServerHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

@Service("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ChannelGroup channelGroup;
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		// 增加任务处理
		ChannelPipeline p = sc.pipeline();
		p.addLast(
				new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
				new MessageDecoder(),
				new MessageEncoder(),
				new ServerHandler(channelGroup)
		);
	}

}
