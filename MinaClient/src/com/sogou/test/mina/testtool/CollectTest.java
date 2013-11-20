package com.sogou.test.mina.testtool;

import java.util.HashMap;

import com.sogou.test.mina.http.HttpResponseMessage;

public class CollectTest extends AbstractTest {

	public static void main(String[] args) {
//		 String
//		 header="Connection:keep-alive&Host:zhihui.sogou.com&Cookie:zhihui-sfwsku-s=378ffRq1PJOeQUsA5G16v5vq8E1CPjONL/PDvuDIwLowXDdRihrL33yaulVf8+X65QBnFFqodLwNIOwRn5E16w==;zhihui-sfwsku-m=ZbZ5mOCUOMohqNayjuFFqhg0LepC6RSDBBRcF91KTw7vHbE117mY2rD2PI29ijbBaeZ6DoJZhC39wY67FyOqQJD5nvUR+pMiOuBAXP5FTCY=;";
//		 args=("-t 0 -c 1 -n 100 -p 80 -a 10.16.135.25 -u /mine/info -f format=json -h "+header).split(" ");

//		String form = "token1=2%7C1384414065%7C0%7CbG9naW5pZDowOnx1c2VyaWQ6MjA6dGVzdDg4Nzc2NkBzb2dvdS5jb218c2VydmljZXVzZToyMDowMDEwMDAwMDAwMDAwMDAwMDAwMHxjcnQ6MTA6MjAxMy0xMS0xNHxlbXQ6MTowfGFwcGlkOjQ6MTEyMHx0cnVzdDoxOjF8cGFydG5lcmlkOjE6MHxyZWxhdGlvbjowOnx1dWlkOjE2OmI3NTI1ZWI2YjQxOTQ2MXN8dWlkOjE2OmI3NTI1ZWI2YjQxOTQ2MXN8dW5pcW5hbWU6MDp8&token2=Oj8_x_Oxt0uxEbkgpqZB-7HNpqu0t-2OTa36f99o53BsTwlUma7g8AeT0tv1VmJsYatV4b9PMvlmq5bS7uVX8Geem9Pnn9igmalC7z70MEivXDV1s2jDsSjDbi4SGKnjfBtlfqpaQjPfPKkzM7T3hHCmMtL4YYzHhHkOXlKwkW4&userid=test887766%40sogou.com&nickName=hehe&_method=get";
//		String header = "Connection:keep-alive&Host:zhihui.sogou.com";
//		args = ("-c 1 -n 1 -p 80 -a 10.16.135.25 -u /auth -m POST -f " + form
//				+ " -h " + header).split(" ");

		(new CollectTest(args)).start();
	}

	public CollectTest(String[] args) {
		this.dealArgs(args);
	}

	@Override
	public void onRecieved(HttpResponseMessage response) {
		System.err.println();
		System.err.println("Response data:");
		System.err.println("********************************");
		if (response == null) {
			System.err.println("null response");
		} else {
			try {
				System.err.println("Status");
				System.err.println(response.getStatus());
				System.err.println("Header:");
				HashMap<String, String> header = response.getHeader();
				for (String key : header.keySet())
					System.err.println(key + ":" + header.get(key));
				System.err.println("Content:");
				System.err.println(response.getReplyContentRecved());
			} catch (Exception e) {
				System.err.println(response.getStatus());
			}
		}
		System.err.println("********************************");
	}

	@Override
	public void onFinishedC() {
		// System.out.println(this.finished);
	}

	@Override
	public void onStop() {

		System.out.println("####################");
		System.out
				.println("send:" + this.sendN + "\trecieved:" + this.recieveN);
		long spend = (this.stopMillis - this.startMillis);
		System.out.println("spend total:" + spend + "ms\tspend per request:"
				+ (spend / this.n) + "ms");
	}

	@Override
	public void onSend() {
		int send = this.sendN;
		int recieve = this.recieveN;
		long start = this.startMillis;
		long sp = System.currentTimeMillis() - start;

		int n = this.n / 10;
		if (n == 0)
			return;
		int u = send % n;
		int v = send / n;
		if (v < 10 && u == 0) {
			System.out.println(v + "0% send:" + send + "\trecieved:" + recieve
					+ "\tspend:" + sp + "ms");
		}
	}

	/**
	 * parameters interpreter
	 * 
	 * @param args
	 */
	protected void dealArgs(String[] args) {
		if (args.length <= 0) {
			// help information
			displayHelp();
		}
		try {
			for (int i = 0; i < args.length; i++) {

				if ("-c".equals(args[i])) {
					// connector number
					this.c = Integer.parseInt(args[i + 1]);
				}
				if ("-n".equals(args[i])) {
					// request number per connector
					this.n = Integer.parseInt(args[i + 1]);
				}
				if ("-a".equals(args[i])) {
					// ip address
					this.host = args[i + 1];
				}
				if ("-p".equals(args[i])) {
					// port number
					this.port = Integer.parseInt(args[i + 1]);
				}
				if ("-f".equals(args[i])) {
					// sent content or sent content file
					String[] kvs = args[i + 1].split("&");
					for (String kv : kvs) {
						String[] d = kv.split("=");
						this.form.put(d[0], d[1]);
					}
				}
				if ("-h".equals(args[i])) {
					// help information
					String[] kvs = args[i + 1].split("&");
					for (String kv : kvs) {
						String[] d = kv.split(":");
						this.header.put(d[0], d[1]);
					}
				}

				if ("-u".equals(args[i])) {
					// sent URL
					this.path = args[i + 1];
				}
				if ("-m".equals(args[i])) {
					// sent method
					this.method = args[i + 1].toUpperCase();
				}
				if ("-t".equals(args[i])) {
					// connector number
					this.timeout = Integer.parseInt(args[i + 1]);
				}
			}
			this.displayEnv();
		} catch (Exception e) {
			e.printStackTrace();
			// help information
			displayHelp();
		}
	}

	/**
	 * echo help information
	 */
	protected void displayHelp() {
		System.out.println();
		System.out.println("Illustration:");
		System.out.println("********************************************");
		System.out.println("ARGS Usage:");
		System.out.println("-c:connector number");
		System.out.println("-n:request number per connector");
		System.out.println("-a:host ip address");
		System.out.println("-p:port number");
		System.out.println("-f:post form with x-www-form-urlencoded");
		System.out.println("-u:sent path");
		System.out.println("-m:sent method(POST or GET)");
		System.out.println("-h:sent header with x-www-form-urlencoded");
		System.out.println("-t:none recieve waiting time (ms)");
		System.out.println();
		System.out.println("Example:");
		System.out
				.println("~:/start_commend -c 10 -n 100 -a 10.12.18.196 -p 5555 -u /");
		System.out.println("********************************************");
		System.exit(0);
	}

	/**
	 * echo current environment parameters
	 */
	protected void displayEnv() {
		System.out.println();
		System.out.println("CURRENT PARAM:");
		System.out.println("_____________________________");
		System.out.println("complicate num:" + this.c);
		System.out.println("request num:" + this.n);
		System.out.println("host:" + this.host);
		System.out.println("port:" + this.port);
		System.out.println("path:" + this.path);
		System.out.println("header:" + this.header);
		System.out.println("form:" + this.form);
		System.out.println("method:" + this.method);
		System.out.println("_____________________________");
	}

}
