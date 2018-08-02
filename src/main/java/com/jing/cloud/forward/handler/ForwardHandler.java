package com.jing.cloud.forward.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.RandomUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ForwardHandler.class);
	
	private ChannelGroup channelGroup;
	
	private String token;
	
	public ForwardHandler(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("用户客户端与服务端连接开始...");
		token = RandomUtil.GetGuid32();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("用户客户端与服务端连接关闭...");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		Message message = new Message(token, MessageCode.TRANSFER_DATA, data);
		logger.info("server : " + message);
		channelGroup.writeAndFlush(message);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		logger.info("接收用户客户端信息完毕...");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage());
	}
	
}
