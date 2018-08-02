package com.jing.cloud.proxy.thread;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private int serverPort; //端口号
	
	private int so_backlog;//连接数
	
	private ChannelHandler serverChannelInitializer;
	
	
	public StartupRunnable(int serverPort, int so_backlog, ChannelHandler serverChannelInitializer) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.serverChannelInitializer = serverChannelInitializer;
	}

	@Override
	public void run() {
		//服务端要建立两个group，一个负责接收客户端的连接，一个负责处理数据传输
		//连接处理group
		EventLoopGroup boss = new NioEventLoopGroup();
		//事件处理group
		EventLoopGroup worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		// 绑定处理group
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				//保持连接数
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				//有数据立即发送
				.option(ChannelOption.TCP_NODELAY, true)
				//保持连接
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				//处理新连接
				.childHandler(serverChannelInitializer);

		//绑定端口，同步等待成功
		ChannelFuture future;
		try {
			future = bootstrap.bind(serverPort).sync();
			if (future.isSuccess()) {
				logger.info("代理服务开启成功，端口号为 " + serverPort + " ……");
			} else {
				logger.info("代理服务开启失败……");
			}
			
			future.channel().closeFuture().sync();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//优雅地退出，释放线程池资源
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}

}
