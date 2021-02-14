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

import java.net.InetAddress

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

  final case class ContoCreato(iban: String, bilancio: Int) extends Event
  final case class VersatoInConto(importo: Int, bilancio: Int) extends Event
  final case class PrelevatoDaConto(importo: Int, bilancio: Int) extends Event

  implicit val formatContoCreato: Format[ContoCreato] = Json.format
  implicit val formatVersatoInConto: Format[VersatoInConto] = Json.format
  implicit val formatPrelevatoDaConto: Format[PrelevatoDaConto] = Json.format

  // BEHAVIOUR
  def initial: Conto = Conto("", false, 0)
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
          .snapshotEvery(numberOfEvents = 4, keepNSnapshots = 2)
      )
  }

  implicit val formatItemState: Format[Conto] = Json.format
}

// STATE
final case class Conto(iban: String, giaCreato: Boolean, importo: Int) {
  import Conto._

  // COMMAND HANDLER

  def applyCommand(cmd: Command): ReplyEffect[Event, Conto] = {
    cmd match {
      case CreaConto(iban, importoIniziale, replyTo) =>
        onCreaConto(iban, importoIniziale, replyTo)
      case VersaInConto(importoVersato, replyTo) =>
        onVersaInConto(importoVersato, replyTo)
      case PrelevaDaConto(importoPrelevato, replyTo) =>
        onPrelevaDaConto(importoPrelevato, replyTo)
      case BilancioConto(replyTo) => Effect.reply(replyTo)(importo)
    }
  }

  private def onCreaConto(
      iban: String,
      importoIniziale: Int,
      replyTo: ActorRef[Conferma]
  ): ReplyEffect[Event, Conto] = {
    if (giaCreato)
      Effect.reply(replyTo)(TransazioneRespinta("Conto gia esistente"))
    else
      Effect
        .persist(ContoCreato(iban, importoIniziale))
        .thenReply(replyTo)(updatedCart =>
          TransazioneEseguita(updatedCart.importo)
        )
  }

  private def onVersaInConto(
      importoDaVersare: Int,
      replyTo: ActorRef[Conferma]
  ): ReplyEffect[Event, Conto] = {
    if (giaCreato)
      Effect
        .persist(
          VersatoInConto(importoDaVersare, importo + importoDaVersare)
        )
        .thenReply(replyTo)(updatedCart =>
          TransazioneEseguita(updatedCart.importo)
        )
    else
      Effect.reply(replyTo)(TransazioneRespinta("Il conto indicato non esiste"))

  }

  private def onPrelevaDaConto(
      importoDaPrelevare: Int,
      replyTo: ActorRef[Conferma]
  ): ReplyEffect[Event, Conto] = {
    if (!giaCreato)
      Effect.reply(replyTo)(TransazioneRespinta("Il conto indicato non esiste"))
    if (importoDaPrelevare > importo)
      Effect.reply(replyTo)(TransazioneRespinta("Credito insufficiente"))
    else
      Effect
        .persist(
          PrelevatoDaConto(
            importoDaPrelevare,
            importo - importoDaPrelevare
          )
        )
        .thenReply(replyTo)(updatedCart =>
          TransazioneEseguita(updatedCart.importo)
        )
  }

  // EVENT HANDLER

  def applyEvent(evt: Event): Conto = {
    val hostname = InetAddress.getLocalHost.getHostName
    val localIpAddress = InetAddress.getLocalHost.getHostAddress

    evt match {
      case ContoCreato(iban, bilancio) => {
        println(s". | ${iban} | ${localIpAddress} | _c")
        copy(iban, giaCreato = true, bilancio)
      }
      case VersatoInConto(_, bilancio) => {
        println(s". | ${iban} | ${localIpAddress} | _v")
        copy(importo = bilancio)
      }
      case PrelevatoDaConto(_, bilancio) => {
        println(s". | ${iban} | ${localIpAddress} | _p")
        copy(importo = bilancio)
      }
    }
  }
}
