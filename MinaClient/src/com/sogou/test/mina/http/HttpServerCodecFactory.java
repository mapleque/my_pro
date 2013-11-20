package com.sogou.test.mina.http;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

public class HttpServerCodecFactory extends DemuxingProtocolCodecFactory {

	public HttpServerCodecFactory() {
		super.addMessageEncoder(HttpResponseMessage.class, HttpResponseEncoder.class);
		super.addMessageDecoder(HttpRequestDecoder.class);
	}
}
