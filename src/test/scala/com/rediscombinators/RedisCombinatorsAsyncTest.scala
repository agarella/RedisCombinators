package com.rediscombinators

import com.redis.RedisClientPool
import com.rediscombinators.RedisAsyncOps._
import com.rediscombinators.RedisSyncOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class RedisCombinatorsAsyncTest extends FeatureSpec with GivenWhenThen with MockitoSugar {

  val rcs = new RedisClientPool(Environment.host, Environment.port)
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
        case Failure(e) => fail("Test failed!", e)
      }
    }

    while (!done)(/** Block until done */)
    rcs.withClient(_.flushall)
  }

}
