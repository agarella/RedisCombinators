package com.rediscombinators

object Environment {

  lazy val host = scala.util.Properties.envOrElse("REDIS_SERVER_HOSTNAME", "localhost" )

  lazy val port = Integer.parseInt(scala.util.Properties.envOrElse("REDIS_SERVER_PORT", "6379"))

}
