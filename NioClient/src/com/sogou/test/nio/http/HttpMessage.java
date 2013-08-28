package com.sogou.test.nio.http;

import java.nio.ByteBuffer;

public abstract class HttpMessage {

	static protected final String STRING_CRLF = "\r\n";
	static protected final byte[] BYTES_CRLF = STRING_CRLF.getBytes();
	static protected final String STRING_CRLFCRLF = "\r\n\r\n";
	static protected final byte[] BYTES_CRLFCRLF = STRING_CRLFCRLF.getBytes();
	static protected final String STRING_CONTENTLENGTH = "Content-Length";
	static protected final byte[] BYTES_CONTENTLENGTH = STRING_CONTENTLENGTH
			.getBytes();
	static protected final String STRING_CONTENTTYPE = "Content-Type";
	static protected final String STRING_CONTENTTYPE_VALUE = "application/x-www-form-urlencoded;charset=UTF-16LE";

	static protected int findByteArray(ByteBuffer buffer, byte[] array) {
		return findByteArray(buffer, 0, buffer.remaining() - 1, array);
	}

	static protected int findByteArray(ByteBuffer buffer, int startIndex,
			int endIndex, byte[] array) {
		if (startIndex < 0 || endIndex >= buffer.remaining()
				|| endIndex - startIndex + 1 < array.length)
			return -1;

		int endLoop = endIndex - array.length + 1;
		for (int i = startIndex; i <= endLoop; i++) {
			boolean hit = true;
			for (int j = 0; j < array.length; j++) {
				if (buffer.get(i + j) != array[j]) {
					hit = false;
					break;
				}
			}
			if (hit)
				return i;
		}
		return -1;
	}

	private static final char[] HEX_TABLE = new char[] { '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', };

	private static final void encodeByte(StringBuilder sb, int n) {
		sb.append('%');
		sb.append(HEX_TABLE[(n >> 4) & 0x0f]);
		sb.append(HEX_TABLE[(n) & 0x0f]);
	}

	private static final void encodeChar(StringBuilder sb, char ch) {
		encodeByte(sb, ch & 0xff);
		encodeByte(sb, (ch >> 8) & 0xff);
	}

	static protected String urlEncodeUtf16le(String value) {
		if (value == null)
			return null;

		StringBuilder message = new StringBuilder(value.length() * 6);
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			encodeChar(message, ch);
		}
		return message.toString();
	}

	static protected String urlDecodeUtf16le(String message) {
		if (message == null)
			return null;

		try {
			int length = message.length() / 6;
			StringBuilder utf16Data = new StringBuilder(length);
			for (int i = 0; i < length; i++) {
				// do not check % to improve performance, charAt is very slow
				int stringIndex = i * 6;
				String s = message.substring(stringIndex + 1, stringIndex + 3);
				int low = Integer.parseInt(s, 16);
				s = message.substring(stringIndex + 4, stringIndex + 6);
				int high = Integer.parseInt(s, 16);
				utf16Data.append((char) ((high << 8) | low));
			}
			return utf16Data.toString();
		} catch (Exception e) {
			return null;
		}
	}

	static public final String SESSION_ATTR_HEADERSIZE = "http.HeaderSize";
	static public final String SESSION_ATTR_CONTENTSIZE = "http.ContentSize";
	
	static protected int completeMessageLength(ByteBuffer in){
		int receivedSize = in.remaining();
		if (receivedSize < 4)
			return -1;

		int headerSize = -1, contentSize = 0;
			// find a empty line
			if ((headerSize = findByteArray(in, BYTES_CRLFCRLF)) < 0)
				return -1;
			headerSize += 2; // + 2 to add the last CRLF back

			int contentLengthStart=-1, contentLengthEnd=-1;
			// find Content-Length
			contentLengthStart = findByteArray(in, 0, headerSize,
					BYTES_CONTENTLENGTH);
			if (contentLengthStart > 0){
				contentLengthEnd = findByteArray(in, contentLengthStart,
					headerSize, BYTES_CRLF);
			}
			// get Content-Length string
			if (contentLengthStart > 0 && contentLengthEnd > contentLengthStart){
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
			return headerSize + 2 + contentSize;
	}
	static protected boolean isMessageComplete(ByteBuffer in)
			throws Exception {
		int receivedSize = in.remaining();
		if (receivedSize < 4)
			return false;

		int headerSize = -1, contentSize = 0;
			// find a empty line
			if ((headerSize = findByteArray(in, BYTES_CRLFCRLF)) < 0)
				return false;
			headerSize += 2; // + 2 to add the last CRLF back

			int contentLengthStart=-1, contentLengthEnd=-1;
			// find Content-Length
			contentLengthStart = findByteArray(in, 0, headerSize,
					BYTES_CONTENTLENGTH);
			if (contentLengthStart > 0){
				contentLengthEnd = findByteArray(in, contentLengthStart,
					headerSize, BYTES_CRLF);
			}
			// get Content-Length string
			if (contentLengthStart > 0 && contentLengthEnd > contentLengthStart){
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

		if (headerSize + 2 + contentSize == receivedSize)
			return true;
		else
			return false;
	}
	
	protected static boolean messageComplete(ByteBuffer in) {
		int last = in.remaining() - 1;
		if (in.remaining() < 4) {
			return false;
		}

		// to speed up things we check if the Http request is a GET or POST
		if (in.get(0) == (byte) 'G' && in.get(1) == (byte) 'E'
				&& in.get(2) == (byte) 'T') {
			// Http GET request therefore the last 4 bytes should be 0x0D 0x0A
			// 0x0D 0x0A
			return in.get(last) == (byte) 0x0A
					&& in.get(last - 1) == (byte) 0x0D
					&& in.get(last - 2) == (byte) 0x0A
					&& in.get(last - 3) == (byte) 0x0D;
		} else if (in.get(0) == (byte) 'P' && in.get(1) == (byte) 'O'
				&& in.get(2) == (byte) 'S' && in.get(3) == (byte) 'T') {
			// Http POST request
			// first the position of the 0x0D 0x0A 0x0D 0x0A bytes
			int eoh = -1;
			for (int i = last; i > 2; i--) {
				if (in.get(i) == (byte) 0x0A && in.get(i - 1) == (byte) 0x0D
						&& in.get(i - 2) == (byte) 0x0A
						&& in.get(i - 3) == (byte) 0x0D) {
					eoh = i + 1;
					break;
				}
			}
			if (eoh == -1) {
				return false;
			}
			for (int i = 0; i < last; i++) {
				boolean found = false;
				for (int j = 0; j < BYTES_CONTENTLENGTH.length; j++) {
					if (in.get(i + j) != BYTES_CONTENTLENGTH[j]) {
						found = false;
						break;
					}
					found = true;
				}
				if (found) {
					// retrieve value from this position till next 0x0D 0x0A
					StringBuilder contentLength = new StringBuilder();
					for (int j = i + BYTES_CONTENTLENGTH.length; j < last; j++) {
						if (in.get(j) == 0x0D) {
							break;
						}
						if (in.get(j) == 0x3A) {
							continue;
						}
						contentLength.append((char) in.get(j));
					}
					// if content-length worth of data has been received then
					// the message is complete
					return Integer.parseInt(contentLength.toString().trim())
							+ eoh == in.remaining();
				}
			}
		}

		// the message is not complete and we need more data
		return false;
	}
}
