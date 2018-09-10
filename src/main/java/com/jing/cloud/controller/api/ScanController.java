package com.jing.cloud.controller.api;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jing.cloud.module.HostInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.jing.cloud.module.ScanHostResult;
import com.thread.RecvMessageThreadPool;
import com.util.RandomUtil;

import io.netty.channel.Channel;

@Controller
@RequestMapping("/api")
public class ScanController {
	
	private static final Logger logger = LogManager.getLogger(ScanController.class);
	
	@Autowired
	private Map<String,RecvMessageThreadPool> recvMessagePool;
	
	@Autowired
	private Map<String,Channel> onlineProxyClient;
	
	@RequestMapping("/scanOs")
	@ResponseBody
	public Map<String,Object> scanOs(String clientId,String host){
		logger.info("clientId : " + clientId);
		logger.info("onlineProxyClient : " + onlineProxyClient);
		Map<String,Object> map = new HashMap<String,Object>();
		Channel channel = onlineProxyClient.get(clientId);
		if(channel == null) {
			map.put("code", 1);
			map.put("msg", "Con't find agent client.");
		}else {
			LinkedList<HostInfo> list = new LinkedList<HostInfo>();
			String token = RandomUtil.GetGuid32();
			Message message = new Message();
			message.setType(MessageCode.SCAN_OS);
			message.setToken(token);
			message.setData(host);
			RecvMessageThreadPool rmtp = new RecvMessageThreadPool(recvMessagePool, token, MessageCode.SCAN_OS_RESULT);
			channel.writeAndFlush(message);
			try {
				rmtp.waitClose();
				Message result = rmtp.result();
				if(result != null) {
					Object data = result.getData();
					if(data instanceof ScanHostResult) {
						ScanHostResult shr = (ScanHostResult) data;
						while(shr.isNotEmpty()) {
							list.add(shr.pop());
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			map.put("code", 0);
			map.put("data", list);
		}
		return map;
	}
}
