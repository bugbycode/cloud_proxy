package com.jing.cloud.forward.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jing.cloud.forward.server.ForwardServer;
import com.jing.cloud.forward.service.RegisterPortService;

@Service("registerPortService")
public class RegisterPortServiceImpl implements RegisterPortService {

	@Autowired
	private Map<Integer,ForwardServer> forwardServerMap;

	private final int MIN_PORT = 55000;
	
	private final int MAX_PORT = 65535;
	
	@Override
	public synchronized int registerServerPort() {
		int port = 0;
		if(forwardServerMap.isEmpty()) {
			port = MIN_PORT;
		}else {
			for(int num = MIN_PORT;num <= MAX_PORT;num++) {
				if(forwardServerMap.get(num) == null) {
					port = num;
					break;
				}
			}
		}
		return port;
	}

}
