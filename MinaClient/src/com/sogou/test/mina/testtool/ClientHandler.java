package com.sogou.test.mina.testtool;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.sogou.test.mina.http.HttpResponseMessage;

public class ClientHandler  extends IoHandlerAdapter{
	public AbstractTest at;
	
	public ClientHandler(AbstractTest at){
		this.at=at;
	}
	
	@Override
	public void sessionOpened(IoSession session) {
		this.sendMessage(session);
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) {
		HttpResponseMessage response=null;
		if (message instanceof HttpResponseMessage) {
			response=(HttpResponseMessage)message;
		}
		at.recieveN++;
		at.onRecieved(response);
		this.sendMessage(session);
	}
	
	@Override
	public void messageSent(IoSession session, Object message){
		try {
			at.sendN++;
			at.onSend();
			super.messageSent(session, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendMessage(IoSession session){
		if (at.isFinished())
			session.close(true);
		else
			session.write(at.getSendRequest());
	}
}
