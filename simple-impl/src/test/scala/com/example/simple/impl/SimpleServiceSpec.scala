package com.example.simple.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.example.simple.api._

class SimpleServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new SimpleApplication(ctx) with LocalServiceLocator
  }

  val client: SimpleService = server.serviceClient.implement[SimpleService]

  override protected def afterAll(): Unit = server.stop()

//  "simple service" should {
//
//    "say hello" in {
//      client.hello("Alice").invoke().map { answer =>
//        answer should ===("Hello, Alice!")
//      }
//    }
//
//    "allow responding with a custom message" in {
//      for {
//        _ <- client.useGreeting("Bob").invoke(GreetingMessage("Hi"))
//        answer <- client.hello("Bob").invoke()
//      } yield {
//        answer should ===("Hi, Bob!")
//      }
//    }
//  }
}
