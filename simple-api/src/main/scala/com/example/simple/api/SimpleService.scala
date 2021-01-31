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

case class ItemState(itemId: Int, message: String) {}

final case class ItemStateView(itemId: Int, message: String)
