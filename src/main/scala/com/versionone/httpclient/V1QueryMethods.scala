package com.versionone.httpclient

import scala.util.parsing.json._

// from http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes

class CC[T] {
  def unapply(a: Any): Option[T] =
    Some(a.asInstanceOf[T])
}

object JMap extends CC[Map[String, Any]]
object JList extends CC[List[Any]]
object JStr extends CC[String]
object JNum extends CC[Double]
object JBool extends CC[Boolean]

/**
 * An HTTP client that parses the body as JSON document
 */
trait JsonHttpClient extends HttpClient {
  /**
   * Performs the request and parses the body of the HTTP response with the scala JSON parser.
   */
  def DoJson(pathSuffix: String, body: String) = {
    val (status, content, getParam) = DoRequest(pathSuffix, body)
    (status, JSON.parseFull(content), getParam)
  }
}

/**
 * VersionOne-oriented methods for the HTTP Client
 */
trait V1QueryMethods extends JsonHttpClient {

  /**
   * Return multiple result sets from a VersionOne query.v1 YAML or JSON query body.
   */
  def QueryAll(body: String) = {
    val (status, json, getParam) = DoJson("query.v1", body)
    for {
      Some(JList(resultsets)) <- List(json)
      JList(resultset) <- resultsets
    } yield {
      for (JMap(result) <- resultset) yield result
    }
  }

  /**
   * Return the first result set from a VersionOne query.v1 YAML or JSON query body.
   */
  def Query(body: String) = QueryAll(body).head
}

