package com.example.simple.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import play.api.libs.json.Format
import play.api.libs.json.Json

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
object ItemAggregate {

  // COMMANDS

  trait CommandSerializable
  sealed trait ItemCommand extends CommandSerializable
  case class AddItem(itemId: Int, message: String, replyTo: ActorRef[Conferma])
    extends ItemCommand
  case class GetItem(itemId: Int, replyTo: ActorRef[String]) extends ItemCommand

  sealed trait Conferma
  final case class Accettato(reason: String) extends Conferma
  final case class Respinto(reason: String) extends Conferma

//  implicit val format: Format[Conferma] = Json.format
  implicit val formatAccettato: Format[Accettato] = Json.format
  implicit val formatRespinto: Format[Respinto] = Json.format

  // EVENTS

  sealed trait ItemEvent extends AggregateEvent[ItemEvent] {
    override def aggregateTag: AggregateEventShards[ItemEvent] =
      ItemEvent.Tag
  }
  object ItemEvent {
    val Tag = AggregateEventTag.sharded[ItemEvent](4) // val NumShards = 4
  }

  final case class ItemAdded(itemId: Int, message: String) extends ItemEvent

  implicit val formatItemAdded: Format[ItemAdded] = Json.format

  // BEHAVIOUR
  def initial: ItemState = ItemState(0, "Hello")
  val typeKey = EntityTypeKey[ItemCommand]("ItemAggregate")

  def apply(persistenceId: PersistenceId) =
    EventSourcedBehavior
      .withEnforcedReplies[ItemCommand, ItemEvent, ItemState](
        persistenceId = persistenceId,
        emptyState = initial,
        commandHandler = (cart, cmd) => cart.applyCommand(cmd),
        eventHandler = (cart, evt) => cart.applyEvent(evt)
      )

  def apply(
              entityContext: EntityContext[ItemCommand]
            ): Behavior[ItemCommand] = {

    val persistenceId: PersistenceId =
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    apply(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, ItemEvent.Tag)
      )
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
  }

  implicit val formatItemState: Format[ItemState] = Json.format
}

// STATE
final case class ItemState(itemId: Int, message: String) {
  import ItemAggregate._

  def applyCommand(cmd: ItemCommand): ReplyEffect[ItemEvent, ItemState] =
    cmd match {
      case x: AddItem => onAddItem(x)
      case x: GetItem => onGetItem(x)
    }

  def applyEvent(evt: ItemEvent): ItemState =
    evt match {
      case ItemAdded(itemId, msg) => updateItem(itemId, msg)
    }
  private def onGetItem(cmd: GetItem): ReplyEffect[ItemEvent, ItemState] = {
    // lettura item
    Effect.reply(cmd.replyTo)(
      s"item: $itemId - messaggio: $message"
    ) // s"$message, ${cmd.name}!"))
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




