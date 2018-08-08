package com.jing.cloud.forward.server;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jing.cloud.forward.handler.ForwardHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ForwardServer implements Runnable {

	private final Logger logger = LogManager.getLogger(ForwardServer.class);
	
	private int so_backlog = 50;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Channel proxyChannel;
	
	private Map<String,ForwardHandler> appHandlerMap;
	
	private Map<Integer,ForwardServer> forwardServerMap;
	
	private String host = "";
	
	private int port;
	
	private int proxyPort;
	
	private boolean closeApp = true;
	
	private boolean isFinish = false;
	
	private boolean isOpen = false;
	
	public ForwardServer(String host,int port,int proxyPort,
			boolean closeApp,Channel proxyChannel,
			Map<String,ForwardHandler> appHandlerMap,
			Map<Integer,ForwardServer> forwardServerMap) {
		this.proxyChannel = proxyChannel;
		this.appHandlerMap = appHandlerMap;
		this.forwardServerMap = forwardServerMap;
		this.host = host;
		this.port = port;
		this.proxyPort = proxyPort;
		this.closeApp = closeApp;
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
						ch.pipeline().addLast(new ForwardHandler(host,port,closeApp,proxyChannel,appHandlerMap));
					}
				});

		bootstrap.bind(proxyPort).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				isFinish = true;
				if (future.isSuccess()) {
					forwardServerMap.put(proxyPort, ForwardServer.this);
					isOpen = true;
					logger.info("Forward server startup successfully, port " + proxyPort + "......");
				} else {
					logger.info("Forward server startup failed, port " + proxyPort + "......");
				}
				notifyAllWait();
			}
		});
	}
	
	public void close() {
		boss.shutdownGracefully();
		worker.shutdownGracefully();
		forwardServerMap.remove(proxyPort);
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

}
