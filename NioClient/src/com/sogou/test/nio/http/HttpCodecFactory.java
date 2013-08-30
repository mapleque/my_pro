package com.sogou.test.nio.http;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP request and response encoding and decoding
 * 
 * @author yangyang@sogou-inc.com
 * 
 */
public class HttpCodecFactory {

	private final static long TIMEOUT=5000;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

	/**
	 * read a HTTP request from channel
	 * 
	 * @param socketChannel
	 * @return HttpRequestMessage
	 * @see HttpReqeustMessage
	 */
	public HttpRequestMessage readRequest(SocketChannel socketChannel)
			throws Exception {
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		int count = 0;
		buffer.clear();
		List<ByteBuffer> buffer_list = new ArrayList<ByteBuffer>();
		while ((count = socketChannel.read(buffer)) > 0) {
			buffer.flip();
			ByteBuffer cur_buffer = ByteBuffer.allocateDirect((buffer_list
					.size() + 1) * 1024);
			for (ByteBuffer buf : buffer_list)
				cur_buffer.put(buf);
			cur_buffer.put(buffer);
			cur_buffer.flip();
			if ((decoder.decodable(cur_buffer))) {
				return decoder.decode(cur_buffer);
			} else {
				buffer.flip();
				buffer_list.add(buffer);
				buffer.clear();
			}
		}
		if (count < 0) {
			socketChannel.close();
		}
		return null;
	}

	/**
	 * read a HTTP response from channel
	 * 
	 * @param socketChannel
	 * @return HttpResponseMessage
	 * @throws Exception
	 * @see HttpResponseMessage
	 */
	public HttpResponseMessage readResponse(SocketChannel socketChannel)
			throws Exception {
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		int count = 0;
		buffer.clear();
		List<byte[]> buffer_list = new ArrayList<byte[]>();
		long startTime=System.currentTimeMillis();
		while ((count = socketChannel.read(buffer)) >=0) {
			if (count==0){
				//wait for time out
				long curTime=System.currentTimeMillis();
				if (curTime-startTime>TIMEOUT)
					return null;
				buffer.flip();
				buffer.clear();
				continue;
			}
			ByteBuffer cur_buffer = ByteBuffer.allocateDirect((buffer_list
					.size() + 1) * 1024);
			byte[] dst;
			for (byte[] buf : buffer_list) {
				cur_buffer.put(buf);
			}
			dst = new byte[buffer.position()];
			buffer.flip();
			buffer.get(dst);
			//System.out.println(new String(dst,"UTF-16LE"));
			cur_buffer.put(dst);
			cur_buffer.flip();
			if ((decoder.decodable(cur_buffer))) {
				return decoder.decode(cur_buffer);
			} else {
				dst = new byte[buffer.position()];
				buffer.flip();
				buffer.get(dst);
				buffer_list.add(dst);
				buffer.clear();
			}
		}
		if (count < 0) {
			socketChannel.close();
			System.err.println("channel close");
			return null;
		}
		return null;
	}

	/**
	 * send a HTTP response message through channel
	 * 
	 * @param socketChannel
	 * @param message
	 *            HttpResponseMessage
	 * @throws Exception
	 * @see HttpResponseMessage
	 */
	public void sendResponse(SocketChannel socketChannel,
			HttpResponseMessage message) throws Exception {
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		ByteBuffer buffer = encoder.encode(message);
		socketChannel.write(buffer);
	}

	/**
	 * send a HTTP request message through channel
	 * 
	 * @param socketChannel
	 * @param message
	 *            HttpRequestMessage
	 * @throws Exception
	 * @see HttpRequestMessage
	 */
	public void sendRequest(SocketChannel socketChannel,
			HttpRequestMessage message) throws Exception {
		HttpRequestEncoder encoder = new HttpRequestEncoder();
		ByteBuffer buffer = encoder.encode(message);
		socketChannel.write(buffer);
	}
}
