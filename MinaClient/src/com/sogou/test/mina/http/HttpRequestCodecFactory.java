package com.sogou.test.mina.http;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

/**
 * for the client handler encoding and decoding
 * @author yangyang
 *
 */
public class HttpRequestCodecFactory extends DemuxingProtocolCodecFactory {

	public HttpRequestCodecFactory() {
		super.addMessageEncoder(HttpRequestMessage.class, HttpRequestEncoder.class);
		super.addMessageDecoder(HttpResponseDecoder.class);
	}
}
