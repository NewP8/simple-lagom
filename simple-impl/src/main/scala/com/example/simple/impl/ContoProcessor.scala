package com.example.simple.impl

import com.example.simple.impl.Conto.ContoCreato
import com.example.simple.impl.Conto.PrelevatoDaConto
import com.example.simple.impl.Conto.VersatoInConto
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide

class ContoProcessor(readSide: SlickReadSide, repository: ContoRepository)
    extends ReadSideProcessor[Conto.Event] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Conto.Event] =
    readSide
      .builder[Conto.Event]("conto-read-side")
      .setGlobalPrepare(repository.createTable())
      .setEventHandler[ContoCreato] { nuovoConto =>
        repository.createOrUpdateItem(
          nuovoConto.entityId,
          nuovoConto.event.bilancio
        )
      }
      .setEventHandler[VersatoInConto] { nuovoConto =>
        repository.createOrUpdateItem(
          nuovoConto.entityId,
          nuovoConto.event.bilancio
        )
      }
      .setEventHandler[PrelevatoDaConto] { nuovoConto =>
        repository.createOrUpdateItem(
          nuovoConto.entityId,
          nuovoConto.event.bilancio
        )
      }
      .build()

  override def aggregateTags: Set[AggregateEventTag[Conto.Event]] =
    Set(Conto.Event.Tag)
}
