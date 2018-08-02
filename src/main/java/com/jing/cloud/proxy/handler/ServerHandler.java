package com.jing.cloud.proxy.handler;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private ChannelGroup channelGroup;

	public ServerHandler(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("客户端与服务端连接开始...");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channelGroup.remove(ctx.channel());
		logger.info("客户端与服务端连接关闭...");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;

		Channel channel = ctx.channel();
		
		logger.info(msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		logger.info("信息接收完毕...");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			// 服务端对应着读事件，当为READER_IDLE时触发
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				logger.info("接收消息超时");
				if (loss_connect_time > 2) {
					logger.info("关闭不活动的链接");
					ctx.channel().close();
				}
			} else {
				super.userEventTriggered(ctx, evt);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		logger.error(cause.getMessage());
	}

	
}
