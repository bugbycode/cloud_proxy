package com.jing.cloud.forward.initializer;

import java.util.Map;

import com.jing.cloud.forward.handler.ForwardHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ForwardChannelInitializer extends ChannelInitializer<SocketChannel> {

	private Channel agentChannel;
	
	private String host;	//����Ŀ������
	
	private int port; 		//����Ŀ�������˿ں�
	
	private Map<String,Channel> onlineUserClient;
	
	public ForwardChannelInitializer(Channel agentChannel,String host,int port,
			Map<String,Channel> onlineUserClient) {
		this.agentChannel = agentChannel;
		this.host = host;
		this.port = port;
		this.onlineUserClient = onlineUserClient;
	}

	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		// ����������
		ChannelPipeline p = sc.pipeline();
		p.addLast(new ForwardHandler(agentChannel,host,port,onlineUserClient));
	}

}
