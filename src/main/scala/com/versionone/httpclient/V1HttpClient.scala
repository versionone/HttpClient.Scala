package com.versionone.httpclient

import org.slf4j.Logger

/**
 * Grab-bag of methods related to VersionOne implementation
 */
trait V1MiscMethods extends XmlHttpClient {
  //val serverDateFormat = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
  //val notificationUpdateDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

  /**
   * Quickly gain the server's current Date by doing an empty-bodied query.
   * Used by the ClarityOne connector to establish dates for server queries.
   */
  def getServerDate() = {
    val (status, body, getParam) = DoXml("notification.v1", null)
    val date = (body \\ "feed" \ "updated").text
    val dd = javax.xml.bind.DatatypeConverter.parseDateTime(date).getTime()
    dd
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
class V1HttpClient(settings: OAuth2Settings, log:SimpleLogger, agent: String)
  extends OAuth2HttpClient(settings, log, agent)
  with V1Methods
  