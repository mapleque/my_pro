package com.sogou.test.nio.client;

public class NioTest extends SelectSoketsClientThreadPool {

	public static void main(String[] args) {
		dealArgs(args);
		displayEnv();
		(new Thread() {
			@Override
			public void run() {
				NioTest nt=new NioTest();
				nt.start();
				nt.resultOutput();
				System.exit(0);
			}
		}).start();
	}
	protected static void dealArgs(String[] args){
		if (args.length <= 0) {
			displayHelp();
		}
		try {
			for (int i = 0; i < args.length; i++) {
				if ("-h".equals(args[i])) {
					displayHelp();
				}
				if ("-c".equals(args[i])) {
					// connector number
					COMPLICATE = Integer.parseInt(args[i + 1]);
				}
				if ("-n".equals(args[i])) {
					// request number per connector
					REQUEST = Integer.parseInt(args[i + 1]);
				}
				if ("-a".equals(args[i])) {
					// ip address
					IP = args[i + 1];
				}
				if ("-p".equals(args[i])) {
					// port number
					PORT = Integer.parseInt(args[i + 1]);
				}
				if ("-f".equals(args[i])) {
					// sent content or sent content file
					SENT_CONTENT = args[i + 1];
				}
				if ("-u".equals(args[i])) {
					// sent URL
					SENT_URL = args[i + 1];
				}
				if ("-m".equals(args[i])) {
					// sent method
					METHOD = args[i + 1].toLowerCase();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			displayHelp();
		}
	}

	protected static void displayHelp() {
		System.out.println("Illustration:");
		System.out.println("********************************************");
		System.out.println("ARGS Usage:");
		System.out.println("-c:connector number");
		System.out.println("-n:request number per connector");
		System.out.println("-a:ip address");
		System.out.println("-p:port number");
		System.out.println("-f:sent content or sent content file");
		System.out.println("-u:sent URL");
		System.out.println("-m:sent method(post or get)");
		System.out.println("-h:help");
		System.out.println();
		System.out.println("Example:");
		System.out
				.println("~:/start_commend -c 10 -n 100 -a 10.12.18.196 -p 5555 -f /root/usr/url_file -u /");
		System.out.println("********************************************");
		System.exit(0);
	}

	protected static void displayEnv() {
		System.out.println("CURRENT ARGS:");
		System.out.println("_____________________________");
		System.out.println("complicate num:" + COMPLICATE);
		System.out.println("request num:" + REQUEST);
		System.out.println("ip addr:" + IP);
		System.out.println("port:" + PORT);
		System.out.println("data:" + SENT_CONTENT);
		System.out.println("url:" + SENT_URL);
		System.out.println("method:" + METHOD);
		System.out.println("_____________________________");
	}
	
	public static long startTime;// start time stamp
	public static long curTime;// current time stamp
	/**
	 * To output the result every 2sec in a independent thread.</br> Stopping
	 * when all thread finished.
	 * 
	 * @param curThread
	 *            The number of running threads
	 */
	protected final void resultOutput() {
		//output the tiltle
		System.out.println("RESULT:");
		System.out
				.println("******************************************************************************************************");
		System.out.println("" + "[TOTAL]" + "\t" + "[THREAD]" + "\t" + "[SENT]"
				+ "\t" + "[SENT_P]" + "\t" + "[SUCC_P]" + "\t" + "[EACH_TIME]"
				+ "\t" + "[FAIL]" + "\t" + "[TIME]" + "\t" + "");
		startTime = System.currentTimeMillis();
		
		//start send request
		try {
//			startSend();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//output the result per 2sec
		while (!finishFlag) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			curTime = System.currentTimeMillis();
			double sec = (curTime - startTime) / (1000.0);
			System.out.println("" + result[TOTAL] + "\t" + result[THREAD]
					+ "\t" + "\t" + result[SENT] + "\t"
					+ ((int) (result[SENT] / sec)) + "\t" + "\t"
					+ ((int) (result[SUCCESS] / sec)) + "\t" + "\t"
					+ (int) ( sec *1000/ result[SENT]) + "\t"
					+ "\t" + (int) (result[FAIL] / sec) + "\t" + +sec + "");
		}
		
		//output the result at last
		curTime = System.currentTimeMillis();
		double sec = (curTime - startTime) / (1000.0);
		System.out.println("" + result[TOTAL] + "\t" + result[THREAD] + "\t"
				+ "\t" + result[SENT] + "\t" + ((int) (result[SENT] / sec))
				+ "\t" + "\t" + ((int) (result[SUCCESS] / sec)) + "\t" + "\t"
				+ (int) (sec / result[SENT]) + "\t" + "\t"
				+ (int) (result[FAIL] / sec) + "\t" + +sec + "");
		System.out
				.println("******************************************************************************************************");
		System.out.println("TEST FINISHED");
	}
}
