package com.versionone.httpclient

/**
 * An HTTP client that parses the body as an XML document
 */
trait XmlHttpClient extends HttpClient {
  /**
   * Performs the request and parses the body of the HTTP response with the scala XML parser.
   */
  def DoXml(path: String, body: xml.Elem) = {
    val (status, content, getParam) =
      DoRequest(path, if (body == null) null else body.toString())
    (status, xml.XML.loadString(content), getParam)
  }
}

/**
 * Methods oriented toward the "rest-1.v1" style XML apis of VersionOne instances.
 */
trait V1RestMethods extends XmlHttpClient {
  /**
   * Performs the request and parses the body of the HTTP response with the V1 REST XML parser
   */
  def DoAsset(pathSuffix: String, actions: List[V1XML.Action]) = {
    val xmlRequest = if (actions == null) null else V1XML fromActions actions
    val (status, xmlResponse, getP) = DoXml(pathSuffix, xmlRequest)
    (status, V1XML deconstruct xmlResponse, getP)
  }
  
  /**
   * Return an XML asset template.
   * The template has values filled out from the context object, if possible.
   */
  def getTemplate(assetType: String, contextRef: String) =
    DoAsset("rest-1.v1/New/{assetType}?ctx={contextRef}", null)

  /**
   * Return the rest-1 asset data for a given asset type and ID
   */
  def getAsset(assetType: String, id: Int) =
    DoAsset("rest-1.v1/Data/{assetType}/{id}", null)

  /**
   * Create a new asset given Attribute actions
   */
  def createAsset(assetType: String, data: List[V1XML.Action]) =
    DoAsset("rest-1.v1/Data/{assetType}", data)

  /**
   * Update an asset given Attribute actions
   */
  def updateAsset(assetType: String, id: Int, data: List[V1XML.Action]) =
    DoAsset("rest-1.v1/Data/{assetType}/{id}", data)
}
