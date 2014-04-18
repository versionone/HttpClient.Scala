package com.versionone.httpclient.tests

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.versionone.httpclient._

@RunWith(classOf[JUnitRunner])
class JsonParsingTest extends FunSuite with ShouldMatchers {
  
  test("parses JSON object with all types") {
    val json = """{
      "bool1": true,
      "num1": 123,
      "num2": 125.25,
      "string1": "Hi, tis a string",
      "list1": [1,2,3],
      "list2": [{"a":"a1", "b":"b1"}, {"a":"a2", "b":"b2"}],
	  "map1": {
	    "bool1": true,
	    "num1": 123,
	    "num2": 125.25,
	    "string1": "Hi, tis a string",
        "list1": [1,2,3]
	    }
      }"""
    val parsed = scala.util.parsing.json.JSON.parseFull(json)
    val Some(JMap(root)) = parsed
    val JBool(bool1) = root("bool1")
    val JNum(num1) = root("num1")
    val JNum(num2) = root("num2")
    val JStr(string1) = root("string1")
    val JMap(map1) = root("map1")
    val JBool(map1b1) = map1("bool1")
    val JNum(map1n1) = map1("num1")
    val JNum(map1n2) = map1("num2")
    val JStr(map1s1) = map1("string1")
    val JList(map1l1) = map1("list1")
    val JList(list1) = root("list1")
    val JList(list2) = root("list2")
    val converted = for (JMap(m1) <- list2) yield {
      val JStr(m1a) = m1("a")
      val JStr(m1b) = m1("b")
      (bool1, num1, num2, string1, map1b1, map1n1, map1n2, map1n2, map1s1, map1l1, list1, m1a, m1b)
    }
    converted should not be ('empty)
  }
}
