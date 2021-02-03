package com.example.simple.impl

import com.example.simple.api._
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.AsyncWordSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

class SimpleServiceSpec
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll {

  lazy val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
    //.withJdbc()
  ) { ctx =>
    new SimpleApplication(ctx) with LocalServiceLocator
  }

  lazy val client: SimpleService = server.serviceClient.implement[SimpleService]

  "simple service" should {

    "return version number" in {
      client.versione().invoke().map { answer =>
        answer should ===("0.0.1")
      }
    }
  }
  protected override def beforeAll() = server
  override protected def afterAll(): Unit = server.stop()

}
