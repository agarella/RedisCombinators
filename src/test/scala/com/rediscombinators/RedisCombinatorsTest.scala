package com.rediscombinators

import com.redis.RedisClientPool
import com.rediscombinators.RedisAsyncOps._
import com.rediscombinators.RedisSyncOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen}
import rx.lang.scala.Observer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

class RedisCombinatorsTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter with MockitoSugar {

  val rcs = new RedisClientPool("127.0.0.1", 6379)
  @volatile var done = false

  feature("getKeysAsync") {
    Given("a Redis client")
    When("storing a list of key-value pairs")
    val kvs = (1 to 100).map(x => s"$x" -> x).toList
    rcs.withClient(rc => rc.mSet(kvs))

    When("retrieving the keys asynchronously")
    rcs.withClient { rc =>
      rc.getKeysAsync.onComplete {
        case Success(x) =>

          Then("the result should contain the original keys")
          assertResult(kvs.map(_._1))(x.map(Integer.parseInt).sorted.map(_.toString))
          done = true
        case Failure(e) => assertResult(true)(false)
      }
    }

    while (!done)(/** Block until done */)
  }

  feature("mGetKeysRx"){
    import RedisRxOps._


    val kvs = (1 to 100).map(x => s"$x" -> x).toList
    rcs.withClient(rc => kvs.foreach { kv => rc.set(kv._1, kv._2) })

    val o = createObserver[String]

    rcs.withClient { rc =>
      rc.getKeysRx.subscribe(o)
    }

    while (!done)(/** Block until done */)
  }

  feature("mGetRx"){
    import RedisRxOps._

    val kvs = (1 to 100).map(x => s"$x" -> s"$x").toList
    rcs.withClient(rc => rc.mSet(kvs))

    val o = createObserver[String]

    rcs.withClient { rc =>
      rc.mGetStream.subscribe(o)
    }

    while (!done)(/** Block until done */)
  }

  def createObserver[A]: Observer[A] = new Observer[A] {
    override def onNext(value: A): Unit = println(value)
    override def onError(error: Throwable): Unit = error.printStackTrace()
    override def onCompleted(): Unit = done = true
  }

  def parseInt(s: String): Int = Try(Integer.parseInt(s)).toOption.orZero

  after {
    done = false
    rcs.withClient(_.flushall)
  }

}
