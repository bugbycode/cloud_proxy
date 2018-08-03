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
	
	private int serverPort; // �˿ں�

	private int so_backlog;// ������

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
				.childHandler(new ForwardChannelInitializer(agentChannel,token));

		// �󶨶˿ڣ�ͬ���ȴ��ɹ�
		try {
			future = bootstrap.bind(serverPort).sync();
			Message message = new Message();
			message.setToken(token);
			if (future.isSuccess()) {
				logger.info("ת���������ɹ����˿ں�Ϊ " + serverPort + " ����");
				ConnectionInfo conn = new ConnectionInfo();
				conn.setPort(serverPort);
				message.setData(conn);
				message.setType(MessageCode.CONNECTION_SUCCESS);
			} else {
				logger.info("ת��������ʧ�ܡ���");
				message.setType(MessageCode.CONNECTION_ERROR);
			}
			
			WaitConnectionThreadPool wct = connectionMap.get(token);
			
			wct.sendMessage(message);
			
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
