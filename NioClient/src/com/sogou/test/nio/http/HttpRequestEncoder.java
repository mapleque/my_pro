package com.sogou.test.nio.http;

import java.nio.ByteBuffer;

public class HttpRequestEncoder{

	public ByteBuffer encode( HttpRequestMessage message) throws Exception {
		ByteBuffer buffer = message.encodeMessage();
		buffer.flip();
		return buffer;
	}
}
