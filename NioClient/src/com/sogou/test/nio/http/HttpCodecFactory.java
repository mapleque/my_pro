package com.sogou.test.nio.http;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * for the client handler encoding and decoding
 * @author yangyang
 *
 */
public class HttpCodecFactory{
	private ByteBuffer buffer=ByteBuffer.allocateDirect(1024);
	public HttpRequestMessage readRequest(SocketChannel socketChannel)  throws Exception{
		HttpRequestDecoder decoder=new HttpRequestDecoder();
		int count=0;
		buffer.clear();
		List<ByteBuffer> buffer_list=new ArrayList<ByteBuffer>();
		while ((count = socketChannel.read(buffer)) > 0) {
			buffer.flip();
			ByteBuffer cur_buffer=ByteBuffer.allocateDirect((buffer_list.size()+1)*1024);
			for (ByteBuffer buf:buffer_list)
				cur_buffer.put(buf);
			cur_buffer.put(buffer);
			cur_buffer.flip();
			if ((decoder.decodable(cur_buffer))){
				return decoder.decode(cur_buffer);
			}else{
				buffer.flip();
				buffer_list.add(buffer);
				buffer.clear();
			}
		}
		if (count < 0) {
			System.err.println("channel close");
			socketChannel.close();
		}
		return null;
	}
	
	public HttpResponseMessage readResponse(SocketChannel socketChannel) throws Exception{
		HttpResponseDecoder decoder=new HttpResponseDecoder();
		int count=0;
		buffer.clear();
		List<byte[]> buffer_list=new ArrayList<byte[]>();
		while ((count = socketChannel.read(buffer)) > 0) {
			ByteBuffer cur_buffer=ByteBuffer.allocateDirect((buffer_list.size()+1)*1024);
			byte[] dst;
			for (byte[] buf:buffer_list){
				cur_buffer.put(buf);
			}
			dst=new byte[buffer.position()];
			buffer.flip();
			buffer.get(dst);
			cur_buffer.put(dst);
			cur_buffer.flip();
			if ((decoder.decodable(cur_buffer))){
				return decoder.decode(cur_buffer);
			}else{
				dst=new byte[buffer.position()];
				buffer.flip();
				buffer.get(dst);
				buffer_list.add(dst);
				buffer.clear();
			}
		}
		if (count < 0) {
			System.err.println("channel close");
			socketChannel.close();
			return null;
		}
		return null;
	}
	
	public void sendResponse(SocketChannel socketChannel,HttpResponseMessage message)  throws Exception{
		HttpResponseEncoder encoder=new HttpResponseEncoder();
		ByteBuffer buffer=encoder.encode(message);
		socketChannel.write(buffer);
	}
	
	public void sendRequest(SocketChannel socketChannel,HttpRequestMessage message) throws Exception{
		HttpRequestEncoder encoder=new HttpRequestEncoder();
		ByteBuffer buffer=encoder.encode(message);
		socketChannel.write(buffer);
	}
}
