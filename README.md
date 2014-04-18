HttpClient.Scala
================

A Java-compatible HTTP client with VersionOne-specific methods, implemented in Scala.




Example (see also https://github.com/versionone/HttpClient.Scala/blob/master/src/test/scala/com/versionone/httpclient/sampleApp/SampleApp.scala):

    import com.versionone.httpclient._
    import org.apache.oltu.oauth2._
    
    val creds = new BasicOAuthToken(...)
    val settings = new OAuth2Settings(creds, ...)
    val logAdapter = new SimpleLogger {
      def error(msg:String) = println(s"ERROR: $msg")
      def debug(msg:String) = println(s"DEBUG: $msg")
      def info(msg:String) = println(s"INFO: $msg")
    }
    val v1 = new V1HttpClient(settings, logAdapter, "MyTest/1.0")
    val results =  v1 DoQuery """
      from: Member
      select:
        - Name
        - Avatar.ContentType
        - Avatar.Content
        - OwnedWorkitems:PrimaryWorkitem.Estimate.@Sum
        - from: OwnedWorkitems:PrimaryWorkitem
          select:
            - Name
            - Number
            - Estimate
            - Description
            - ToDo
        """
    for {
      JMap(member) <- results
      JStr(mname) = member("name")
      JStr(itemsum) = member("OwnedWorkitems:PrimaryWorkitem.Estimate.@Sum")
    } {
      println((mname, itemsum))
    }
