package com.versionone.httpclient.sampleApp;

import com.versionone.httpclient.OAuth2Settings;
import com.versionone.httpclient.V1HttpClient;
import com.versionone.httpclient.SimpleLogger;
import com.versionone.httpclient.V1Methods;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class JavaSampleApp {
	
	public static void main(String[] args) {
		com.versionone.httpclient.OAuth2Settings settings = OAuth2Settings.fromFiles("client_secrets.json", "stored_credentials.json");
		
		final Logger l4jlog = LoggerFactory.getLogger("SampleApp");
		SimpleLogger logAdapter = new SimpleLogger() {
			public void info(String msg) { l4jlog.info(msg); }
			public void debug(String msg) { l4jlog.debug(msg); }
			public void error(String msg) { l4jlog.error(msg); }
		};
		
		V1Methods v1 = new V1HttpClient(settings, logAdapter, "V1Http-Sample/1.0");
		
		Object me = v1.Query(
			"from: Member" +
			"select:" +
			"  - Name" +
			"where:" +
			"  isSelf: true"
			);
		
		System.out.println(me);
	}
}
