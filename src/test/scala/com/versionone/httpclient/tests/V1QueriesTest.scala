package com.versionone.httpclient.tests

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.versionone.httpclient._

@RunWith(classOf[JUnitRunner])
class V1QueriesTest extends FunSuite with ShouldMatchers {
    
  object mockV1 extends V1QueryMethods {
    override def DoRequest(path:String, body:String) = (path, body) match {
      case ("query.v1", "test1") => (200, """
        [
          [
            {
              "Attribute1": "Data"
            }
          ]
        ]
        """, (hdr:String)=>null)
        
      case (p, _) =>
        sys error "Request for unmocked path $p"
    }
  }
  
  test("Interprets structure of V1 query.v1 endpoint") {
    val results = mockV1.Query("test1")
    results(0)("Attribute1") should be ("Data")
  }
}
