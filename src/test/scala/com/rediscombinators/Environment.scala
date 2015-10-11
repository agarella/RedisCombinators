package com.rediscombinators

object Environment {

  lazy val host = scala.util.Properties.envOrElse("REDIS_SERVER_HOST", "localhost" )

  lazy val port = Integer.parseInt(scala.util.Properties.envOrElse("REDIS_SERVER_PORT", "6379"))

  lazy val password = scala.util.Properties.envOrNone("REDIS_SERVER_PASSWORD")

}
