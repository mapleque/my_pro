package com.sogou.test.nio.http;

import java.nio.ByteBuffer;

/**
 * 
 * @author yangyang@sogou-inc.com
 * 
 */
public class HttpResponseDecoder {

	public boolean decodable(ByteBuffer in) {
		// Return NEED_DATA if the whole header is not read yet.
		boolean result;
		try {
			result = HttpResponseMessage.isMessageComplete(in);
		} catch (Exception ex) {
			ex.printStackTrace();
			result = false;
		}
		return result;
	}

	public HttpResponseMessage decode(ByteBuffer in) throws Exception {
		try {
			HttpResponseMessage message = HttpResponseMessage.decodeMessage(in);
			if (message != null) {
				return message;
			} else
				return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
