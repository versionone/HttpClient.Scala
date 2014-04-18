package com.versionone.httpclient.tests

import com.versionone.httpclient._
import org.apache.oltu.oauth2.common.token._

import scala.util.parsing.json.JSON
import scala.io.Source

import org.slf4j.LoggerFactory


object SampleApp {
  def parseFile (filename:String) = JSON parseFull Source.fromFile(filename).mkString
  
  def readCreds(storedCreds:String) = {
    val Some(JMap(creds)) = parseFile(storedCreds)
    val JStr(scope) = creds("scope")
    val JStr(refreshToken) = creds("refresh_token")
    val JStr(accessToken) = creds("access_token")
    new BasicOAuthToken(
	      accessToken,
	      600, // TODO see if this is used anywhere and get rid of magic number
	      refreshToken,
	      scope
	      )
  }
  
  def readSettings(clientSecrets:String, storedCreds:String) = {
    val Some(JMap(secrets)) = parseFile(clientSecrets)
    val JMap(installed) = secrets("installed")
    val JStr(clientId) = installed("client_id")
    val JStr(clientSecret) = installed("client_secret")
    val JStr(authUri) = installed("auth_uri")
    val JStr(tokenUri) = installed("token_uri")
    val JStr(serverBase) = installed("server_base_uri")
    val JList(redirectUris) = installed("redirect_uris")
    val redirs = for (JStr(url) <- redirectUris) yield url
    OAuth2Settings(
	  readCreds(storedCreds),
	  clientId,
	  clientSecret, 
	  serverBase,
	  tokenUri,
	  authUri,
	  redirs.head
	  )
  }
  
  def main(args: Array[String]): Unit = {
    val settings = readSettings("client_secrets.json", "stored_credentials.json")
    val log = LoggerFactory.getLogger("SampleApp")
    val client = new V1HttpClient(settings, log, "ClarityOne-Test/1.0")
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