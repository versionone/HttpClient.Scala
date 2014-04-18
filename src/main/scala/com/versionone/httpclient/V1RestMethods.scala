package com.versionone.httpclient

import scala.language.postfixOps
import scala.collection.JavaConversions._

sealed trait Action
case class SetAttr(name:String, action:String, value:String) extends Action
case class SetRel(name:String, action:String, idref:String) extends Action
case class SetMulti(name:String, actions:List[(String,String)]) extends Action

/**
 * An HTTP client that parses the body as an XML document
 */
trait XmlHttpClient extends HttpClient {
  /**
   * Performs the request and parses the body of the HTTP response with the scala XML parser.
   */
  def DoXml(path:String, body:xml.Elem) = {
    val (status, content, getParam) =
      DoRequest(path, if(body==null) null else body.toString())
    (status, xml.XML.loadString(content), getParam)
  }
}


object V1XML {
  type Assets = List[Map[String,List[Any]]]
  
  def maybeList(s:String) = {
    if(s != null && s != "") List(s) else List()
  }
  
  /**
   * deconstruct versionone XML response into Map[String,Any]s 
   */
  def asBasicTypes(document:scala.xml.Elem) : Assets  = {
    val assets  = for (asset <- document \ "Asset") yield {
      
      val attrs = for (attr <- asset \ "Attribute") yield {
        (attr \ "@name").text -> (maybeList(attr.text) ++ (for (value <- attr \ "@Value")  yield value.text))
        }
        
      val rels = for (rel <- asset \ "Relation") yield {
        val rassets = for (rasset <- rel \ "Asset") yield { Map("idref" -> (rel \ "@idref").text) }
        (rel \ "@name").text -> (rassets toList)
      }
      ( List("idref" -> List((asset \ "@idref").text))
        ++ (attrs toList)
        ++ (rels toList)
      ) toMap
    }
    assets toList
  }
}

/**
 * Methods oriented toward the "rest-1.v1" style XML apis of VersionOne instances.
 */
trait V1RestMethods extends XmlHttpClient {
  
  /**
   * Return an XML asset template.
   * The template has values filled out from the context object, if possible.
   */
  def getTemplate(assetType:String, contextRef:String) =
    DoXml("rest-1.v1/New/{assetType}?ctx={contextRef}", null)
    
  /**
   * Return the rest-1 asset XML for a given asset type and ID
   */
  def getAsset(assetType:String, id:Int) =
    DoXml("rest-1.v1/Data/{assetType}/{id}", null)
    
  /**
   * Create a new given Attribute actions
   */
  def createAsset(assetType:String, data:List[Action]) =
    DoXml("rest-1.v1/Data/{assetType}", genAssetXml(data))

  /**
   * Update an asset given Attribute actions
   */
  def updateAsset(assetType:String, id:Int, data:List[Action]) =
    DoXml("rest-1.v1/Data/{assetType}/{id}", genAssetXml(data))
  
  /**
   * given attribute actions, generate rest-1 style asset update XML 
   */
  private def genAssetXml (attrs:List[Action]) =
    <Asset>
      {for (attr <- attrs) yield attr match {
        case SetAttr(name, action, value) =>
          <Attribute name={name} act="set">{value}</Attribute>
        case SetRel(name, action, idref) =>
          <Relation name={name} act={action}>
            <Asset idref={idref} />
          </Relation>
        case SetMulti(name, actions) =>
          <Relation name={name}>
            {for ((action,idref) <- actions) 
             <Asset idref={idref} act={action}/>}
          </Relation>
	    }
      }
    </Asset>
}