package com.jing.cloud.controller.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jing.cloud.forward.handler.ForwardHandler;
import com.jing.cloud.forward.server.ForwardServer;
import com.jing.cloud.forward.service.RegisterPortService;
import com.jing.cloud.proxy.handler.ServerHandler;

import io.netty.channel.Channel;

@Controller
@RequestMapping("/api")
public class SsoController {
	
	private static final Logger logger = LogManager.getLogger(SsoController.class);
	
	@Autowired
	private Map<String,Channel> onlineProxyClient;
	
	@Autowired
	private Map<String,ForwardHandler> appHandlerMap;
	
	@Autowired
	private Map<Integer,ForwardServer> forwardServerMap;
	
	@Autowired
	public Map<String,ServerHandler> serverHandlerMap;
	
	@Autowired
	private RegisterPortService registerPortService;
	
	@RequestMapping("/getChannel")
	@ResponseBody
	public Map<String,Object> getChannel(
			@RequestParam(name="clientId",defaultValue = "") 
			String clientId,
			@RequestParam(name="host",defaultValue = "") 
			String host,
			@RequestParam(name="port",defaultValue = "0") 
			int port,
			@RequestParam(name="closeApp",defaultValue = "true") 
			boolean closeApp
			) throws IOException, InterruptedException{
		
		logger.info("clientId : " + clientId);
		logger.info("onlineProxyClient : " + onlineProxyClient);
		Map<String,Object> map = new HashMap<String,Object>();
		
		Channel channel = onlineProxyClient.get(clientId);
		
		if(channel == null) {
			map.put("code", 1);
			map.put("msg", "Con't find agent client.");
		}else {
			
			int proxyPort = registerPortService.registerServerPort();
			
			if(proxyPort == 0) {
				map.put("code", 2);
				map.put("msg", "Unavailable ports");
			}else {
				ForwardServer server = new ForwardServer(host,port,proxyPort,clientId,closeApp,
						channel, appHandlerMap,forwardServerMap,serverHandlerMap);
				server.run();
				
				boolean isOpen = server.waitFinish();
				
				if(isOpen) {
					map.put("code", 0);
					map.put("msg", "success");
					map.put("port", proxyPort);
				}else {
					map.put("code", 1);
					map.put("msg", "Bind " + proxyPort + " failed.");
				}
			}
		}
		
		return map;
	}
	
	@RequestMapping("/closeChannel")
	@ResponseBody
	public Map<String,Object> closeChannel(
			@RequestParam(name="port",defaultValue = "0") 
			int port
			) throws IOException{
		
		ForwardServer server = forwardServerMap.get(port);
		if(server != null) {
			server.close();
		}
		
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("code", 0);
		map.put("msg", "success");
		
		return map;
	}
	
	@RequestMapping("/getConnCount")
	@ResponseBody
	public int getConnCount() {
		return serverHandlerMap.size();
	}
	
	@RequestMapping("/getAllClientId")
	@ResponseBody
	public Map<String,Object> getAllClientId(){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("code", 0);
		map.put("data", onlineProxyClient.keySet());
		return map;
	}
}
