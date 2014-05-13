package com.versionone.httpclient.granttool

import org.apache.oltu.oauth2.client._
import org.apache.oltu.oauth2.common.message.types._
import scala.language.postfixOps
import scala.util.parsing.json._
import sys.process._
import java.net._
import com.versionone.httpclient._

import scala.io.Source

/**
 * @author ${user.name}
 */

object V1Oauth2Api {
  
  sealed trait HttpResponse
  case class Success(body:String) extends HttpResponse
  case class Failure(code:Int, message:String, body:String) extends HttpResponse
  
  def get(url:String, username:String, password:String) = {
    new URL(url).openConnection() match {
      case conn : HttpURLConnection => {
        val auth = new sun.misc.BASE64Encoder().encode((username + ":" + password).getBytes())
        conn.setRequestProperty("Authorization", "Basic " + auth); 
        conn.setRequestMethod("GET")
        def getBody() = Source.fromInputStream(conn.getInputStream()).mkString
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
          Failure(conn.getResponseCode(), conn.getResponseMessage(), getBody())
        else
          Success(getBody())
      }
    }
  }
  
  def post(url:String, postBody:String, username:String, password:String) = {
    new URL(url).openConnection() match {
      case conn : HttpURLConnection => {
        val auth = new sun.misc.BASE64Encoder().encode((username + ":" + password).getBytes())
        conn.setRequestProperty("Authorization", "Basic " + auth); 
        conn.setDoOutput(true)
        conn.setRequestMethod("POST")
        val writer = new java.io.OutputStreamWriter(conn.getOutputStream())
        writer.write(postBody)
        writer.close()
        def getBody() = Source.fromInputStream(conn.getInputStream()).mkString
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
          Failure(conn.getResponseCode(), conn.getResponseMessage(), getBody())
        else
          Success(getBody())
      }
    }
  }
  
  
  def makeSecrets(baseurl:String, username:String, password:String) : Option[Map[String,Any]] = {
    val body = s"""{
      "client_name": "Integration ${new java.util.Date().toGMTString()}",
      "client_type": "Public",
      "client_apikey": "",
      "redirect_uri": "urn:ietf:wg:oauth:2.0:oob"
      }"""
    
    post(baseurl + "/ClientRegistration.mvc/register", body, username, password) match {
      case Failure(code, msg, respbody) =>
        sys error s"Unable to register client. $msg\n$respbody"
      case Success(respbody) =>
        for {
          Some(JMap(response)) <- JSON.parseFull(respbody)
          JStr(client_id) = response("client_id")
          JStr(client_name) = response("client_name")
          JStr(redirect_uri) = response("redirect_uri")
          JStr(client_secret) = response("client_secret")
          JStr(server_base_uri) = response("server_base_uri")
          JStr(auth_uri) = response("auth_uri")
          JStr(token_uri) = response("token_uri")
        } yield
          Map("installed" -> response)
    }
  }
  
  def getGrantCode(baseUrl:String, grantUrl:String, username:String, password:String) : String = {
    get(grantUrl, username, password) match {
      case Failure(code, msg, txt) =>
        sys error s"Unable to reach grant url $grantUrl: $code $msg\n$txt"
      case Success(txt) => {
          val x = xml.XML loadString txt
          val action = x \\ "form" \ "@action"
          val auth_request = x \\ "input" \ "@value"
          val postBody = s"""allow=true&auth_request=$auth_request"""
          post(baseUrl + action, postBody, username, password) match {
            case Failure(code, msg, txt) =>
              sys error s"Unable to post auth code to $action: $code $msg\n$txt"
            case Success(txt) => {
              val x = xml.XML loadString txt
              val code = (x \\ "textarea").text
              code
            }
          }
      }
    }
  }
  
  
}

object GrantTool extends App {
  case class Args(
    secrets:  String         = "client_secrets.json",
    creds:    String         = "stored_credentials.json",
    scopes:   List[String]   = Nil,
    username: Option[String] = None,
    password: Option[String] = None,
    url:      Option[String] = None
    )
    
  def parseArgs (parsed:Args, remainingArgs:List[String]) : Args  =
    remainingArgs match {
      case Nil => parsed
      case "--secrets"  :: secrets :: tail => parseArgs(parsed.copy(secrets  = secrets),              tail)
      case "--creds"    :: creds   :: tail => parseArgs(parsed.copy(creds    = creds),                tail)
      case "--username" :: user    :: tail => parseArgs(parsed.copy(username = Some(user)),           tail)
      case "--password" :: pass    :: tail => parseArgs(parsed.copy(password = Some(pass)),           tail)
      case "--url"      :: url     :: tail => parseArgs(parsed.copy(url      = Some(url)),            tail)
      case                 scope   :: tail => parseArgs(parsed.copy(scopes   = scope::parsed.scopes), tail)
    }
  
  val myargs = parseArgs(new Args(), args toList)

  //   get scope from command line
  assert(myargs.scopes != Nil, "Must provide at least one scope to request.")
  
  //   read client secrets
  val maybeSettings = com.versionone.httpclient.OAuth2Settings.fromFiles(myargs.secrets, myargs.creds)

  for (settings <- maybeSettings) {
    //   print grant url to console
    val grantUrl = request.OAuthClientRequest
        .authorizationLocation(settings.authUri)
        .setClientId(settings.clientId)
        .setRedirectURI(settings.redirectUri)
        .setScope(myargs.scopes mkString " ")
        .setResponseType("code")
        .buildQueryMessage()
        .getLocationUri()    
    println(s"Please visit:\n\n$grantUrl\n\nAnd paste the code you receive here:\n")

    //   await pasted auth code
    val code = System.console().readLine()
    println("\nCode received. Attemping to trade for access token...")
    
    //   contact server token endpoint
    val req = request.OAuthClientRequest
        .tokenLocation(settings.tokenUri)
        .setGrantType(GrantType.AUTHORIZATION_CODE)
        .setClientId(settings.clientId)
        .setClientSecret(settings.clientSecret)
        .setRedirectURI(settings.redirectUri)
        .setCode(code)
        .buildBodyMessage()
    val oAuthClient = new OAuthClient(new URLConnectionClient())
    val creds = oAuthClient.accessToken(req).getOAuthToken()

    //   write stored_credentials.json
    val p = new java.io.PrintWriter(myargs.creds)
    p.write(JSONObject(Map(
      "access_token"  -> creds.getAccessToken(),
      "refresh_token" -> creds.getRefreshToken(),
      "scope"         -> creds.getScope(),
      "expires_in"    -> creds.getExpiresIn()
    )).toString())
    p.close()
    println(s"\nSuccess. Credentials written to ${myargs.creds}")
  }
}