package com.thread;

import java.util.LinkedList;
import java.util.Map;

import com.jing.cloud.module.Message;

public class RecvMessageThreadPool extends ThreadGroup {

	private Map<String,RecvMessageThreadPool> recvMessagePool;
	
	public LinkedList<Message> queue;
	
	private final int WAIT_MAX_TIME = 120000;
	
	private boolean isClosed = true;
	
	private String token;
	
	private int type;
	
	private long endTime;
	
	private Message result;
	
	public RecvMessageThreadPool(Map<String,RecvMessageThreadPool> recvMessagePool,String token,int type) {
		super(token);
		this.recvMessagePool = recvMessagePool;
		this.recvMessagePool.put(token, this);
		this.isClosed = false;
		this.queue = new LinkedList<Message>();
		this.token = token;
		this.type = type;
		this.endTime = System.currentTimeMillis() + WAIT_MAX_TIME;
		new WorkThread().start();
	}
	
	public synchronized void addMessage(Message message) {
		if(!(message.getType() == type && message.getToken().equals(token))) {
			return;
		}
		queue.addLast(message);
		this.notifyAll();
	}

	private synchronized Message getMessage() throws InterruptedException {
		while(queue.isEmpty()) {
			if(isClosed) {
				throw new InterruptedException("Thread pool closed.");
			}
			long now = System.currentTimeMillis();
			long time = endTime - now;
			if(time <= 0) {
				this.close();
				throw new InterruptedException("Recv message timeout.");
			}
			wait(time);
		}
		return queue.removeFirst();
	}
	
	private synchronized void close() {
		this.recvMessagePool.remove(token);
		this.isClosed = true;
		this.notifyAll();
	}
	
	public synchronized void waitClose() throws InterruptedException {
		while(!isClosed) {
			wait();
		}
	}
	
	public Message result() {
		return this.result;
	}
	
	private class WorkThread extends Thread{

		@Override
		public void run() {
			while(!isClosed) {
				try {
					result = getMessage();
					close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
