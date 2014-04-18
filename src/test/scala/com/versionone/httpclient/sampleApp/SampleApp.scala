package com.versionone.httpclient.sampleApp

import com.versionone.httpclient._
import org.slf4j.LoggerFactory

object SampleApp {
  def main(args: Array[String]): Unit = {
    
    val settings = OAuth2Settings.fromFiles(
        "client_secrets.json",
        "stored_credentials.json" )
        
    val l4jlog = LoggerFactory.getLogger("SampleApp")
    val logAdapter = new SimpleLogger {
      def debug(s:String) = l4jlog debug s
      def info(s:String) = l4jlog info s
      def error(s:String) = l4jlog error s
    }
    
    val client = new V1HttpClient(settings, logAdapter, "V1Http-Sample/1.0")
    
    val me = client.Query("""
      from: Member
      select:
        - Name
      where:
        isSelf: true
      """)
    println(s"$me")
  }

}