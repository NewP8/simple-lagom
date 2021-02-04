package com.example.sentinella.impl

import com.example.sentinella.api.SentinellaService
import com.example.simple.api.SimpleService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
import com.lightbend.lagom.scaladsl.server.LagomApplicationLoader
import com.lightbend.lagom.scaladsl.server.LagomServer
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

class SentinellaLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new SentinellaApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new SentinellaApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[SentinellaService])
}

abstract class SentinellaApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with LagomKafkaClientComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer =
    serverFor[SentinellaService](wire[SentinellaServiceImpl])

  // necessario per ascoltare topic iniettare client di servizion
  lazy val simpleService = serviceClient.implement[SimpleService]

}
