package com.jing.cloud.forward.handler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.module.ConnectionInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.RandomUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ForwardHandler.class);
	
	private Channel agentChannel;
	
	private String token;
	
	private String host;	//����Ŀ������
	
	private int port; 		//����Ŀ�������˿ں�
	
	private Map<String,Channel> onlineUserClient;

	public ForwardHandler(Channel agentChannel,String host,int port,
			Map<String,Channel> onlineUserClient) {
		this.agentChannel = agentChannel;
		this.host = host;
		this.port = port;
		this.token = RandomUtil.GetGuid32();
		this.onlineUserClient = onlineUserClient;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//logger.info("�û��ͻ������������ӿ�ʼ...");
		ConnectionInfo conn = new ConnectionInfo(host, port);
		Message message = new Message();
		message.setToken(token);
		message.setType(MessageCode.CONNECTION);
		message.setData(conn);
		agentChannel.writeAndFlush(message);
		onlineUserClient.put(token, ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//logger.info("�û��ͻ������������ӹر�...");
		Message message = new Message();
		message.setToken(token);
		message.setType(MessageCode.CLOSE_CONNECTION);
		agentChannel.writeAndFlush(message);
		onlineUserClient.remove(token);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		logger.info("user data " + data);
		Message message = new Message(token, MessageCode.TRANSFER_DATA, data);
		agentChannel.writeAndFlush(message);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		//logger.info("�����û��ͻ�����Ϣ���...");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getLocalizedMessage());
		ctx.close();
	}
	
}
