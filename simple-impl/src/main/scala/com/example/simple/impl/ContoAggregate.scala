package com.example.simple.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.persistence.typed.scaladsl.RetentionCriteria
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import play.api.libs.json.Format
import play.api.libs.json.Json

object Conto {

  // COMMANDS

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  case class CreaConto(
      iban: String,
      importo: Int,
      replyTo: ActorRef[Conferma]
  ) extends Command

  case class VersaInConto(
      importo: Int,
      replyTo: ActorRef[Conferma]
  ) extends Command
  case class PrelevaDaConto(
      importo: Int,
      replyTo: ActorRef[Conferma]
  ) extends Command

  case class BilancioConto(replyTo: ActorRef[Int]) extends Command

  sealed trait Conferma
  final case class TransazioneEseguita(bilancio: Int) extends Conferma
  final case class TransazioneRespinta(reason: String) extends Conferma

//  implicit val format: Format[Conferma] = Json.format
  implicit val formatTransazioneEseguita: Format[TransazioneEseguita] =
    Json.format
  implicit val formatTransazioneRespinta: Format[TransazioneRespinta] =
    Json.format

  // EVENTS

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] =
      Event.Tag
  }
  object Event {
    val Tag = AggregateEventTag[Event]
  }

  final case class ContoCreato(importoIniziale: Int) extends Event

  implicit val formatContoCreato: Format[ContoCreato] = Json.format

  // BEHAVIOUR
  def initial: Conto = Conto(false, 0)
  val typeKey = EntityTypeKey[Command]("Conto")

  def apply(persistenceId: PersistenceId) =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, Conto](
        persistenceId = persistenceId,
        emptyState = initial,
        commandHandler = (cart, cmd) => cart.applyCommand(cmd),
        eventHandler = (cart, evt) => cart.applyEvent(evt)
      )

  def apply(
      entityContext: EntityContext[Command]
  ): Behavior[Command] = {

    val persistenceId: PersistenceId =
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    apply(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)
      )
      .withRetention(
        RetentionCriteria
          .snapshotEvery(numberOfEvents = 10, keepNSnapshots = 2)
      )
  }

  implicit val formatItemState: Format[Conto] = Json.format
}

// STATE
final case class Conto(creato: Boolean, importo: Int) {
  import Conto._

  // COMMAND HANDLER

  def applyCommand(cmd: Command): ReplyEffect[Event, Conto] = {
    cmd match {
      case CreaConto(iban, importoIniziale, replyTo) =>
        onCreaConto(importoIniziale, replyTo)
      case VersaInConto(importoVersato, replyTo) =>
        Effect.reply(replyTo)(TransazioneRespinta("nyi"))
      case PrelevaDaConto(importoPrelevato, replyTo) =>
        Effect.reply(replyTo)(TransazioneRespinta("nyi"))
      case BilancioConto(replyTo) => Effect.reply(replyTo)(importo)
    }
  }

  private def onCreaConto(
      importoIniziale: Int,
      replyTo: ActorRef[Conferma]
  ): ReplyEffect[Event, Conto] = {
    if (creato == true)
      Effect.reply(replyTo)(TransazioneRespinta("Conto gia esistente"))
    else
      Effect
        .persist(ContoCreato(importoIniziale))
        .thenReply(replyTo)(updatedCart =>
          TransazioneEseguita(updatedCart.importo)
        )
  }

  // EVENT HANDLER

  def applyEvent(evt: Event): Conto =
    evt match {
      case ContoCreato(importoIniziale) => copy(creato = true, importoIniziale)
    }

}
