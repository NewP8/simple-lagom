package com.example.simple.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.example.simple.api.ContoEventDto
import com.example.simple.api.SimpleService
import com.example.simple.impl.Conto.BilancioConto
import com.example.simple.impl.Conto.Conferma
import com.example.simple.impl.Conto.ContoCreato
import com.example.simple.impl.Conto.CreaConto
import com.example.simple.impl.Conto.PrelevaDaConto
import com.example.simple.impl.Conto.PrelevatoDaConto
import com.example.simple.impl.Conto.TransazioneEseguita
import com.example.simple.impl.Conto.TransazioneRespinta
import com.example.simple.impl.Conto.VersaInConto
import com.example.simple.impl.Conto.VersatoInConto
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class SimpleServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    contoRepository: ContoRepository
    // ReadSide: ReadSide, myDatabase: MyDatabase
)(implicit ec: ExecutionContext)
    extends SimpleService {

  override def versione =
    ServiceCall { _ =>
      Future.successful(s"0.0.1")
    }

  private def contoRef(iban: String): EntityRef[Conto.Command] =
    clusterSharding.entityRefFor(Conto.typeKey, iban)

  implicit val timeout = Timeout(10.seconds)

  override def creaConto(iban: String) =
    ServiceCall { importoIniziale =>
      val ref = contoRef(iban)
      ref
        .ask[Conferma](replyTo => CreaConto(importoIniziale, replyTo))
        .map {
          case TransazioneEseguita(_) => Done
          case TransazioneRespinta(reason) =>
            throw BadRequest(s"Errore: ${reason}")
        }
    }

  override def versaInConto(iban: String) =
    ServiceCall { importo =>
      val ref = contoRef(iban)
      ref
        .ask[Conferma](replyTo => VersaInConto(importo, replyTo))
        .map {
          case TransazioneEseguita(_) => Done
          case TransazioneRespinta(reason) =>
            throw BadRequest(s"Errore: ${reason}")
        }
    }

  override def prelevaDaConto(iban: String) =
    ServiceCall { importo =>
      val ref = contoRef(iban)
      ref
        .ask[Conferma](replyTo => PrelevaDaConto(importo, replyTo))
        .map {
          case TransazioneEseguita(_) => Done
          case TransazioneRespinta(reason) =>
            throw BadRequest(s"Errore: ${reason}")
        }
    }

  override def bilancioConto(iban: String) =
    ServiceCall { _ =>
      val ref = contoRef(iban)
      ref
        .ask[Int](replyTo => BilancioConto(replyTo))
    }

  override def contiTopic: Topic[ContoEventDto] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(Conto.Event.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    // eventualmente filtra con Nil e mapConcat
    }

  private def convertEvent(
      eventoConto: EventStreamElement[Conto.Event]
  ): ContoEventDto = {
    // recupera iban
    eventoConto.event match {
      case x: ContoCreato =>
        ContoEventDto("creazione conto", eventoConto.entityId, x.bilancio)
      case x: VersatoInConto =>
        ContoEventDto("versamento in conto", eventoConto.entityId, x.importo)
      case x: PrelevatoDaConto =>
        ContoEventDto("prelievo da conto", eventoConto.entityId, x.importo)
    }
  }
}
