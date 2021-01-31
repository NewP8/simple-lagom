package com.example.simple.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.example.simple.api.SimpleService
import com.example.simple.impl.ItemAggregate.{Accettato, Conferma, ItemAdded, Respinto, formatItemState}
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.server._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable.Seq
// import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.softwaremill.macwire._

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
      //JdbcPersistenceComponents
//    with WriteSideCassandraPersistenceComponents
//    with ReadSideSlickPersistenceComponents
    with HikariCPComponents
    // with LagomKafkaComponents
    with AhcWSComponents {

  // implicit def executionContext: ExecutionContext

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer =
    serverFor[SimpleService](wire[SimpleServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    ItemSerializerRegistry

  /**
    * Akka serialization, used by both persistence and remoting, needs to have
    * serializers registered for every type serialized or deserialized. While it's
    * possible to use any serializer you want for Akka messages, out of the box
    * Lagom provides support for JSON, via this registry abstraction.
    *
    * The serializers are registered here, and then provided to Lagom in the
    * application loader.
    */
  object ItemSerializerRegistry extends JsonSerializerRegistry {
    override def serializers: Seq[JsonSerializer[_]] =
      Seq(
        // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
        JsonSerializer[ItemAdded],
        JsonSerializer[ItemState],
        // the replies use play-json as well
        // JsonSerializer[Conferma],
        JsonSerializer[Accettato],
        JsonSerializer[Respinto],
//        JsonSerializer[Greeting],
//        JsonSerializer[GreetingMessageChanged],
//        JsonSerializer[SimpleState],
//        JsonSerializer[Greeting],
//        JsonSerializer[Confirmation],
//        JsonSerializer[Accepted],
//        JsonSerializer[Rejected]
      )
  }

//  lazy val itemRepository: ItemRepository =
//    wire[ItemRepository]
//  readSide.register(wire[ItemProcessor])


  clusterSharding.init(
    Entity(ItemAggregate.typeKey)(entityContext =>
      ItemAggregate(entityContext)
    )
  )

}
