package com.example.simple.impl

import play.api.libs.json.Json
import play.api.libs.json.Format
import java.time.LocalDateTime

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.libs.json._

import scala.collection.immutable.Seq

/**
  * This provides an event sourced behavior. It has a state, [[SimpleState]], which
  * stores what the greeting should be (eg, "Hello").
  *
  * Event sourced entities are interacted with by sending them commands. This
  * aggregate supports two commands, a [[UseGreetingMessage]] command, which is
  * used to change the greeting, and a [[Hello]] command, which is a read
  * only command which returns a greeting to the name specified by the command.
  *
  * Commands get translated to events, and it's the events that get persisted.
  * Each event will have an event handler registered for it, and an
  * event handler simply applies an event to the current state. This will be done
  * when the event is first created, and it will also be done when the aggregate is
  * loaded from the database - each event will be replayed to recreate the state
  * of the aggregate.
  *
  * This aggregate defines one event, the [[GreetingMessageChanged]] event,
  * which is emitted when a [[UseGreetingMessage]] command is received.
  */
object ItemBehavior {

  /**
    * Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
    */ 
  def create(entityContext: EntityContext[ItemCommand]): Behavior[ItemCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, ItemEvent.Tag)
      )

  }
  /*
   * This method is extracted to write unit tests that are completely independendant to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId) = EventSourcedBehavior
      .withEnforcedReplies[ItemCommand, ItemEvent, ItemState](
        persistenceId = persistenceId,
        emptyState = ItemState.initial,
        commandHandler = (cart, cmd) => cart.applyCommand(cmd),
        eventHandler = (cart, evt) => cart.applyEvent(evt)
      )
}

/**
  * The current state of the Aggregate.
  */
case class ItemState(itemId:Int, message: String) {
  def applyCommand(cmd: ItemCommand): ReplyEffect[ItemEvent, ItemState] =
    cmd match {
      case x: AddItem              => onAddItem(x)
      case x: GetItem => onGetItem(x)
    }

  def applyEvent(evt: ItemEvent): ItemState =
    evt match {
      case ItemAdded(itemId, msg) => updateItem(itemId, msg)
    }
  private def onGetItem(cmd: GetItem): ReplyEffect[ItemEvent, ItemState] = {
    // lettura item
    Effect.reply(cmd.replyTo)(s"item: $itemId - messaggio: $message") // s"$message, ${cmd.name}!"))
  }

  private def onAddItem(
    cmd: AddItem
  ): ReplyEffect[ItemEvent, ItemState] =
    Effect
      .persist(ItemAdded(cmd.itemId, cmd.message))
      .thenReply(cmd.replyTo) { _ =>
        Accettato("ciao")
      }

  private def updateItem(newItemId: Int, newMessage: String) =
    copy(newItemId, newMessage)
}

object ItemState {

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: ItemState = ItemState(0, "Hello")

  /**
    * The [[EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey = EntityTypeKey[ItemCommand]("ItemAggregate")

  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[ItemState] = Json.format
}

/**
  * This interface defines all the events that the ItemAggregate supports.
  */
sealed trait ItemEvent extends AggregateEvent[ItemEvent] {
  def aggregateTag: AggregateEventTag[ItemEvent] = ItemEvent.Tag
}

object ItemEvent {
  val Tag: AggregateEventTag[ItemEvent] = AggregateEventTag[ItemEvent]
}

/**
  * An event that represents a change in greeting message.
  */
case class ItemAdded(itemId: Int, message: String) extends ItemEvent

object ItemAdded {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[ItemAdded] = Json.format
}

/**
  * This is a marker trait for commands.
  * We will serialize them using Akka's Jackson support that is able to deal with the replyTo field.
  * (see application.conf)
  */
trait ItemCommandSerializable

/**
  * This interface defines all the commands that the ItemAggregate supports.
  */
sealed trait ItemCommand
    extends ItemCommandSerializable

/**
  * A command to switch the greeting message.
  *
  * It has a reply type of [[Confirmation]], which is sent back to the caller
  * when all the events emitted by this command are successfully persisted.
  */
case class AddItem(itemId:Int, message: String, replyTo: ActorRef[Conferma])
    extends ItemCommand

/**
  * A command to say hello to someone using the current greeting message.
  *
  * The reply type is String, and will contain the message to say to that
  * person.
  */
case class GetItem(itemId: Int, replyTo: ActorRef[String])
    extends ItemCommand

//final case class Greeting(message: String)
//
//object Greeting {
//  implicit val format: Format[Greeting] = Json.format
//}

sealed trait Conferma

case object Conferma {
  implicit val format: Format[Conferma] = Json.format
}

// case object Accettato extends Conferma

case class Accettato(reason: String)  extends Conferma

object Accettato {
  implicit val format: Format[Accettato] = Json.format
}

case class Respinto(reason: String)  extends Conferma

object Respinto {
  implicit val format: Format[Respinto] = Json.format
}
//sealed trait Accepted extends Confirmation
//
//case object Accepted extends Accepted {
//  implicit val format: Format[Accepted] =
//    Format(Reads(_ => JsSuccess(Accepted)), Writes(_ => Json.obj()))
//}
//
//case class Rejected(reason: String) extends Confirmation
//
//object Rejected {
//  implicit val format: Format[Rejected] = Json.format
//}

