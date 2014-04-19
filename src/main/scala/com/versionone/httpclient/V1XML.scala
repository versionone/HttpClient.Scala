package com.versionone.httpclient

import scala.language.postfixOps
import scala.collection.JavaConversions._

/**
 * Functions to transform the XML produced and consumed by V1 REST endpoints
 */
object V1XML {
  type Assets = List[Map[String, List[Any]]]

  /**
   * Types of attribute updates
   */
  sealed trait Action
  case class SetAttr(name: String, action: String, value: String) extends Action
  case class SetRel(name: String, action: String, idref: String) extends Action
  case class SetMulti(name: String, actions: List[(String, String)]) extends Action

  def maybeList(s: String) = {
    if (s != null && s != "") List(s) else List()
  }

  /**
   * deconstruct versionone XML response into Map[String,Any]s
   */
  def deconstruct(document: scala.xml.Elem): Assets = {
    (for (asset <- document \ "Asset") yield {
      (
        List("idref" -> List((asset \ "@idref").text))
        ++
        ((for (attr <- asset \ "Attribute") yield {
          val multiValues = (for (value <- attr \ "Value") yield value.text)
          (attr \ "@name").text -> (maybeList(attr.text) ++ multiValues)
        }) toList)
        ++
        ((for (rel <- asset \ "Relation") yield {
          val rassets = for (rasset <- rel \ "Asset") yield { Map("idref" -> (rel \ "@idref").text) }
          (rel \ "@name").text -> (rassets toList)
        }) toList)) toMap
    }) toList
  }

  /**
   * Construct REST XML update body from list of Attribute actions
   */
  def fromActions(attrs: List[Action]) =
    <Asset>
      {
        for (attr <- attrs) yield attr match {
          case SetAttr(name, action, value) =>
            <Attribute name={ name } act="set">{ value }</Attribute>
          case SetRel(name, action, idref) =>
            <Relation name={ name } act={ action }>
              <Asset idref={ idref }/>
            </Relation>
          case SetMulti(name, actions) =>
            <Relation name={ name }>
              {
                for ((action, idref) <- actions)
                  <Asset idref={ idref } act={ action }/>
              }
            </Relation>
        }
      }
    </Asset>
}
