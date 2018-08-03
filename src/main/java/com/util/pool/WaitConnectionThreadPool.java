package com.util.pool;

import java.util.LinkedList;
import java.util.Map;

import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;

public class WaitConnectionThreadPool extends ThreadGroup {

	private final int DEFAULT_WAIT_TIME = 15000;
	
	private String token;
	
	private boolean isClosed = true;
	
	private LinkedList<Message> linkedList;
	
	private long endTime = 0;
	
	private Message result;
	
	private Map<String,WaitConnectionThreadPool> connectionMap;
	
	public WaitConnectionThreadPool(String token,Map<String,WaitConnectionThreadPool> connectionMap) {
		super(token);
		isClosed = false;
		this.token = token;
		this.linkedList = new LinkedList<Message>();
		long startTime = System.currentTimeMillis();
		endTime = startTime + DEFAULT_WAIT_TIME;
		this.connectionMap = connectionMap;
		this.connectionMap.put(token, this);
		new WorkThread().start();
	}

	public synchronized void sendMessage(Message msg) {
		if(isClosed) {
			return;
		}
		if(msg == null) {
			return;
		}
		linkedList.addFirst(msg);
		this.notifyAll();
	}
	
	private synchronized Message getMessage() throws InterruptedException {
		while(linkedList.isEmpty()) {
			if(isClosed) {
				throw new InterruptedException("Thread is closed.");
			}
			long now = System.currentTimeMillis();
			long waitTime = endTime - now;
			if(waitTime <= 0) {
				throw new InterruptedException("Get message timeout.");
			}
			wait(waitTime);
		}
		return linkedList.removeFirst();
	}
	
	public synchronized void waitPoolClose() throws InterruptedException {
		while(!isClosed) {
			wait();
		}
	}
	
	public Message getResultMessage() {
		return this.result;
	}
	
	private synchronized void notifyPool() {
		this.notifyAll();
	}
	
	private class WorkThread extends Thread{

		@Override
		public void run() {
			try {
				while(!isClosed) {
					Message message = getMessage();
					if(token.equals(message.getToken()) 
							&& (message.getType() == MessageCode.CONNECTION_SUCCESS || 
									message.getType() == MessageCode.CONNECTION_ERROR)) {
						result = message;
						break;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				connectionMap.remove(token);
				isClosed = true;
				notifyPool();
			}
		}
	}
	
}
