package com.sogou.test.mina.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;

public class HttpResponseMessage extends HttpMessage {

	public String encodeType = "UTF8";

	static public enum HttpStatus {
		Success(200, "OK"), NotFound(404, "Not Found"), Unauthorized(401,
				"Unauthorized"), NotAllow(405, "Method Not Allow");
		private int retCode;
		private String description;

		private HttpStatus(int retCode, String description) {
			this.retCode = retCode;
			this.description = description;
		}

		public String getReturnCode() {
			return Integer.toString(retCode);
		}

		public String getDescription() {
			return this.description;
		}
	}

	protected HttpStatus status;

	public boolean isSucceeded() {
		return status == HttpStatus.Success;
	}

	public String getStatus() {
		return status.getReturnCode() + ":" + status.getDescription();
	}

	// protected String protocol;
	protected HashMap<String, String> headers = new HashMap<String, String>();
	protected char[] contentRecved = null;
	protected byte[] contentToSend = null;

	private String outputHeader = null;
	private boolean isChanged = true;

	public HttpResponseMessage(HttpStatus status) {
		this.status = status;
		// this.protocol = "HTTP/1.1";
	}

	public void setHeader(String name, String value) {
		isChanged = true;
		headers.put(name, value);
	}

	public HashMap<String, String> getHeader() {
		return headers;
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public void setReplyContentToSend(byte[] replyContent) {
		isChanged = true;
		this.contentToSend = replyContent;
	}

	public char[] getReplyContentRecved() {
		return this.contentRecved;
	}

	public void setReplyContentRecved(char[] d) {
		this.contentRecved = d;
	}

	public String generateMessageHeaderStringToSend() {
		if (outputHeader == null || isChanged) {
			int length = contentToSend == null ? 0 : contentToSend.length;
			this.setHeader(STRING_CONTENTLENGTH, Integer.toString(length + 2));

			StringBuilder str = new StringBuilder(512);
			str.append("HTTP/1.1 ").append(status.getReturnCode()).append(' ')
					.append(status.getDescription()).append(STRING_CRLF);
			for (Map.Entry<String, String> e : this.headers.entrySet()) {
				str.append(e.getKey()).append(':').append(e.getValue())
						.append(STRING_CRLF);
			}
			outputHeader = str.toString();
		}
		return outputHeader;
	}

	protected IoBuffer encodeMessage() {
		byte[] messageHeader = generateMessageHeaderStringToSend().getBytes();
		int length = contentToSend == null ? 0 : contentToSend.length;
		IoBuffer buffer = IoBuffer.allocate(messageHeader.length + length + 4);
		buffer.put(messageHeader);
		buffer.put(BYTES_CRLF);
		if (contentToSend != null)
			buffer.put(contentToSend);
		buffer.put(BYTES_CRLF);
		return buffer;
	}

	static protected HttpResponseMessage decodeMessage(IoBuffer in,
			int headerSize, int contentSize, boolean chunked,
			String chunkedContent) throws Exception {
		// get Header
		byte[] headerData = new byte[headerSize];
		in.get(headerData, 0, headerSize);
		// read Header string
		ByteArrayInputStream stream = new ByteArrayInputStream(headerData);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		// URI
		String line = reader.readLine();
		if (line == null)
			return null;
		String[] tokens = line.split(" ");
		if (tokens.length < 3)
			return null;
		// create instance
		HttpStatus status;
		if (tokens[1].equals(Integer.toString(HttpStatus.Success.retCode)))
			status = HttpStatus.Success;
		else if (tokens[1]
				.equals(Integer.toString(HttpStatus.NotFound.retCode)))
			status = HttpStatus.NotFound;
		else if (tokens[1].equals(Integer
				.toString(HttpStatus.Unauthorized.retCode)))
			status = HttpStatus.Unauthorized;
		else if (tokens[1]
				.equals(Integer.toString(HttpStatus.NotAllow.retCode)))
			status = HttpStatus.NotAllow;
		else
			throw new IllegalStateException();
		HttpResponseMessage message = new HttpResponseMessage(status);

		line = reader.readLine();
		while (line != null) {
			if (line.isEmpty())
				break;
			tokens = line.split(":");
			if (tokens.length >= 2) {
				message.setHeader(tokens[0].trim(), tokens[1].trim());
				if ("Content-Type".equals(tokens[0].trim())) {
					if (tokens[1].indexOf("charset=GBK") != -1)
						message.encodeType = "GBK";
					else if (tokens[1].indexOf("charset=GB18030") != -1)
						message.encodeType = "GB18030";
				}
			}
			line = reader.readLine();
		}
		if (chunked) {
			message.contentRecved = chunkedContent.toCharArray();
		} else {
			if (contentSize > 0) {
				if ("GBK".equals(message.encodeType)
						|| "UTF8".equals(message.encodeType)
						|| "GB18030".equals(message.encodeType)) {
					in.position(headerSize + 2);
					in.order(ByteOrder.LITTLE_ENDIAN);
					// CharBuffer buffer = in.asCharBuffer();
					Charset charset = null;
					CharsetDecoder decoder = null;
					charset = Charset.forName(message.encodeType);
					decoder = charset.newDecoder();
					String content = in.getString(decoder);
					message.contentRecved = content.toCharArray();
				} else {
					// copy content
					in.position(headerSize + 2);
					in.order(ByteOrder.LITTLE_ENDIAN);
					CharBuffer buffer = in.asCharBuffer();
					char[] data = new char[contentSize / 2];
					buffer.get(data, 0, contentSize / 2);
					message.contentRecved = data;
				}
			}
		}

		// move buffer pos
		in.position(headerSize + 2 + contentSize);
		return message;
	}
}
