package com.example.sentinella.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.example.sentinella.api.SentinellaService
import com.example.simple.api.ContoEventDto
import com.example.simple.api.SimpleService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future

class SentinellaServiceImpl(
    simpleService: SimpleService
) extends SentinellaService {

  simpleService.contiTopic.subscribe
    .atLeastOnce(Flow[ContoEventDto].map { msg =>
      println(s"** log: ${msg.tipoEvento} - ${msg.iban} - ${msg.importo}")
      Done
    })

  override def versione =
    ServiceCall { _ =>
      Future.successful(s"0.0.1")
    }

}
