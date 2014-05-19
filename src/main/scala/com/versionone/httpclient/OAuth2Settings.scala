package com.versionone.httpclient

import org.apache.oltu.oauth2.common.token._
import scala.util._
import scala.util.parsing.json.JSON
import scala.io.Source

/**
 * Settings for the OAuth2HttpClient
 */
case class OAuth2Settings(
  creds: Option[OAuthToken],
  clientId: String,
  clientSecret: String,
  baseUri: String,
  tokenUri: String,
  authUri: String,
  redirectUri: String)

object OAuth2SettingsFuncs {

  def parseFile(filename: String) = {
    try
      JSON.parseFull(Source.fromFile(filename).mkString)
    catch {
      case ex:java.io.FileNotFoundException => None
    }
  }

  def readCreds(storedCreds: String) = {
    for {
      JMap(creds) <- parseFile(storedCreds)
      JStr(scope) = creds("scope")
      JStr(refreshToken) = creds("refresh_token")
      JStr(accessToken) = creds("access_token")
    } yield
      new BasicOAuthToken(
        accessToken,
        600, // TODO see if this is used anywhere and get rid of magic number
        refreshToken,
        scope
        )
  }
  
  def readSecrets(clientSecrets: String) = {
    for {
      JMap(secrets) <- parseFile(clientSecrets)
      JMap(installed) = secrets("installed")
      JStr(clientId) = installed("client_id")
      JStr(clientSecret) = installed("client_secret")
      JStr(authUri) = installed("auth_uri")
      JStr(tokenUri) = installed("token_uri")
      JStr(serverBase) = installed("server_base_uri")
    } yield {
      val redir =
        if (installed.contains("redirect_uris")) {
    	  val redirs = for {
    	    JList(urls) <- List(installed("redirect_uris"))
    	    JStr(url) <- urls
    	    } yield url
    	  redirs.head
    	} else {
    	  installed("redirect_uri") match {
    	    case s : String => s
    	  }
    	}
      OAuth2Settings(
        None,
        clientId,
        clientSecret,
        serverBase,
        tokenUri,
        authUri,
        redir
        )
    }
  }

  def fromFiles(clientSecrets: String, storedCreds: String) = {
    for {
      secrets <- readSecrets(clientSecrets)
    } yield {
      secrets.copy(creds=readCreds(storedCreds))
    }
  }
}
