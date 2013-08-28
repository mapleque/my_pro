package com.sogou.test.nio.http;

import java.nio.ByteBuffer;

public class HttpResponseEncoder{

	public ByteBuffer encode(HttpResponseMessage message) throws Exception {
		ByteBuffer buffer = message.encodeMessage();
		buffer.flip();
		return buffer;
	}
}
