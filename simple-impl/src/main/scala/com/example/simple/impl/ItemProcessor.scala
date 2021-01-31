//package com.example.simple.impl
//
//import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
//import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
//import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
//
//class ItemProcessor(readSide: SlickReadSide, repository: ItemRepository)
//    extends ReadSideProcessor[ItemEvent] {
//
//  override def buildHandler(): ReadSideProcessor.ReadSideHandler[ItemEvent] =
//    readSide
//      .builder[ItemEvent]("item-read-side")
//      .setGlobalPrepare(repository.createTable())
//      .setEventHandler[ItemAdded] { itemAdded =>
//        repository.createOrUpdateItem(
//          itemAdded.event.itemId,
//          itemAdded.event.message
//        )
//      }
//      //      .setEventHandler[ItemRemoved] { envelope =>
//      //        DBIOAction.successful(Done) // not used in report
//      //      }.. e altri event handler...
//      .build()
//
//  override def aggregateTags: Set[AggregateEventTag[ItemEvent]] =
//    ItemEvent.Tag.allTags
//}
