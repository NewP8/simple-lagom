package com.example.simple.api

import akka.Done
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method.GET
import com.lightbend.lagom.scaladsl.api.transport.Method.POST
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import play.api.libs.json.Format
import play.api.libs.json.Json

object SimpleService {
  val TOPIC_NAME = "greetings"
}

/**
  * The simple service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the SimpleService.
  */
trait SimpleService extends Service {

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  def hello(id: String): ServiceCall[NotUsed, String]

  /**
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
    * "Hi"}' http://localhost:9000/api/hello/Alice
    */
  def useGreeting(id: String): ServiceCall[GreetingMessage, Done]

  def addItem(itemId: Int): ServiceCall[String, Done]
  def getItem(itemId: Int): ServiceCall[NotUsed, String]

  /**
    * This gets published to Kafka.
    */
  // def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("simple")
      .withCalls(
        pathCall("/api/hello/:id", hello _),
        pathCall("/api/hello/:id", useGreeting _),
        restCall(POST,"/api/item/:id", addItem _),
        restCall(GET,"/api/item/:id", getItem _)
      )
//      .withTopics(
//        topic(SimpleService.TOPIC_NAME, greetingsTopic _)
//          // Kafka partitions messages, messages within the same partition will
//          // be delivered in order, to ensure that all messages for the same user
//          // go to the same partition (and hence are delivered in order with respect
//          // to that user), we configure a partition key strategy that extracts the
//          // name as the partition key.
//          .addProperty(
//            KafkaProperties.partitionKeyStrategy,
//            PartitionKeyStrategy[GreetingMessageChanged](_.name)
//          )
//      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

/**
  * The greeting message class.
  */
case class GreetingMessage(message: String)

object GreetingMessage {

  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessage] = Json.format[GreetingMessage]
}

/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class GreetingMessageChanged(name: String, message: String)

object GreetingMessageChanged {

  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessageChanged] =
    Json.format[GreetingMessageChanged]
}

case class ItemState(itemId: Int, message: String) {}

/**
  * A ItemState View Model exposes information about a ShoppingCart.
  */
final case class ItemStateView(itemId: Int, message: String)

object ShoppingCartReport {
  implicit val format: Format[ItemStateView] = Json.format

  // For case classes with hand-written companion objects, .tupled only works if
  // you manually extend the correct Scala function type. See SI-3664 and SI-4808.
  def tupled(t: (Int, String)) =
    ItemStateView(t._1, t._2)
}
