package com.jing.cloud.proxy.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Authentication;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

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
		logger.info("�ͻ������������ӿ�ʼ...");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channelGroup.remove(ctx.channel());
		logger.info("�ͻ������������ӹر�...");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		logger.info(msg);
		int type = message.getType();
		Object data = message.getData();
		if(type == MessageCode.REGISTER) {
			if(data == null || !(data instanceof Authentication)) {
				ctx.close();
				return;
			}
			Authentication authInfo = (Authentication)data;
			if(!("fort".equals(authInfo.getClientId()) 
					&& "fort".equals(authInfo.getSecret()))) {
				message.setType(MessageCode.REGISTER_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			message.setType(MessageCode.REGISTER_SUCCESS);
			message.setData(null);
			
			channel.writeAndFlush(message);
			
			channelGroup.add(channel);
			return;
		}
		
		channel = channelGroup.find(channel.id());
		if(channel == null) {
			ctx.close();
			return;
		}
		
		if(type == MessageCode.HEARTBEAT) {
			return;
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		logger.info("��Ϣ�������...");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			// ����˶�Ӧ�Ŷ��¼�����ΪREADER_IDLEʱ����
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				logger.info("������Ϣ��ʱ");
				if (loss_connect_time > 2) {
					logger.info("�رղ��������");
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
