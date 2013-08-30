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

public class SelectSoketsClientThreadPool extends Thread {

	// default parameter
	// --------------------------------------
	protected static int PORT = 5555;
	protected static String IP = "0.0.0.0";
	protected static int COMPLICATE = 5;
	protected static int REQUEST = 100;
	protected static String SENT_CONTENT = "http://baike.baidu.com/view/24982.htm";
	protected static String SENT_URL = "/";
	protected static String METHOD = "post";
	// --------------------------------------

	protected static boolean finishFlag = false;// finish flag

	// The result record
	// ---------------------------------------
	public static int[] result;// result record space
	public static final int TOTAL = 0;// total request number
	public static final int THREAD = 1;// complicate number
	public static final int SENT = 2;// sent request number
	public static final int SUCCESS = 3;// success response number
	public static final int FAIL = 4;// fail response number
	// ---------------------------------------

	private static int sentNum = 0;// sent number
	private SocketAddress socketAddress = null;// socket address

	public static void main(String[] argv) {
		new SelectSoketsClientThreadPool().start();
	}

	@Override
	public void run() {
		// initial result space
		// --------------------------
		result = new int[5];
		result[TOTAL] = REQUEST;
		result[THREAD] = COMPLICATE;
		result[SENT] = 0;
		result[SUCCESS] = 0;
		result[FAIL] = 0;
		// --------------------------

		try {
			// initial client socket channel
			// ------------------------------------------------------
			socketAddress = new InetSocketAddress(IP, PORT);
			Selector selector = Selector.open();
			for (int i = 0; i < COMPLICATE; i++) {
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.socket().setReuseAddress(true);
				socketChannel.connect(socketAddress);
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_READ);
				// start sending
				sendMessage(socketChannel);
			}
			// ------------------------------------------------------

			// start selector
			// ------------------------------------------------------
			while (true) {
				int n = selector.select();
				if (n == 0) {
					continue;
				}
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					if (key.isReadable()) {
						// dealing when receive data
						onReceived(key);// read response
					}
				}
			}
			// ------------------------------------------------------
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	/**
	 * Read a HTTP response
	 * 
	 * @param key
	 *            socket channel key
	 */
	protected void onReceived(SelectionKey key) throws Exception {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		HttpCodecFactory httpFactory = new HttpCodecFactory();
		//decode data into a HTTP response entity
		HttpResponseMessage response = httpFactory.readResponse(socketChannel);
		
		//result checking
		if (isCorrectReplay(response)) {
			result[SUCCESS]++;
		} else {
			result[FAIL]++;
		}
		sendMessage(key);// send request
	}

	/**
	 * Send a HTTP request if not finished
	 */
	protected void sendMessage(SocketChannel socketChannel) throws Exception {
		if (sentNum >= REQUEST) {
			finishFlag = true;
			return;
		}
		
		//build a HTTP request
		HttpRequestMessage mess;
		if ("get".equals(METHOD))
			mess = new HttpRequestMessage(HttpRequestMessage.HttpMethod.GET,
					SENT_URL);
		else
			mess = new HttpRequestMessage(HttpRequestMessage.HttpMethod.POST,
					SENT_URL);
		mess.setHeader("charset", "UTF-16LE");
		mess.setParameters("url", getUrlParam());
		HttpCodecFactory httpFactory = new HttpCodecFactory();
		//encode HTTP request and send
		httpFactory.sendRequest(socketChannel, mess);
		
		result[SENT]++;
		sentNum++;
	}

	/**
	 * Send a HTTP request if not finished
	 * 
	 * @param key
	 *            socket channel key
	 */
	protected void sendMessage(SelectionKey key) throws Exception {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		sendMessage(socketChannel);
	}

	/**
	 * HTTP Response checking method.
	 * @param response
	 * HTTP response message
	 * @return
	 * Only match the pattern "{@code <scene>(.*?)</scene>}" will be true.
	 */
	private static int respNum=0;
	protected boolean isCorrectReplay(HttpResponseMessage response) {
		respNum++;
		if (response == null || !response.isSucceeded()) {
			//null or without succeeded flag is false
			System.err.println(respNum+":response time out!");
			return false;
		}
		
		//content match
		String xml = String.valueOf(response.getReplyContentRecved());
		Pattern pattern = Pattern.compile("<scene>(.*?)</scene>");
		Matcher matcher = pattern.matcher(xml);
		if (matcher.find()) {
			//match is true
			//System.out.println(respNum+":"+xml);
			return true;
		}
		
		//not match is false
		System.err.println(respNum+":"+xml);
		return false;
	}

	private static BufferedReader in = null;// URL parameter file reader

	/**
	 * Get URL parameter from file
	 * 
	 * @return
	 */
	public static String getUrlParam() {
		File file = new File(SENT_CONTENT);
		if (!file.exists())//not a file
			return SENT_CONTENT;
		else {
			String line = null;
			try {
				//open a stream reader
				if (in == null) {
					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file)));
				}
				
				//open a new stream reader when EOF
				if ((line = in.readLine()) == null) {
					in.close();
					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file)));
					//read a line of the file
					line = in.readLine();
				}
				
				if (line == null)//error read
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
