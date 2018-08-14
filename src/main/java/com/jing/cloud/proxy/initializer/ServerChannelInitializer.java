package com.jing.cloud.proxy.initializer;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.bugbycode.https.HttpsClient;
import com.jing.cloud.config.IdleConfig;
import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.handler.MessageDecoder;
import com.jing.cloud.handler.MessageEncoder;
import com.jing.cloud.module.HandlerConst;
import com.jing.cloud.proxy.handler.ServerHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

@Configuration
@Service("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ChannelGroup channelGroup;
	
	@Autowired
	private Map<String,Channel> onlineProxyClient;
	
	@Autowired
	public Map<String,ForwardHandler> appHandlerMap;
	
	@Autowired
	public Map<String,ServerHandler> serverHandlerMap;
	
	@Value("${spring.oauth.oauthUri}")
	private String oauthUri;
	
	@Value("${server.keystorePath}")
	private String keystorePath;
	
	@Value("${server.keystorePassword}")
	private String keystorePassword;
	
	private HttpsClient client;
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		this.client = new HttpsClient(keystorePath, keystorePassword);
		ChannelPipeline p = sc.pipeline();
		p.addLast(
				new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
				new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
						HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
						HandlerConst.INITIAL_BYTES_TO_STRIP),
				new MessageEncoder(),
				new ServerHandler(channelGroup,onlineProxyClient,
						appHandlerMap,serverHandlerMap,
						oauthUri,client)
		);
		
	}

}
