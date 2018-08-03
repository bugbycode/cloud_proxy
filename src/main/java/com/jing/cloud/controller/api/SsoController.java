package com.jing.cloud.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jing.cloud.module.ConnectionInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.util.RandomUtil;
import com.util.pool.WaitConnectionThreadPool;

import io.netty.channel.Channel;

@Controller
@RequestMapping("/api")
public class SsoController {
	
	@Autowired
	private Map<String,Channel> onlineProxyClient;
	
	@Autowired
	private Map<String,WaitConnectionThreadPool> connectionMap;
	
	@RequestMapping("/getChannel")
	@ResponseBody
	public Map<String,Object> getChannel(
			@RequestParam(name="clientId",defaultValue = "") 
			String clientId,
			@RequestParam(name="host",defaultValue = "") 
			String host,
			@RequestParam(name="port",defaultValue = "0") 
			int port){
		Map<String,Object> map = new HashMap<String,Object>();
		
		Channel channel = onlineProxyClient.get(clientId);
		
		if(channel == null) {
			map.put("code", 1);
			map.put("msg", "Con't find agent client.");
		}else {
			
			Message message = new Message();
			String token = RandomUtil.GetGuid32();
			ConnectionInfo conn = new ConnectionInfo(host, port);
			
			message.setType(MessageCode.CONNECTION);
			message.setData(conn);
			message.setToken(token);
			
			WaitConnectionThreadPool wct = new WaitConnectionThreadPool(token, connectionMap);
			
			channel.writeAndFlush(message);
			
			try {
				wct.waitPoolClose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Message result = wct.getResultMessage();
			if(result == null || result.getType() == MessageCode.CONNECTION_ERROR) {
				map.put("code", 1);
				map.put("msg", "Create channel failed.");
			}else {
				ConnectionInfo con = (ConnectionInfo) result.getData();
				map.put("code", 0);
				map.put("msg", "success");
				map.put("host", "127.0.0.1");
				map.put("port", con.getPort());
			}
		}
		
		return map;
	}
	
	@RequestMapping("/closeChannel")
	@ResponseBody
	public Map<String,Object> closeChannel(
			@RequestParam(name="port",defaultValue = "0") 
			int port
			){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("code", 0);
		map.put("msg", "success");
		return map;
	}
}
