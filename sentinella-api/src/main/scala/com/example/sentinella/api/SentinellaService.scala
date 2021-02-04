package com.example.sentinella.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.Method.GET

trait SentinellaService extends Service {

  def versione(): ServiceCall[NotUsed, String]
  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("sentinella")
      .withCalls(
        restCall(GET,"/sentinella/versione", versione _))
      .withAutoAcl(true)
  }
}
