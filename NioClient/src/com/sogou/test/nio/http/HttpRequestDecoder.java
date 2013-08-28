package com.sogou.test.nio.http;

import java.nio.ByteBuffer;

/**
 * 
 * @author yangyang@sogou-inc.com
 * 
 */
public class HttpRequestDecoder {

	public boolean decodable(ByteBuffer in) {
		// Return NEED_DATA if the whole header is not read yet.
		boolean result;
		try {
			result = HttpRequestMessage.messageComplete(in);
		} catch (Exception ex) {
			result = false;
		}
		return result;
	}

	public HttpRequestMessage decode(ByteBuffer in) throws Exception {
		// Try to decode body
		HttpRequestMessage message = HttpRequestMessage.decodeMessage(in);
		return message;
	}

}
