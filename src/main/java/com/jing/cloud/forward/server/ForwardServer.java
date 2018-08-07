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
	
	private String host = "";
	
	private int port;
	
	private int proxyPort;
	
	public ForwardServer(String host,int port,int proxyPort,Channel proxyChannel,
			Map<String,ForwardHandler> appHandlerMap) {
		this.proxyChannel = proxyChannel;
		this.appHandlerMap = appHandlerMap;
		this.host = host;
		this.port = port;
		this.proxyPort = proxyPort;
	}
	
	@Override
	public void run() {
		// 服务端要建立两个group，一个负责接收客户端的连接，一个负责处理数据传输
		// 连接处理group
		boss = new NioEventLoopGroup();
		// 事件处理group
		worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();

		// 绑定处理group
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				// 保持连接数
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				// 有数据立即发送
				.option(ChannelOption.TCP_NODELAY, true)
				// 保持连接
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				// 处理新连接
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ForwardHandler(host,port,proxyChannel,appHandlerMap));
					}
				});

		// 绑定端口，同步等待成功
		bootstrap.bind(proxyPort).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("代理服务开启成功，端口号为 " + proxyPort + " ……");
				} else {
					logger.info("代理服务开启失败……");
				}
			}
		});
	}
	
	public void close() {
		boss.shutdownGracefully();
		worker.shutdownGracefully();
	}

}
