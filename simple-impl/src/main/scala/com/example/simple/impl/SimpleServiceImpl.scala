package com.example.simple.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.example.simple.api.SimpleService
import com.example.simple.impl.Conto.BilancioConto
import com.example.simple.impl.Conto.Conferma
import com.example.simple.impl.Conto.CreaConto
import com.example.simple.impl.Conto.PrelevaDaConto
import com.example.simple.impl.Conto.TransazioneEseguita
import com.example.simple.impl.Conto.TransazioneRespinta
import com.example.simple.impl.Conto.VersaInConto
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import java.net.InetAddress
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
      val localIpAddress = InetAddress.getLocalHost.getHostAddress
      println(s"- | ${iban} | ${localIpAddress} | C_")

      val ref = contoRef(iban)
      ref
        .ask[Conferma](replyTo => CreaConto(iban, importoIniziale, replyTo))
        .map {
          case TransazioneEseguita(_) => Done
          case TransazioneRespinta(reason) =>
            throw BadRequest(s"Errore: ${reason}")
        }
    }

  override def versaInConto(iban: String) =
    ServiceCall { importo =>
      val localIpAddress = InetAddress.getLocalHost.getHostAddress
      println(s"- | ${iban} | ${localIpAddress} | V_")

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
      val localIpAddress = InetAddress.getLocalHost.getHostAddress
      println(s"- | ${iban} | ${localIpAddress} | P_")

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
      val localIpAddress = InetAddress.getLocalHost.getHostAddress
      println(s"- | ${iban} | ${localIpAddress} | B_")

      val ref = contoRef(iban)
      ref
        .ask[Int](replyTo => BilancioConto(replyTo))
    }
}
