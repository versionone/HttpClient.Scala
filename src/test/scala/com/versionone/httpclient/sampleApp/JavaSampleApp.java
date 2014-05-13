package com.versionone.httpclient.sampleApp;

import com.versionone.httpclient.*;

import org.slf4j.*;

import scala.Option;

public class JavaSampleApp {
	
	public static void main(String[] args) {
		Option<OAuth2Settings> maybeSettings = OAuth2Settings.fromFiles(
			"client_secrets.json",
			"stored_credentials.json" );
		
		if(maybeSettings.isEmpty()) {
			System.err.println("Unable to read OAuth2 files.");
		} else {
			OAuth2Settings settings = maybeSettings.get();
			SimpleLogger logAdapter = new SimpleLogger() {
				Logger log = LoggerFactory.getLogger(JavaSampleApp.class);
				public void info(String msg) { log.info(msg); }
				public void debug(String msg) { log.debug(msg); }
				public void error(String msg) { log.error(msg); }
			};
			
			V1Methods v1 = new V1HttpClient(settings, logAdapter, "V1Http-JavaSample/1.0");
			
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
}
