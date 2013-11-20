package com.sogou.test.mina.http;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

public class HttpResponseDecoder extends MessageDecoderAdapter {
	
	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		// Return NEED_DATA if the whole header is not read yet.
		MessageDecoderResult result;
		try {
			result = HttpRequestMessage.isMessageComplete(session, in) ? MessageDecoderResult.OK : MessageDecoderResult.NEED_DATA;
		} catch (Exception ex) {
			ex.printStackTrace();
			result = MessageDecoderResult.NOT_OK;
		}
		return result;
	}
	
	@Override
	public MessageDecoderResult decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		MessageDecoderResult result;
		try {
			int headerSize = (Integer) session.getAttribute(HttpMessage.SESSION_ATTR_HEADERSIZE);
			int contentSize = (Integer) session.getAttribute(HttpMessage.SESSION_ATTR_CONTENTSIZE);
			HttpResponseMessage message = HttpResponseMessage.decodeMessage(in,
					headerSize, contentSize);
			if (message != null) {
				out.write(message);
				session.removeAttribute(HttpMessage.SESSION_ATTR_HEADERSIZE);
				session.removeAttribute(HttpMessage.SESSION_ATTR_CONTENTSIZE);
				result = MessageDecoderResult.OK;
			} else
				result = MessageDecoderResult.NEED_DATA;
		} catch (Exception ex) {
			ex.printStackTrace();
			result = MessageDecoderResult.NOT_OK;
		}
		return result;
	}

	
}
