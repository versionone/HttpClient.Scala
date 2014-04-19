package com.versionone.httpclient

/**
 * Grab-bag of methods related to VersionOne implementation
 */
trait V1MiscMethods extends XmlHttpClient {
  /**
   * Poll for the server's current Date.
   * Used by the ClarityOne connector to establish dates for server queries.
   */
  def getServerDate() = {
    val (status, bodyXml, getP) = DoXml("notification.v1", null)
    val dateXml = bodyXml \\ "feed" \ "updated"
    javax.xml.bind.DatatypeConverter.parseDateTime(dateXml.text).getTime()
  }
}

/**
 * The collection of all the VersionOne specific methods that require the HTTP client
 */
trait V1Methods extends HttpClient
  with V1MiscMethods
  with V1RestMethods
  with V1QueryMethods

/**
 * An HTTP client with VersionOne specific methods
 */
class V1HttpClient(settings: OAuth2Settings, log: SimpleLogger, agent: String)
  extends OAuth2HttpClient(settings, log, agent)
  with V1Methods
  