package com.versionone.httpclient.sampleApp

import com.versionone.httpclient._
import org.slf4j.LoggerFactory

object SampleApp {
  
  def main(args: Array[String]): Unit = {
    OAuth2SettingsFuncs fromFiles("client_secrets.json", "stored_credentials.json" ) match {
      case None =>
        sys error "Unable to read Oauth2 settings"
      case Some(settings) => {
        object logAdapter extends SimpleLogger {
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
  }
}
