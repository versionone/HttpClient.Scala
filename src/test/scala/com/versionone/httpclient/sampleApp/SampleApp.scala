package com.versionone.httpclient.sampleApp

import com.versionone.httpclient._
import org.slf4j.LoggerFactory

object SampleApp {
  
  def main(args: Array[String]): Unit = {
    val settings = OAuth2Settings fromFiles (
        "client_secrets.json",
        "stored_credentials.json" )
        
    val logAdapter = new SimpleLogger() {
      val log = LoggerFactory getLogger "SampleApp"
      def debug(s:String) = log debug s
      def info(s:String) = log info s
      def error(s:String) = log error s
    }
    
    val v1 = new V1HttpClient(settings, logAdapter, "V1Http-ScalaSample/1.0")
    
    val me = v1 Query """
      from: Member
      select:
        - Name
      where:
        isSelf: true
      """
      
    println(s"$me")
  }
}
