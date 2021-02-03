package com.example.simple.impl

import akka.Done
import com.example.simple.api.ContoView
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContoRepository(database: Database) {

  // itemId: Int, message: String
  class ItemTable(tag: Tag) extends Table[ContoView](tag, "conti") {
    def iban = column[String]("iban", O.PrimaryKey)

    def importo = column[Int]("importo")

//    def checkoutDate = column[Option[Instant]]("checkout_date")

    def * = (iban, importo).mapTo[ContoView]
  }

  val itemTable = TableQuery[ItemTable]

  def createTable() = itemTable.schema.createIfNotExists

  def findById(iban: String): Future[Option[ContoView]] =
    database.run(findByIdQuery(iban))

  def createOrUpdateItem(iban: String, importo: Int): DBIO[Done] = {
    findByIdQuery(iban)
      .flatMap {
        case None =>
          itemTable += ContoView(iban, importo)
        case Some(iteme) =>
          itemTable.insertOrUpdate(
            iteme.copy(iban, importo)
          )
      }
      .map(_ => Done)
      .transactionally
  }

  private def findByIdQuery(iban: String): DBIO[Option[ContoView]] =
    itemTable
      .filter(_.iban === iban)
      .result
      .headOption
}
