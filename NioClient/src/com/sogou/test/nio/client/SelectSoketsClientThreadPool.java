package com.sogou.test.nio.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sogou.test.nio.http.HttpCodecFactory;
import com.sogou.test.nio.http.HttpRequestMessage;
import com.sogou.test.nio.http.HttpResponseMessage;

public class SelectSoketsClientThreadPool extends Thread{

	protected static int PORT = 5555;
	protected static String IP = "10.12.18.196";
	protected static int COMPLICATE = 5;
	protected static int REQUEST=100;
	protected static String SENT_CONTENT = "http://baike.baidu.com/view/24982.htm?fromId=489499";
	protected static String SENT_URL="/";
	protected static String METHOD="post";
	
	private static int sentNum=0;
	protected static boolean finishFlag=false;
	
	/******** The result record **********/
	public static int[] result;
	public static final int TOTAL = 0;
	public static final int THREAD = 1;
	public static final int SENT = 2;
	public static final int SUCCESS = 3;
	public static final int FAIL = 4;
	/******** The result record end *******/

	private SocketAddress socketAddress = null;
//	private List<SocketChannel> channelList=new ArrayList<SocketChannel>();

	public static void main(String[] argv) {
		new SelectSoketsClientThreadPool().start();
	}
	
	public void run() {
		result = new int[5];
		result[TOTAL] = REQUEST;
		result[THREAD] = COMPLICATE;
		result[SENT] = 0;
		result[SUCCESS] = 0;
		result[FAIL] = 0;
		
		try {
			socketAddress = new InetSocketAddress(IP, PORT);
			Selector selector = Selector.open();

			for (int i=0;i<COMPLICATE;i++){
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.socket().setReuseAddress(true);
				socketChannel.connect(socketAddress);
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_READ);
				
//				channelList.add(socketChannel);
				sendMessage(socketChannel);
			}
			

			while (true) {
				int n = selector.select();
				if (n == 0) {
					System.out.println("no selector remain");
					continue;
				}
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					if (key.isReadable()) {
						onRecieved(key);
						sendMessage(key);
					}
					it.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
	
//	protected void startSend() throws Exception{
//		for (SocketChannel socketChannel:channelList){
//			sendMessage(socketChannel);
//		}
//	}
	
	protected void onRecieved(SelectionKey key) throws Exception {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		HttpCodecFactory httpFactory=new HttpCodecFactory();
		HttpResponseMessage response=httpFactory.readResponse(socketChannel);
		if (isCorrectReplay(response)){
			result[SUCCESS]++;
		}else{
			result[FAIL]++;
		}
	}
	
	protected void sendMessage(SocketChannel socketChannel)throws Exception{
		if (sentNum>=REQUEST){
			finishFlag=true;
			return;
		}
			
		HttpRequestMessage mess;
		if ("get".equals(METHOD))
			mess=new HttpRequestMessage(HttpRequestMessage.HttpMethod.GET,SENT_URL);
		else
			mess=new HttpRequestMessage(HttpRequestMessage.HttpMethod.POST,SENT_URL);
		mess.setHeader("charset", "utf-8");
		mess.setParameters("url", getUrlParam());
		HttpCodecFactory httpFactory=new HttpCodecFactory();
		httpFactory.sendRequest(socketChannel, mess);
		result[SENT]++;
		sentNum++;
	}
	
	protected void sendMessage(SelectionKey key) throws Exception{
		SocketChannel socketChannel = (SocketChannel) key.channel();
		sendMessage(socketChannel);
	}
	
	protected boolean isCorrectReplay(HttpResponseMessage response){
		if (response==null||!response.isSucceeded()){
			System.err.println("unkown response");
			return false;
		}
		String xml=String.valueOf(response.getReplyContentRecved());
//		System.err.println(xml);
		Pattern pattern = Pattern
				.compile("<scene>(.*?)</scene>");
		Matcher matcher = pattern.matcher(xml);
		if (matcher.find()){
//			System.err.println(matcher.group());
//			System.err.println(matcher.group(1));
			return true;
		}
		System.err.println(xml);
		return false;
	}
	
	private static BufferedReader in = null;//URL parameter file reader
	/**
	 * Get URL parameter from file
	 * @return
	 */
	public static String getUrlParam() {
		File file = new File(SENT_CONTENT);
		if (!file.exists())
			return SENT_CONTENT;
		else {
			String line = null;
			try {
				if (in == null) {
					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file)));
				}
				if ((line = in.readLine()) == null) {
					in.close();
					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file)));
					line = in.readLine();
				}
				if (line == null)
					line = SENT_CONTENT;
				return line;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SENT_CONTENT;
		}
	}
	
}
