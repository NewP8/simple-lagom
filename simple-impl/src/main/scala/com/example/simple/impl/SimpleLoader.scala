package com.example.simple.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.example.simple.api.SimpleService
import com.example.simple.impl.Conto.ContoCreato
import com.example.simple.impl.Conto.PrelevatoDaConto
import com.example.simple.impl.Conto.TransazioneEseguita
import com.example.simple.impl.Conto.TransazioneRespinta
import com.example.simple.impl.Conto.VersatoInConto
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable.Seq

class SimpleLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new SimpleApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new SimpleApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[SimpleService])
}

abstract class SimpleApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with SlickPersistenceComponents
    with JdbcPersistenceComponents
//    with WriteSideCassandraPersistenceComponents
//    with ReadSideSlickPersistenceComponents
    with HikariCPComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer =
    serverFor[SimpleService](wire[SimpleServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    ContoSerializerRegistry

  object ContoSerializerRegistry extends JsonSerializerRegistry {
    override def serializers: Seq[JsonSerializer[_]] =
      Seq(
        // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
        JsonSerializer[ContoCreato],
        JsonSerializer[VersatoInConto],
        JsonSerializer[PrelevatoDaConto],
        JsonSerializer[Conto],
        JsonSerializer[TransazioneEseguita],
        JsonSerializer[TransazioneRespinta]
      )
  }

  lazy val contoRepository: ContoRepository =
    wire[ContoRepository]
  readSide.register(wire[ContoProcessor])

  clusterSharding.init(
    Entity(Conto.typeKey)(entityContext => Conto(entityContext))
  )

}
