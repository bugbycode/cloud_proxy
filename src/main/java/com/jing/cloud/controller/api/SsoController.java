package com.jing.cloud.controller.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.forward.server.ForwardServer;

import io.netty.channel.Channel;

@Controller
@RequestMapping("/api")
public class SsoController {
	
	@Autowired
	private Map<String,Channel> onlineProxyClient;
	
	@Autowired
	public Map<String,ForwardHandler> appHandlerMap;
	
	@RequestMapping("/getChannel")
	@ResponseBody
	public Map<String,Object> getChannel(
			@RequestParam(name="clientId",defaultValue = "") 
			String clientId,
			@RequestParam(name="host",defaultValue = "") 
			String host,
			@RequestParam(name="port",defaultValue = "0") 
			int port,
			@RequestParam(name="proxyPort",defaultValue = "0") 
			int proxyPort,
			@RequestParam(name="closeApp",defaultValue = "true") 
			boolean closeApp
			) throws IOException{
		Map<String,Object> map = new HashMap<String,Object>();
		
		Channel channel = onlineProxyClient.get(clientId);
		
		if(channel == null) {
			map.put("code", 1);
			map.put("msg", "Con't find agent client.");
		}else {
			ForwardServer server = new ForwardServer(host,port,proxyPort,closeApp,channel, appHandlerMap);
			server.run();
			map.put("code", 0);
			map.put("msg", "success");
			map.put("host", "127.0.0.1");
			map.put("port", proxyPort);
		}
		
		return map;
	}
	
	@RequestMapping("/closeChannel")
	@ResponseBody
	public Map<String,Object> closeChannel(
			@RequestParam(name="port",defaultValue = "0") 
			int port
			) throws IOException{
		
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("code", 0);
		map.put("msg", "success");
		
		return map;
	}
}
