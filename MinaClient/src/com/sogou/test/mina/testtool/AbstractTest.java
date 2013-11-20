package com.sogou.test.mina.testtool;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.sogou.test.mina.http.HttpRequestCodecFactory;
import com.sogou.test.mina.http.HttpRequestMessage;
import com.sogou.test.mina.http.HttpResponseMessage;

public abstract class AbstractTest {
	public long startMillis;
	public long stopMillis;
	
	public long timeout=10000;

	protected int c = 1;// 并发数
	protected int n = 1;// 请求数

	protected String host = "0.0.0.0";// 请求ip或域名
	protected int port = 80;// 请求端口
	protected String path = "/";// 请求路径
	protected String method = "GET";// 请求方法

	protected HashMap<String, String> form = new HashMap<String, String>();// post表单数据
	protected HashMap<String, String> header = new HashMap<String, String>();// header数据
	
	protected int sendN=0;//已发送请求数
	protected int recieveN=0;//已返回请求数
	
	protected SocketConnector connector=null;
	
	public abstract void onFinishedC();
	
	public abstract void onStop();

	public abstract void onSend();

	public abstract void onRecieved(HttpResponseMessage response);

	public boolean isFinished(){
		if (this.sendN>=this.n){
			return true;
		}
		return false;
	}

	public HttpRequestMessage getSendRequest() {
		HttpRequestMessage mess = null;
		if ("POST".equals(method))
			mess = new HttpRequestMessage(HttpRequestMessage.HttpMethod.POST,
					path);
		else
			mess = new HttpRequestMessage(HttpRequestMessage.HttpMethod.GET,
					path);
		for (String key : header.keySet())
			mess.setHeader(key, header.get(key));
		for (String key : form.keySet())
			mess.setParameters(key, form.get(key));
		return mess;
	}

	public void start() {
		connector = new NioSocketConnector();
		connector.getFilterChain().addLast("protocolFilter",
				new ProtocolCodecFilter(new HttpRequestCodecFactory()));
		connector.setHandler(new ClientHandler(this));
		this.startMillis=System.currentTimeMillis();
		for (int i = 0; i < c; i++) {
			ConnectFuture cf = connector.connect(new InetSocketAddress(host,
					port));// 建立连接
			cf.awaitUninterruptibly();
			(new StopThread(cf,this)).start();//启动用于结束程序的守护线程
			(new TimeOutThread(this)).start();//用于监控超时的守护线程
		}
	}
	protected int finished=0;
	
	public void finishedC(){
		this.finished++;
		this.onFinishedC();
		if (this.finished>=this.c)
			this.stop();
	}
	
	public void stop(){
		this.stopMillis=System.currentTimeMillis();
		if (connector!=null)
			connector.dispose();
		this.onStop();
		System.exit(0);
	}
	
	protected class StopThread extends Thread{
		private ConnectFuture cf;
		private AbstractTest at;
		public StopThread(ConnectFuture cf,AbstractTest at){
			this.cf=cf;
			this.at=at;
		}
		@Override
		public void run() {
			cf.getSession().getCloseFuture().awaitUninterruptibly();
			at.finishedC();
		}
	}
	
	protected class TimeOutThread extends Thread{
		private AbstractTest at;
		public TimeOutThread(AbstractTest at){
			this.at=at;
		}
		@Override
		public void run() {
			try {
				sleep(at.timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (at.recieveN<=0){
				at.stop();
			}
		}
	}

}
