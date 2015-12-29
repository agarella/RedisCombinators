package com.rediscombinators

import java.util.UUID

import com.redis.RedisClientPool
import com.rediscombinators.RedisLockOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Try}

class RedisCombinatorsLockTest extends FeatureSpec with GivenWhenThen with MockitoSugar {

  val rcs = new RedisClientPool(Environment.host, Environment.port, secret = Environment.password)
  @volatile var counter = 0

  feature("RedisLock") {
    Given("a Redis client")
    val uuid = UUID.randomUUID().toString

    When("acquiring locks")
    And("a calculation throws an exception while having the lock")
    Then("only one calculation should have the lock")
    And("no deadlock occurs")
    acquireLockAndPrintSomething(uuid)
    acquireLockAndThrowException(uuid)
    acquireLockAndPrintSomething(uuid)

    Try(Await.result(Future { while (counter < 3)(/** Block until done */) }, 10 seconds)) match {
      case Failure(e) =>
        rcs.withClient(_.flushall)
        fail("Test failed!", e)
      case _          => ()
    }
    rcs.withClient(_.flushall)
  }

  def acquireLockAndPrintSomething(lock: Lock): Future[Unit] =
  Future {
      rcs.withClient { client =>
        client.withLock(lock) { _ =>
          println("Acquired lock!")
          Thread.sleep(1000)
          counter += 1
          println(s"Increment counter to: $counter")

        }
      }
    }

  def acquireLockAndThrowException(lock: Lock): Future[Unit] =
  Future {
      rcs.withClient { client =>
        client.withLock(lock) {
          println("Acquired lock")
          Thread.sleep(1000)
          println("GOING TO DIE!")
          counter += 1
          println(s"Increment counter to: $counter")
          throw new Exception("DIE!")
        }
      }
    }

}
