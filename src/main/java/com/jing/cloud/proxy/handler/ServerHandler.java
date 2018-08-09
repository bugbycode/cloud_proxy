package com.jing.cloud.proxy.handler;

import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.forward.server.ForwardServer;
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

	private Map<String, Channel> onlineProxyClient;
	
	public Map<String,ForwardHandler> appHandlerMap;
	
	private String clientId = "";
	
	private LinkedList<ForwardServer> queue;
	
	public Map<String,ServerHandler> serverHandlerMap;

	public ServerHandler(ChannelGroup channelGroup, 
			Map<String, Channel> onlineProxyClient,
			Map<String,ForwardHandler> appHandlerMap,
			Map<String,ServerHandler> serverHandlerMap) {
		this.channelGroup = channelGroup;
		this.onlineProxyClient = onlineProxyClient;
		this.appHandlerMap = appHandlerMap;
		this.queue = new LinkedList<ForwardServer>();
		this.serverHandlerMap = serverHandlerMap;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.info("Agent connection...");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channelGroup.remove(ctx.channel());
		onlineProxyClient.remove(this.clientId);
		
		while(!queue.isEmpty()) {
			queue.removeFirst().close();
		}
		
		logger.info("Agent connection closed... " + clientId);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		if(type == MessageCode.REGISTER) {
			if(data == null || !(data instanceof Authentication)) {
				ctx.close();
				return;
			}
			Authentication authInfo = (Authentication)data;
			
			String clientId = authInfo.getClientId();
			
			Channel clientChannel = onlineProxyClient.get(clientId);
			
			if(!(clientChannel == null)) {
				message.setType(MessageCode.REGISTER_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			
			this.clientId = clientId;
			
			message.setType(MessageCode.REGISTER_SUCCESS);
			message.setData(null);
			
			channel.writeAndFlush(message);
			onlineProxyClient.put(clientId, channel);
			channelGroup.add(channel);
			serverHandlerMap.put(clientId, this);
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
		
		if(type == MessageCode.CONNECTION_ERROR || type == MessageCode.CONNECTION_SUCCESS ||
				type == MessageCode.CLOSE_CONNECTION || type == MessageCode.TRANSFER_DATA) {
			ForwardHandler forward = appHandlerMap.get(token);
			if(forward != null) {
				forward.sendMessage(message);
			}
			return;
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				logger.info("Read heartbeat timeout.");
				if (loss_connect_time > 2) {
					logger.info("Channel timeout.");
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
	
	public void addForwardServer(ForwardServer server) {
		queue.add(server);
	}
	
	public void removeForwardServer(ForwardServer server) {
		queue.remove(server);
	}
}
