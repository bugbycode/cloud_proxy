package com.jing.cloud.forward.thread;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.jing.cloud.forward.initializer.ForwardChannelInitializer;
import com.jing.cloud.module.ConnectionInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.pool.WaitConnectionThreadPool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ForwardStartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(ForwardStartupRunnable.class);
	
	private int serverPort; // 端口号

	private int so_backlog;// 连接数

	private ChannelFuture future;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Channel agentChannel;
	
	private String token;
	
	private Map<String,WaitConnectionThreadPool> connectionMap;
	
	public ForwardStartupRunnable(int serverPort, int so_backlog, 
			Channel agentChannel,
			String token,
			Map<String,WaitConnectionThreadPool> connectionMap) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.agentChannel = agentChannel;
		this.token = token;
		this.connectionMap = connectionMap;
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
				.childHandler(new ForwardChannelInitializer(agentChannel,token));

		// 绑定端口，同步等待成功
		try {
			future = bootstrap.bind(serverPort).sync();
			Message message = new Message();
			message.setToken(token);
			if (future.isSuccess()) {
				logger.info("转发服务开启成功，端口号为 " + serverPort + " ……");
				ConnectionInfo conn = new ConnectionInfo();
				conn.setPort(serverPort);
				message.setData(conn);
				message.setType(MessageCode.CONNECTION_SUCCESS);
			} else {
				logger.info("转发服务开启失败……");
				message.setType(MessageCode.CONNECTION_ERROR);
			}
			
			WaitConnectionThreadPool wct = connectionMap.get(token);
			
			wct.sendMessage(message);
			
			future.channel().closeFuture().sync();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 优雅地退出，释放线程池资源
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}

	//关闭端口
	public void shutdown() {
		if(future != null) {
			future.channel().close();
		}
		
		if(boss != null) {
			boss.shutdownGracefully();
		}
		
		if(worker != null) {
			worker.shutdownGracefully();
		}
	}
}
