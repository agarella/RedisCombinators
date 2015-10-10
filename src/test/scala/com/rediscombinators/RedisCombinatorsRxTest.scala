package com.rediscombinators

import com.redis.RedisClientPool
import com.rediscombinators.RedisRxOps._
import com.rediscombinators.RedisSyncOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}
import rx.lang.scala.Observer

import scala.util.Try
import scalaz.Scalaz._

class RedisCombinatorsRxTest extends FeatureSpec with GivenWhenThen with MockitoSugar {

  val rcs = new RedisClientPool(Environment.host, Environment.port)
  val kvs = (1 to 100).map(x => s"$x" -> s"$x").toList

  @volatile var ks = kvs.toMap.keys.toSet
  @volatile var done = false

  feature("mGetRx"){
    rcs.withClient(rc => rc.mSet(kvs))

    val o = createObserver[String]

    rcs.withClient { rc =>
      rc.getKeyStream.subscribe(o)
    }

    while (!done || ks.nonEmpty)(/** Block until done */)
    rcs.withClient(_.flushall)
  }

  def createObserver[String]: Observer[String] = new Observer[String] {
    override def onNext(value: String): Unit = ks -= value.toString
    override def onError(error: Throwable): Unit = throw error
    override def onCompleted(): Unit = done = true
  }

  private def parseInt(s: String): Int = Try(Integer.parseInt(s)).toOption.orZero

}
