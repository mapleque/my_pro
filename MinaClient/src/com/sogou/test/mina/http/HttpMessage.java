package com.sogou.test.mina.http;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public abstract class HttpMessage {

	static protected final String STRING_CRLF = "\r\n";
	static protected final byte[] BYTES_CRLF = STRING_CRLF.getBytes();
	static protected final String STRING_CRLFCRLF = "\r\n\r\n";
	static protected final byte[] BYTES_CRLFCRLF = STRING_CRLFCRLF.getBytes();
	static protected final String STRING_CONTENTLENGTH = "Content-Length";
	static protected final byte[] BYTES_CONTENTLENGTH = STRING_CONTENTLENGTH.getBytes();
	static protected final String STRING_CONTENTTYPE = "Content-Type";
	static protected final String STRING_CONTENTTYPE_VALUE = "application/x-www-form-urlencoded;charset=UTF-16LE";
	static protected final String STRING_CONTENTTYPE_UTF8VALUE = "application/x-www-form-urlencoded;charset=UTF-8";
	static protected final String STRING_CONTENTTYPE_GBKVALUE = "application/x-www-form-urlencoded;charset=GBK";
	static protected final String STRING_CONTENTTYPE_GB18030VALUE = "application/x-www-form-urlencoded;charset=GB18030";

	static protected int findByteArray( IoBuffer buffer, byte[] array ) {
		return findByteArray(buffer, 0, buffer.remaining() - 1, array);
	}

	static protected int findByteArray(IoBuffer buffer, int startIndex,
			int endIndex, byte[] array) {
		if (startIndex < 0 || endIndex >= buffer.remaining()
				|| endIndex - startIndex + 1 < array.length)
			return -1;

		int endLoop = endIndex - array.length + 1;
		for (int i = startIndex; i <= endLoop; i++) {
			boolean hit = true;
			for (int j = 0; j < array.length; j++)
				if (buffer.get(i + j) != array[j]) {
					hit = false;
					break;
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
			e.printStackTrace();
			return null;
		}
	}

	static public final String SESSION_ATTR_HEADERSIZE = "http.HeaderSize";
	static public final String SESSION_ATTR_CONTENTSIZE = "http.ContentSize";

	/**
	 * 
	 * @param session
	 * @param in
	 * @return
	 * @throws Exception
	 */
	static protected boolean isMessageComplete(IoSession session, IoBuffer in)
			throws Exception {
		int receivedSize = in.remaining();
		if (receivedSize < 4)
			return false;

		Object obj;
		int headerSize = -1, contentSize = -1;
		obj = session.getAttribute(SESSION_ATTR_HEADERSIZE);
		if (obj == null) {
			// find a empty line
			if ((headerSize = findByteArray(in, BYTES_CRLFCRLF)) < 0)
				return false;
			headerSize += 2; // + 2 to add the last CRLF back
			session.setAttribute(SESSION_ATTR_HEADERSIZE, headerSize);
		} else
			headerSize = (Integer) obj;

		obj = session.getAttribute(SESSION_ATTR_CONTENTSIZE);
		if (obj == null) {
			int contentLengthStart, contentLengthEnd;
			// find Content-Length
			if ((contentLengthStart = findByteArray(in, 0, headerSize,
					BYTES_CONTENTLENGTH)) < 0)
				return false;
			if ((contentLengthEnd = findByteArray(in, contentLengthStart,
					headerSize, BYTES_CRLF)) < 0)
				return false;
			// get Content-Length string
			int start = contentLengthStart + BYTES_CONTENTLENGTH.length + 1;
			int length = contentLengthEnd - contentLengthStart
					- BYTES_CONTENTLENGTH.length;
			in.position(start);
			byte[] bytes = new byte[length];
			in.get(bytes, 0, length);
			String stringContentLength = new String(bytes);
			// parse string, remove : and blank space
			contentSize = Integer.parseInt(stringContentLength.trim());
			session.setAttribute(SESSION_ATTR_CONTENTSIZE, contentSize);
		} else
			contentSize = (Integer) obj;

		if (headerSize + 2 + contentSize == receivedSize)
			return true;
		else
			return false;
	}
}
