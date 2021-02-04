package com.example.simple.api

import akka.Done
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.KafkaProperties
import com.lightbend.lagom.scaladsl.api.broker.kafka.PartitionKeyStrategy
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

  def contiTopic: Topic[ContoEventDto]

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

      .withTopics(
        topic(SimpleService.TOPIC_NAME, contiTopic)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[ContoEventDto](_.iban)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }

}

// eventuale Message serialization per dati scambiati
// (in questo caso sempre tipi dato semplici gia serializzati di default

//case class User(id: Long,name: String,email: Option[String])
//object User {
//  implicit val format: Format[User] = Json.format[User]
//}

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
