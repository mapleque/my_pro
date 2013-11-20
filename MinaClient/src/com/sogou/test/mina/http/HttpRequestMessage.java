package com.sogou.test.mina.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;


public class HttpRequestMessage extends HttpMessage {

	public String encodeType = "UTF8";

	public String getEncodeType() {
		return encodeType;
	}

	public void setEncodeType(String encodeType) {
		this.encodeType = encodeType;
	}

	static protected final byte[] BYTES_GET = "GET".getBytes();
	static protected final byte[] BYTES_POST = "POST".getBytes();

	static public enum HttpMethod {
		GET,
		POST;
	}

	protected HttpMethod method;
	protected String url;
//	protected String protocol;
	protected HashMap<String, String> headers = new HashMap<String, String>();
	protected HashMap<String, String> parameters = new HashMap<String, String>();

	private String outputMessage = null;
	private boolean isChanged = true;

	public HttpRequestMessage(HttpMethod method, String url) {
		this.method = method;
		this.url = url;
//    	this.protocol = "HTTP/1.1";
		this.setHeader("Host", "default"); // necessary data in HTTP/1.1
	}

	public void setHeader(String name, String value) {
		isChanged = true;
		headers.put(name, value);
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public void setParameters(String name, String value) {
		if (name != null && value != null) {
			isChanged = true;
			parameters.put(name, value);
		}
	}

	public String getParameter(String name) {
		return parameters.get(name);
	}

	/**
	 * 
	 * @return
	 */
	public String getMessageParamsString() {
		//StringBuilder logStr = new StringBuilder();
		StringBuilder content = new StringBuilder();
		if (outputMessage == null || isChanged) {
			for (Map.Entry<String, String> e : this.parameters.entrySet()) {
				if (e.getValue() != null) {
					if ("GBK".equals(encodeType)
							|| "UTF8".equals(encodeType)
							|| "GB18030".equals(encodeType)) {
						content.append(e.getKey()).append('=').append(
								e.getValue()).append('&');
					} else {
						content.append(e.getKey()).append('=').append(
								urlEncodeUtf16le(e.getValue())).append('&');
					}
				}
			}
			if (this.parameters.size() > 0)
				content.deleteCharAt(content.length() - 1);
		}
		return content.toString();
	}

	public String generateMessageString() {
		if (outputMessage == null || isChanged) {
			// Parameters
			StringBuilder content = new StringBuilder(2048);
			for (Map.Entry<String, String> e : this.parameters.entrySet()) {
				if (e.getValue() != null) {
					if ("GBK".equals(encodeType)
							|| "UTF8".equals(encodeType)
							|| "GB18030".equals(encodeType)) {
						content.append(e.getKey()).append('=').append(
								e.getValue()).append('&');
					} else {
						content.append(e.getKey()).append('=').append(
								urlEncodeUtf16le(e.getValue())).append('&');
					}
				}
			}
			if (this.parameters.size() > 0)
				content.deleteCharAt(content.length() - 1);

			StringBuilder str = new StringBuilder(3560);
			switch (method) {
			case GET:
				// set content type: utf-16le
				if ("GBK".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_GBKVALUE);
				else if ("GB18030".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_GB18030VALUE);
				else if ("UTF8".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_UTF8VALUE);
				else
					this.setHeader(STRING_CONTENTTYPE, STRING_CONTENTTYPE_VALUE);

				str.append(method.toString()).append(' ').append(url).append("?").append(
						content.toString()).append(" HTTP/1.1").append(
						STRING_CRLF);
				for (Map.Entry<String, String> e : this.headers.entrySet()) {
					str.append(e.getKey()).append(": ").append(e.getValue()).append(
							STRING_CRLF);
				}
				str.append(STRING_CRLF);
				outputMessage = str.toString();
				break;

			case POST:
				this.setHeader(
						STRING_CONTENTLENGTH,
						Integer.toString(content.toString().getBytes().length));
				if ("GBK".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_GBKVALUE);
				else if ("GB18030".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_GB18030VALUE);
				else if ("UTF8".equals(encodeType))
					this.setHeader(STRING_CONTENTTYPE,
							STRING_CONTENTTYPE_UTF8VALUE);
				else
					this.setHeader(STRING_CONTENTTYPE, STRING_CONTENTTYPE_VALUE);

				str.append(method.toString()).append(' ').append(url).append(
						" HTTP/1.1").append(STRING_CRLF);
				for (Map.Entry<String, String> e : this.headers.entrySet()) {
					str.append(e.getKey()).append(": ").append(e.getValue()).append(
							STRING_CRLF);
				}
				str.append(STRING_CRLF);
				str.append(content.toString());
				outputMessage = str.toString();
				break;
			}
		}
		System.err.println();
		System.err.println("Request format data");
		System.err.println("********************************");
		System.err.println(outputMessage);
		System.err.println("********************************");
		return outputMessage;
	}

	protected IoBuffer encodeMessage() {
		byte[] message = generateMessageString().getBytes();
		IoBuffer buffer = IoBuffer.allocate(message.length);
		buffer.put(message);
		return buffer;
	}

	static protected HttpRequestMessage decodeMessage(IoBuffer in)
			throws Exception {
		if (findByteArray(in, 0, 3, BYTES_POST) == 0) {
			// get message in string format
			byte[] bytes = new byte[in.remaining()];
			in.get(bytes);
			// prepare stream
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			// URI
			String line = reader.readLine();
			if (line == null)
				return null;
			String[] tokens = line.split(" ");
			if (tokens.length < 3)
				return null;
			// create instance
			HttpRequestMessage message = new HttpRequestMessage(
					HttpMethod.POST, tokens[1]);

			// header
			line = reader.readLine();
			while (line != null) {
				if (line.isEmpty())
					break;
				tokens = line.split(":");
				if (tokens.length < 2)
					continue;
				message.setHeader(tokens[0].trim(), tokens[1].trim());
				line = reader.readLine();
			}
			// parameter
			line = reader.readLine();
			
			
			tokens = line.split("&");
			for (int i = 0; i < tokens.length; i++) {
				String[] param = tokens[i].split("=");
				switch (param.length) {
				case 0:
					message.setParameters(tokens[i].trim(), "");
					break;
				case 1:
					message.setParameters(param[0].trim(), "");
					break;
				default:
					String code = message.getHeader(STRING_CONTENTTYPE);
					if (code != null && code.indexOf("UTF-16LE") != -1)
						message.setParameters(param[0].trim(),
								urlDecodeUtf16le(param[1].trim()));
					else if (code != null && code.indexOf("UTF-8") != -1)
						message.setParameters(param[0].trim(),
								URLDecoder.decode(param[1].trim(), "UTF-8"));
					else if (code != null && code.indexOf("GB18030") != -1)
						message.setParameters(param[0].trim(),
								URLDecoder.decode(param[1].trim(), "GB18030"));
					else
						message.setParameters(param[0].trim(),
								URLDecoder.decode(param[1].trim(), "GBK"));
				}
			}
			
			return message;
		} else if (findByteArray(in, 0, 2, BYTES_GET) == 0) {
			// the last 4 bytes should be BYTES_CRLF
			throw new IllegalStateException();
		} else
			throw new IllegalStateException();
	}

}
