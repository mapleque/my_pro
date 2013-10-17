package com.sogou.test.nio.http;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author yangyang@sogou-inc.com
 * 
 */
public class HttpResponseMessage extends HttpMessage {

	static public enum HttpStatus {
		Success(200, "OK"), NotFound(404, "Not Found");
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

	protected ByteBuffer encodeMessage() {
		byte[] messageHeader = generateMessageHeaderStringToSend().getBytes();
		int length = contentToSend == null ? 0 : contentToSend.length;
		ByteBuffer buffer = ByteBuffer.allocate(messageHeader.length + length
				+ 4);
		buffer.put(messageHeader);
		buffer.put(BYTES_CRLF);
		if (contentToSend != null)
			buffer.put(contentToSend);
		buffer.put(BYTES_CRLF);
		return buffer;
	}

	static protected HttpResponseMessage decodeMessage(ByteBuffer in)
			throws Exception {
		in.position(0);
		int headerSize = -1, contentSize = 0;
		// find a empty line
		if ((headerSize = findByteArray(in, BYTES_CRLFCRLF)) < 0)
			return null;
		headerSize += 2; // + 2 to add the last CRLF back

		int contentLengthStart = -1, contentLengthEnd = -1;
		// find Content-Length
		contentLengthStart = findByteArray(in, 0, headerSize,
				BYTES_CONTENTLENGTH);
		if (contentLengthStart > 0) {
			contentLengthEnd = findByteArray(in, contentLengthStart,
					headerSize, BYTES_CRLF);
		}
		// get Content-Length string
		if (contentLengthStart > 0 && contentLengthEnd > contentLengthStart) {
			int start = contentLengthStart + BYTES_CONTENTLENGTH.length + 1;
			int length = contentLengthEnd - contentLengthStart
					- BYTES_CONTENTLENGTH.length;
			in.position(start);
			byte[] bytes = new byte[length];
			in.get(bytes, 0, length);
			String stringContentLength = new String(bytes);
			// parse string, remove : and blank space
			contentSize = Integer.parseInt(stringContentLength.trim());
		}

		in.position(0);
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
		String[] tokens = StringUtils.split(line, ' ');
		if (tokens.length < 3)
			return null;
		// create instance
		HttpStatus status;
		if (tokens[1].equals(Integer.toString(HttpStatus.Success.retCode)))
			status = HttpStatus.Success;
		else if (tokens[1]
				.equals(Integer.toString(HttpStatus.NotFound.retCode)))
			status = HttpStatus.NotFound;
		else
			throw new IllegalStateException();
		HttpResponseMessage message = new HttpResponseMessage(status);

		line = reader.readLine();
		while (line != null) {
			if (line.isEmpty())
				break;
			tokens = StringUtils.split(line, ':');
			if (tokens.length >= 2)
				message.setHeader(tokens[0].trim(), tokens[1].trim());
			line = reader.readLine();
		}

		if (contentSize > 0) {
			// copy content
			in.position(headerSize + 2);
			in.order(ByteOrder.LITTLE_ENDIAN);
			CharBuffer buffer = in.asCharBuffer();
			char[] data = new char[contentSize / 2];
			buffer.get(data, 0, contentSize / 2);
			message.contentRecved = data;
		}

		// move buffer pos
		in.position(headerSize + 2 + contentSize);
		return message;
	}
}
