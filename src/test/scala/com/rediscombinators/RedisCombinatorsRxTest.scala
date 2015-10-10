package com.rediscombinators

import com.redis.RedisClientPool
import com.rediscombinators.RedisRxOps._
import com.rediscombinators.RedisSyncOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}
import rx.lang.scala.Observer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Try}
import scalaz.Scalaz._

class RedisCombinatorsRxTest extends FeatureSpec with GivenWhenThen with MockitoSugar {

  val rcs = new RedisClientPool(Environment.host, Environment.port)
  val kvs = (1 to 100).map(x => s"$x" -> s"$x").toList

  @volatile var ks = kvs.toMap.keys.toSet
  @volatile var done = false

  feature("getKeyStream"){
    Given("a redis client")
    rcs.withClient(rc => rc.mSet(kvs))

    And("an observer")
    val o = createObserver[String]

    When("subscribing to the stream")
    rcs.withClient { rc =>
      rc.getKeyStream.subscribe(o)
    }

    Then("All values should be processed")
    Try { Await.result(Future(while (!done || ks.nonEmpty)(/** Block until done */)), 10 seconds) } match {
      case Failure(e) =>
        rcs.withClient(_.flushall)
        fail("Test failed!", e)
      case _          => ()
    }
    rcs.withClient(_.flushall)
  }

  def createObserver[String]: Observer[String] = new Observer[String] {
    override def onNext(value: String): Unit = ks -= value.toString
    override def onError(error: Throwable): Unit = throw error
    override def onCompleted(): Unit = done = true
  }

  private def parseInt(s: String): Int = Try(Integer.parseInt(s)).toOption.orZero

}
