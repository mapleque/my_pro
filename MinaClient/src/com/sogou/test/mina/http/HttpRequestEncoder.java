package com.sogou.test.mina.http;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

public class HttpRequestEncoder implements MessageEncoder<HttpRequestMessage> {

	@Override
	public void encode(IoSession session, HttpRequestMessage message,
			ProtocolEncoderOutput out) throws Exception {
		IoBuffer buffer = message.encodeMessage();
		buffer.flip();
		out.write( buffer );
	}

}
