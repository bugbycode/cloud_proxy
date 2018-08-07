package com.jing.cloud.forward.handler;

import java.util.LinkedList;
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
	
	private Channel proxyChannel;

	private Map<String, ForwardHandler> appHandlerMap;
	
	private String token;
	
	private LinkedList<Message> queue;
	
	private boolean isClosed = true;
	
	private String host = "";
	
	private int port;
	
	public ForwardHandler(String host,int port,Channel proxyChannel, Map<String, ForwardHandler> appHandlerMap) {
		this.proxyChannel = proxyChannel;
		this.appHandlerMap = appHandlerMap;
		this.token = RandomUtil.GetGuid32();
		this.queue = new LinkedList<Message>();
		this.host = host;
		this.port = port;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		Message message = new Message();
		message.setType(MessageCode.TRANSFER_DATA);
		message.setToken(token);
		
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		message.setData(data);
		
		proxyChannel.writeAndFlush(message);
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	public synchronized void sendMessage(Message msg) {
		queue.addLast(msg);
		notifyTask();
	}
	
	private synchronized Message read() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("User client closed." + token);
			}
		}
		return queue.removeFirst();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		isClosed = false;
		appHandlerMap.put(token, this);
		//开始连接目标设备
		Message message = new Message();
		message.setType(MessageCode.CONNECTION);
		message.setToken(token);
		ConnectionInfo conn = new ConnectionInfo(host, port);
		message.setData(conn);
		proxyChannel.writeAndFlush(message);
		//等待连接结果
		Message msg = read();
		
		logger.info("连接结果 " + msg);
		
		if(msg.getType() == MessageCode.CONNECTION_ERROR || 
				msg.getType() == MessageCode.CLOSE_CONNECTION) {
			ctx.close();
		}else {
			new WorkThread(ctx).start();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//断开连接
		Message message = new Message();
		message.setType(MessageCode.CLOSE_CONNECTION);
		message.setToken(token);
		proxyChannel.writeAndFlush(message);
		
		appHandlerMap.remove(token);
		isClosed = true;
		notifyTask();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}

	private class WorkThread extends Thread {
		
		private ChannelHandlerContext ctx;
		
		public WorkThread(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}
		
		@Override
		public void run() {
			Channel channel = ctx.channel();
			try {
				while(!isClosed) {
					Message msg = read();
					logger.info("transfer " + msg);
					if(msg.getType() == MessageCode.CLOSE_CONNECTION) {
						continue;
					}
					
					if(msg.getType() != MessageCode.TRANSFER_DATA) {
						continue;
					}
					
					byte[] data = (byte[]) msg.getData();
					ByteBuf buff = ctx.alloc().buffer(data.length);
					buff.writeBytes(data);
					channel.writeAndFlush(buff);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
