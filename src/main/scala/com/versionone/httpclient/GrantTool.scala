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
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
          Failure(conn.getResponseCode(), conn.getResponseMessage(), Source.fromInputStream(conn.getErrorStream()).mkString)
        else
          Success(Source.fromInputStream(conn.getInputStream()).mkString)
      }
    }
  }
  
  def post(url:String, postBody:String, contentType:String, username:String, password:String) = {
    new URL(url).openConnection() match {
      case conn : HttpURLConnection => {
        val auth = new sun.misc.BASE64Encoder().encode((username + ":" + password).getBytes())
        conn.setRequestProperty("Authorization", "Basic " + auth); 
        conn.setRequestProperty("Content-Type", contentType); 
        conn.setDoOutput(true)
        conn.setRequestMethod("POST")
        val writer = new java.io.OutputStreamWriter(conn.getOutputStream())
        writer.write(postBody)
        writer.close()
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
          Failure(conn.getResponseCode(), conn.getResponseMessage(), Source.fromInputStream(conn.getErrorStream()).mkString)
        else
          Success(Source.fromInputStream(conn.getInputStream()).mkString)
      }
    }
  }
  
  
  def makeSecrets(baseurl:String, username:String, password:String) : String = {
    val body = s"""{
      "client_name": "Integration ${new java.util.Date().toGMTString()}",
      "client_type": "Public",
      "client_apikey": "",
      "redirect_uri": "urn:ietf:wg:oauth:2.0:oob"
      }"""
    
    post(baseurl + "/ClientRegistration.mvc/Register", body, "application/json", username, password) match {
      case Failure(code, msg, respbody) =>
        sys error s"Unable to register client. $msg\n$respbody"
      case Success(respbody) => {
        respbody
      }
    }
  }
  
  def getGrantCode(baseUrl:String, grantUrl:String, username:String, password:String) : String = {
    get(grantUrl, username, password) match {
      case Failure(code, msg, txt) =>
        sys error s"Unable to reach grant url $grantUrl: $code $msg\n$txt"
      case Success(txt) => {
        val authRegex = """<input type="hidden" name="auth_request" value="(.*)"/>""".r
        val actionRegex = """action='(.*)'""".r
        //val x = xml.XML loadString s"""<?xml version="1.0" encoding="utf-8"?>\n$txt"""
        val action = actionRegex findFirstIn txt match {
          case Some(actionRegex(action)) => action
        }
        //val action = x \\ "form" \ "@action"
        val auth_request = authRegex findFirstIn txt match {
          case Some(authRegex(auth)) => auth
        }
        //val auth_request = x \\ "input" \ "@value"
        val postBody = s"""allow=true&auth_request=${java.net.URLEncoder.encode(auth_request)}"""
        val postUrl = baseUrl + action
        post(postUrl, postBody, "application/x-www-form-urlencoded", username, password) match {
          case Failure(code, msg, txt) =>
            sys error s"Unable to post auth code to $action: $code $msg\n$txt"
          case Success(txt) => {
            val codeRegex = """<textarea id="successcode">(.*)</textarea>""".r
            //val x = xml.XML loadString txt
            //val code = (x \\ "textarea").text
            val code = codeRegex findFirstIn txt match {
              case Some(codeRegex(code)) => code
            }
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
  
  (myargs.username, myargs.password, myargs.url) match {
    case (Some(user), Some(pass), Some(url)) =>  {
      val secrets = V1Oauth2Api.makeSecrets(url, user, pass)
      val s = new java.io.PrintWriter(myargs.secrets)
      s.write(s"""{
        "installed":
          $secrets
        }""")
      s.close()
    }
    case (None, None, None) =>
      println("Skipping permitted app creation")
    case _ =>
      sys error "Username, password, and url all required for automatic permitted app creation"
  }
  
  
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
        
    val code = (myargs.username, myargs.password, myargs.url) match {
      case (Some(user), Some(pass), Some(url)) => {
        val uurrll = new URL(url)
        val base = uurrll.getProtocol() + "://" + uurrll.getHost()
        println(s"trying to get grant form from $grantUrl")
        V1Oauth2Api.getGrantCode(base.toString(), grantUrl, user, pass)
      }
      case (None, None, None) => {
        println("Skipping auto grant creation")
        println(s"Please visit:\n\n$grantUrl\n\nAnd paste the code you receive here:\n")
        //   await pasted auth code
        System.console().readLine()
      }
    }        
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