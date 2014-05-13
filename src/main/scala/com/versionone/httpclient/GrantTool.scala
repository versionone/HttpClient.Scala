package com.versionone.httpclient.granttool

import org.apache.oltu.oauth2.client._
import org.apache.oltu.oauth2.common.message.types._
import scala.language.postfixOps
import scala.util.parsing.json._
import sys.process._

/**
 * @author ${user.name}
 */
object GrantTool extends App {
  case class Args(
    secrets: String       = "client_secrets.json",
    creds:   String       = "stored_credentials.json",
    scopes:  List[String] = Nil
    )
    
  def parseArgs (parsed:Args, remainingArgs:List[String]) : Args  =
    remainingArgs match {
      case Nil => parsed
      case "--secrets" :: secrets :: tail => parseArgs(parsed.copy(secrets = secrets), tail)
      case "--creds"   :: creds   :: tail => parseArgs(parsed.copy(creds = creds), tail)    
      case                scope   :: tail => parseArgs(parsed.copy(scopes = scope::parsed.scopes), tail)
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
    println("Code received. Attemping to trade for access token...")
    
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
    println(s"\ncredentials written to ${myargs.creds}")
  }
}