package com.example.simple.impl

import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.example.simple.api
import com.example.simple.api.SimpleService
import com.example.simple.impl.ItemAggregate.{Accettato, AddItem, Conferma, GetItem, ItemCommand}
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
    // itemRepository: ItemRepository
)(implicit ec: ExecutionContext)
    extends SimpleService {


  private def entityItemRef(id: String): EntityRef[ItemCommand] =
    clusterSharding.entityRefFor(ItemAggregate.typeKey, id)

  implicit val timeout = Timeout(10.seconds)


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

//  private def convertEvent(
//      helloEvent: EventStreamElement[SimpleEvent]
//  ): api.GreetingMessageChanged = {
//    helloEvent.event match {
//      case GreetingMessageChanged(msg) =>
//        api.GreetingMessageChanged(helloEvent.entityId, msg)
//    }
//  }
}
