package com.example.simple.api

import akka.Done
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.Method.GET
import com.lightbend.lagom.scaladsl.api.transport.Method.POST
import play.api.libs.json.Format
import play.api.libs.json.Json

object SimpleService {
  val TOPIC_NAME = "conti"
}

trait SimpleService extends Service {

  def versione(): ServiceCall[NotUsed, String]
  def creaConto(iban: String): ServiceCall[Int, Done]
  def versaInConto(iban: String): ServiceCall[Int, Done]
  def prelevaDaConto(iban: String): ServiceCall[Int, Done]
  def bilancioConto(iban: String): ServiceCall[NotUsed, Int]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("simple")
      .withCalls(
        restCall(GET,"/api/versione", versione _),
        restCall(POST,"/api/conto/:iban", creaConto _),
        restCall(POST,"/api/versa/:iban", versaInConto _),
        restCall(POST,"/api/preleva/:iban", prelevaDaConto _),
        restCall(GET,"/api/conto/:iban", bilancioConto _)
        // implicitamente ogni chiamata riceve MessageSerializer per il tipo indicato
        //  call(sayHello)(MessageSerializer.StringMessageSerializer, MessageSerializer.StringMessageSerializer)
      )
      .withAutoAcl(true)
    // @formatter:on
  }

}

final case class ContoView(iban: String, importo: Int)

object ContoView {
  implicit val format: Format[ContoView] = Json.format
  def tupled(t: (String, Int)) =
    ContoView(t._1, t._2)
}

case class ContoEventDto(tipoEvento: String, iban: String, importo: Int)
object ContoEventDto {
  implicit val format: Format[ContoEventDto] = Json.format[ContoEventDto]
}
