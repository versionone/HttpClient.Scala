HttpClient.Scala
================

A Java-compatible HTTP client with VersionOne-specific methods, implemented in Scala.


    import com.versionone.httpclient._
    import org.apache.oltu.oauth2._
    
    val creds = new BasicOAuthToken(...)
    val settings = new OAuth2Settings(creds, ...)
    val log = LoggerFactory getLogger "main"
    val logAdapter = new SimpleLogger {
      def error(msg:String) = log error msg
      def debug(msg:String) = log debug msg
      def info(msg:String) = log info msg
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
