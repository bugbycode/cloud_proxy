package com.jing.cloud.forward.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ForwardStartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(ForwardStartupRunnable.class);
	
	private int serverPort; // �˿ں�

	private int so_backlog;// ������

	private ChannelHandler forwardChannelInitializer;

	private ChannelFuture future;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	public ForwardStartupRunnable(int serverPort, int so_backlog, ChannelHandler forwardChannelInitializer) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.forwardChannelInitializer = forwardChannelInitializer;
	}

	@Override
	public void run() {
		// �����Ҫ��������group��һ��������տͻ��˵����ӣ�һ�����������ݴ���
		// ���Ӵ���group
		boss = new NioEventLoopGroup();
		// �¼�����group
		worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();

		// �󶨴���group
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				// ����������
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				// ��������������
				.option(ChannelOption.TCP_NODELAY, true)
				// ��������
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				// ����������
				.childHandler(forwardChannelInitializer);

		// �󶨶˿ڣ�ͬ���ȴ��ɹ�
		try {
			future = bootstrap.bind(serverPort).sync();
			if (future.isSuccess()) {
				logger.info("ת���������ɹ����˿ں�Ϊ " + serverPort + " ����");
			} else {
				logger.info("ת��������ʧ�ܡ���");
			}

			future.channel().closeFuture().sync();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// ���ŵ��˳����ͷ��̳߳���Դ
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}

	//�رն˿�
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
