package com.sogou.test.nio.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class HttpRequestMessage extends HttpMessage {

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
		this.setHeader("Host", "127.0.0.1"); // necessary data in HTTP/1.1
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

	public String generateMessageString() {
		if (outputMessage == null || isChanged) {
			// Parameters
			StringBuilder content = new StringBuilder(2048);
			for (Map.Entry<String, String> e : this.parameters.entrySet()) {
				if (e.getValue() != null)
					content.append(e.getKey()).append('=').append(
							urlEncodeUtf16le(e.getValue())).append('&');
			}
			if (this.parameters.size() > 0)
				content.deleteCharAt(content.length() - 1);

			StringBuilder str = new StringBuilder(3560);
			switch (method) {
			case GET:
				// set content type: utf-16le
				this.setHeader(STRING_CONTENTTYPE, STRING_CONTENTTYPE_VALUE);

				str.append(method.toString()).append(' ').append(url).append(
						content.toString()).append(" HTTP/1.1").append(
						STRING_CRLF);
				for (Map.Entry<String, String> e : this.headers.entrySet()) {
					str.append(e.getKey()).append(": ").append(e.getValue()).append(
							STRING_CRLF);
				}
				
				//str.append(STRING_CRLF).append(STRING_CRLF);
				//yangyang:here should end with only one CRLF
				str.append(STRING_CRLF);
				outputMessage = str.toString();
				break;

			case POST:
				// add LRCF size: 2
				//yangyang:the content end have no CRLF here,why + 2? 
				//this.setHeader(STRING_CONTENTLENGTH,
				//		Integer.toString(content.length() + 2));
				this.setHeader(STRING_CONTENTLENGTH,
						Integer.toString(content.length()));
				// set content type: utf-16le
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
		return outputMessage;
	}

	protected ByteBuffer encodeMessage() {
		byte[] message = generateMessageString().getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(message.length);
		buffer.put(message);
		return buffer;
	}

	static protected HttpRequestMessage decodeMessage(ByteBuffer in)
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
			String[] tokens = StringUtils.split(line, ' ');
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
				tokens = StringUtils.split(line, ':');
				if (tokens.length < 2)
					continue;
				message.setHeader(tokens[0].trim(), tokens[1].trim());
				line = reader.readLine();
			}
			// parameter
			line = reader.readLine();
			tokens = StringUtils.split(line, '&');
			for (int i = 0; i < tokens.length; i++) {
				String[] param = StringUtils.split(tokens[i], '=');
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
					else
						message.setParameters(param[0].trim(),
								URLDecoder.decode(param[1].trim(), "GBK"));
				}
			}
//			LoggerHelper.appPrint(LoggerHelper.TRACE, message,
//					"decode message is : " + message);
			return message;
		} else if (findByteArray(in, 0, 2, BYTES_GET) == 0) {
			// GET METHOD
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
			String[] tokens = StringUtils.split(line, ' ');
			if (tokens.length < 3)
				return null;
			
			// create instance
			HttpRequestMessage message = new HttpRequestMessage(
					HttpMethod.GET, tokens[1]);

			// parameter
			int idx = tokens[1].lastIndexOf("?");
			if(idx > 0) {
				line = tokens[1].substring(idx+1);
				tokens = StringUtils.split(line, '&');
				for (int i = 0; i < tokens.length; i++) {
					String[] param = StringUtils.split(tokens[i], '=');
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
						else
							message.setParameters(param[0].trim(),
									URLDecoder.decode(param[1].trim(), "GBK"));
					}
				}
			}

			// header
			line = reader.readLine();
			while (line != null) {
				if (line.isEmpty())
					break;
				tokens = StringUtils.split(line, ':');
				if (tokens.length < 2)
					continue;
				message.setHeader(tokens[0].trim(), tokens[1].trim());
				line = reader.readLine();
			}
			
			return message;
		} else
			throw new IllegalStateException();
	}

}
