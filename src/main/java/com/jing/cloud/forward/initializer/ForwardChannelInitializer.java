package com.jing.cloud.forward.initializer;

import java.util.Map;

import com.jing.cloud.forward.handler.ForwardHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ForwardChannelInitializer extends ChannelInitializer<SocketChannel> {

	private Channel agentChannel;
	
	private String host;	//内网目标主机
	
	private int port; 		//内网目标主机端口号
	
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
		// 增加任务处理
		ChannelPipeline p = sc.pipeline();
		p.addLast(new ForwardHandler(agentChannel,host,port,onlineUserClient));
	}

}
