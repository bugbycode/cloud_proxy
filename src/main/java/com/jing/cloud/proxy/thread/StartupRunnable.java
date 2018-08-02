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
	
	private int serverPort; //�˿ں�
	
	private int so_backlog;//������
	
	private ChannelHandler serverChannelInitializer;
	
	
	public StartupRunnable(int serverPort, int so_backlog, ChannelHandler serverChannelInitializer) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.serverChannelInitializer = serverChannelInitializer;
	}

	@Override
	public void run() {
		//�����Ҫ��������group��һ��������տͻ��˵����ӣ�һ�����������ݴ���
		//���Ӵ���group
		EventLoopGroup boss = new NioEventLoopGroup();
		//�¼�����group
		EventLoopGroup worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		// �󶨴���group
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				//����������
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				//��������������
				.option(ChannelOption.TCP_NODELAY, true)
				//��������
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				//����������
				.childHandler(serverChannelInitializer);

		//�󶨶˿ڣ�ͬ���ȴ��ɹ�
		ChannelFuture future;
		try {
			future = bootstrap.bind(serverPort).sync();
			if (future.isSuccess()) {
				logger.info("����������ɹ����˿ں�Ϊ " + serverPort + " ����");
			} else {
				logger.info("���������ʧ�ܡ���");
			}
			
			future.channel().closeFuture().sync();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//���ŵ��˳����ͷ��̳߳���Դ
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}

}
