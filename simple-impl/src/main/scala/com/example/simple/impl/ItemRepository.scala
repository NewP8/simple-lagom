//package com.example.simple.impl
//
//import akka.Done
//import com.example.simple.api.ItemStateView
//import slick.dbio.DBIO
//import slick.jdbc.PostgresProfile.api._
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class ItemRepository(database: Database) {
//
//  // itemId: Int, message: String
//  class ItemTable(tag: Tag) extends Table[ItemStateView](tag, "items") {
//    def itemId = column[Int]("item_id", O.PrimaryKey)
//
//    def message = column[String]("message")
//
////    def checkoutDate = column[Option[Instant]]("checkout_date")
//
//    def * = (itemId, message).mapTo[ItemStateView]
//  }
//
//  val itemTable = TableQuery[ItemTable]
//
//  def createTable() = itemTable.schema.createIfNotExists
//
//  def findById(id: Int): Future[Option[ItemStateView]] =
//    database.run(findByIdQuery(id))
//
//  def createOrUpdateItem(itemId: Int, message: String): DBIO[Done] = {
//    findByIdQuery(itemId)
//      .flatMap {
//        case None =>
//          itemTable += ItemStateView(itemId, message)
//        case Some(iteme) =>
//          itemTable.insertOrUpdate(
//            iteme.copy(itemId = itemId, message = message)
//          )
//      }
//      .map(_ => Done)
//      .transactionally
//  }
////        case None =>
////          throw new RuntimeException(
////            s"Didn't find cart for checkout. CartID: $cartId"
////          )
//
//  private def findByIdQuery(itemId: Int): DBIO[Option[ItemStateView]] =
//    itemTable
//      .filter(_.itemId === itemId)
//      .result
//      .headOption
//}
