package com.jing.cloud.proxy.handler;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.Authentication;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.pool.WaitConnectionThreadPool;

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

	private Map<String, Channel> onlineProxyClient;
	
	private Map<String,WaitConnectionThreadPool> connectionMap;
	
	public ServerHandler(ChannelGroup channelGroup, Map<String, Channel> onlineProxyClient,
			Map<String,WaitConnectionThreadPool> connectionMap) {
		this.channelGroup = channelGroup;
		this.onlineProxyClient = onlineProxyClient;
		this.connectionMap = connectionMap;
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
			
			String clientId = authInfo.getClientId();
			
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
			onlineProxyClient.put(clientId, channel);
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
		
		if(type == MessageCode.CONNECTION_SUCCESS ||
				type == MessageCode.CONNECTION_ERROR) {
			WaitConnectionThreadPool wct = connectionMap.get(message.getToken());
			if(wct == null) {
				return;
			}
			wct.sendMessage(message);
		}
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

	private String findClientId(Channel channel) {
		synchronized (onlineProxyClient) {
			Set<String> keySet = onlineProxyClient.keySet();
			Iterator<String> it = keySet.iterator();
			while(it.hasNext()) {
				String key = it.next();
				Channel client = onlineProxyClient.get(key);
				if(client == channel) {
					return key;
				}
			}
		}
		return null;
	}
}
