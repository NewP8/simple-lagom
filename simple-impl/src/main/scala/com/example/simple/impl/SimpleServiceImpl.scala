package com.example.simple.impl

import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.example.simple.api
import com.example.simple.api.SimpleService
import com.lightbend.lagom.scaladsl.api.ServiceCall
// import com.lightbend.lagom.scaladsl.broker.TopicProducer
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Implementation of the SimpleService.
  */
class SimpleServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    itemRepository: ItemRepository
)(implicit ec: ExecutionContext)
    extends SimpleService {

  /**
    * Looks up the entity for the given ID.
    */
  private def entityRef(id: String): EntityRef[SimpleCommand] =
    clusterSharding.entityRefFor(SimpleState.typeKey, id)

  private def entityItemRef(id: String): EntityRef[ItemCommand] =
    clusterSharding.entityRefFor(ItemState.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  override def hello(id: String): ServiceCall[NotUsed, String] =
    ServiceCall { _ =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityRef(id)

      // Ask the aggregate instance the Hello command.
      ref
        .ask[Greeting](replyTo => Hello(id, replyTo))
        .map(greeting => greeting.message)
    }

  override def useGreeting(id: String) =
    ServiceCall { request =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityRef(id)

      // Tell the aggregate to use the greeting message specified.
      ref
        .ask[Confirmation](replyTo =>
          UseGreetingMessage(request.message, replyTo)
        )
        .map {
          case Accepted => Done
          case _        => throw BadRequest("Can't upgrade the greeting message.")
        }
    }

  override def addItem(id: Int) =
    ServiceCall { request =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityItemRef(id.toString)

      // Tell the aggregate to use the greeting message specified.
      ref
        .ask[Conferma](replyTo => AddItem(id, request, replyTo))
        .map {
          case Accettato(_) => Done
          case _            => throw BadRequest("Can't upgrade the greeting message.")
        }
    }

  override def getItem(id: Int) =
    ServiceCall { _ =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityItemRef(id.toString)

      // Tell the aggregate to use the greeting message specified.
      ref
        .ask[String](replyTo => GetItem(id, replyTo))
//      .map {
//        case s => s
//      }
    }

  //  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
//    TopicProducer.singleStreamWithOffset { fromOffset =>
//      persistentEntityRegistry
//        .eventStream(SimpleEvent.Tag, fromOffset)
//        .map(ev => (convertEvent(ev), ev.offset))
//    }

  private def convertEvent(
      helloEvent: EventStreamElement[SimpleEvent]
  ): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) =>
        api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }
}
