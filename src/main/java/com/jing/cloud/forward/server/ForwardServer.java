package com.jing.cloud.forward.server;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.proxy.handler.ServerHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ForwardServer implements Runnable {

	private final Logger logger = LogManager.getLogger(ForwardServer.class);
	
	private int so_backlog = 50;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Channel proxyChannel;
	
	private String clientId = "";
	
	private Map<String,ForwardHandler> appHandlerMap;
	
	private Map<Integer,ForwardServer> forwardServerMap;
	
	public Map<String,ServerHandler> serverHandlerMap;
	
	private String host = "";
	
	private int port;
	
	private int proxyPort;
	
	private boolean closeApp = true;
	
	private boolean isFinish = false;
	
	private boolean isOpen = false;
	
	private ChannelGroup onlineChannel;
	
	public ForwardServer(String host,int port,int proxyPort,
			String clientId,
			boolean closeApp,Channel proxyChannel,
			Map<String,ForwardHandler> appHandlerMap,
			Map<Integer,ForwardServer> forwardServerMap,
			Map<String,ServerHandler> serverHandlerMap) {
		this.proxyChannel = proxyChannel;
		this.appHandlerMap = appHandlerMap;
		this.forwardServerMap = forwardServerMap;
		this.serverHandlerMap = serverHandlerMap;
		this.onlineChannel = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		this.host = host;
		this.port = port;
		this.proxyPort = proxyPort;
		this.closeApp = closeApp;
		this.clientId = clientId;
	}
	
	@Override
	public void run() {
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();

		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				.option(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ForwardHandler(host,port,clientId,closeApp,proxyChannel,appHandlerMap,onlineChannel));
					}
				});

		bootstrap.bind(proxyPort).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				isFinish = true;
				if (future.isSuccess()) {
					forwardServerMap.put(proxyPort, ForwardServer.this);
					ServerHandler handler = serverHandlerMap.get(clientId);
					if(handler != null) {
						handler.addForwardServer(ForwardServer.this);
					}
					isOpen = true;
					
					new WorkThread().start();
					
					logger.info("Forward server startup successfully, port " + proxyPort + "......");
				} else {
					logger.info("Forward server startup failed, port " + proxyPort + "......");
				}
				notifyAllWait();
			}
		});
	}
	
	public void close() {
		if(boss != null) {
			boss.shutdownGracefully();
		}
		if(worker != null) {
			worker.shutdownGracefully();
		}
		forwardServerMap.remove(proxyPort);
		logger.info("Forward server shutdown, port " + proxyPort + "......");
	}
	
	public synchronized boolean waitFinish() throws InterruptedException {
		while(!isFinish) {
			wait();
		}
		return isOpen;
	}
	
	private synchronized void notifyAllWait() {
		this.notifyAll();
	}

	private class WorkThread extends Thread{

		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(onlineChannel.isEmpty()) {
					close();
					notifyAllWait();
					break;
				}
			}
		}
		
	}
}
