package com.versionone.httpclient.granttool

import org.apache.oltu.oauth2.client._

import com.versionone.httpclient.OAuth2Settings

/**
 * @author ${user.name}
 */
object App {
  
  case class Args(
    secrets: String = "client_secrets.json",
    creds: String = "stored_credentials.json",
    scopes: List[String] = Nil
    )
  
  def parseArgs (parsed:Args, remainingArgs:List[String]) : Args  = remainingArgs match {
    case Nil => parsed
    case "--secrets" :: secretsFile :: tail => parseArgs(parsed.copy(secrets = secretsFile), tail)
    case "--creds" :: credsFile :: tail => parseArgs(parsed.copy(creds = credsFile), tail)    
    case scope :: tail => parseArgs(parsed.copy(scopes = scope::parsed.scopes), tail)
  }
  
  def main(argv : Array[String]) {
    
    val args = parseArgs(new Args(), argv toList)
    
    // mode 1:
    //   read client secrets
    //   get scope from command line
    //   print grant url to console
    //   await pasted auth code
    //   contact server token endpoint
    //   write stored_credentials.json
    
    assert(args.scopes != Nil, "Must provide at least one scope to request.")
    
    val settings = com.versionone.httpclient.OAuth2Settings.fromFiles(args.secrets, args.creds)
    
    val grantUrl = request.OAuthClientRequest
          .authorizationLocation(settings.authUri)
          .setClientId(settings.clientId)
          .setRedirectURI(settings.redirectUri)
          .setScope("")
          .buildQueryMessage()
          .getLocationUri()    
          
    printf(s"Please paste the code from $grantUrl")
    
    
    val code = System.console().readLine()
          
    val req = request.OAuthClientRequest
          .tokenLocation(settings.tokenUri)
          .setGrantType(org.apache.oltu.oauth2.common.message.types.GrantType.AUTHORIZATION_CODE)
          .setClientId(settings.clientId)
          .setClientSecret(settings.clientSecret)
          .setRedirectURI(settings.redirectUri)
          .setCode(code)
          .buildBodyMessage()
          
    val oAuthClient = new OAuthClient(new URLConnectionClient())
    
    val creds = oAuthClient.accessToken(req).getOAuthToken()
    
    // mode 2:
    //   get scope and url and username and password from command line
    //   hit client creation endpoint to create client
    //   store client_secrets.json
    //   if gui:
    //     open system browser with grant url
    //   else:
    //     print grant url to console
    //   if listen:
    //     (this is a race with the user, do it before printing the url to eliminate the race)
    //     open http listener and await request from V1
    //   else:
    //     print "Paste code:" prompt on console and await readline   
    //   contact server token endpoint
    //   write stored_credentials.json
    
    println( "Hello World!" )
  }

}