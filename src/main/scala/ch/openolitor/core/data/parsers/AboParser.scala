package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object AboParser extends EntityParser {
  import EntityParser._

  def parse(kundeIdMapping: Map[Long, KundeId], kunden: List[Kunde], vertriebsartIdMapping: Map[Long, VertriebsartId], vertriebsarten: List[Vertriebsart], vertriebe: List[Vertrieb],
    abotypen: List[Abotyp], depotIdMapping: Map[Long, DepotId], depots: List[Depot],
    tourIdMapping: Map[Long, TourId], tours: List[Tour], abwesenheiten: List[Abwesenheit])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Abo, AboId]("id", Seq("kunde_id", "vertriebsart_id", "start", "ende",
      "guthaben_vertraglich", "guthaben", "guthaben_in_rechnung", "letzte_lieferung", "anzahl_abwesenheiten", "anzahl_lieferungen", "anzahl_einsaetze",
      "depot_id", "tour_id") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(kundeIdIndex, vertriebsartIdIndex, startIndex, endeIndex,
          guthabenVertraglichIndex, guthabenIndex, guthabenInRechnungIndex, indexLetzteLieferung, indexAnzahlAbwesenheiten, lieferungenIndex,
          einsaetzeIndex, depotIdIndex, tourIdIndex) = indexes take (12)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val kundeIdInt = row.value[Long](kundeIdIndex)
        val vertriebsartIdInt = row.value[Long](vertriebsartIdIndex)
        val start = row.value[DateTime](startIndex)
        val ende = row.value[Option[DateTime]](endeIndex)
        val aboId = AboId(id)

        val guthabenVertraglich = row.value[Option[Int]](guthabenVertraglichIndex)
        val guthaben = row.value[Int](guthabenIndex)
        val guthabenInRechnung = row.value[Int](guthabenInRechnungIndex)

        val letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung)
        //calculate count
        val anzahlAbwesenheiten = parseTreeMap(row.value[String](indexAnzahlAbwesenheiten))(identity, _.toInt)
        val anzahlLieferungen = parseTreeMap(row.value[String](lieferungenIndex))(identity, _.toInt)
        val anzahlEinsaetze = parseTreeMap(row.value[String](einsaetzeIndex))(identity, _.toInt)

        val erstelldat = row.value[DateTime](indexErstelldat)
        val ersteller = PersonId(row.value[Long](indexErsteller))
        val modifidat = row.value[DateTime](indexModifidat)
        val modifikator = PersonId(row.value[Long](indexModifikator))

        val kundeId = kundeIdMapping getOrElse (kundeIdInt, throw ParseException(s"Kunde id $kundeIdInt referenced from abo not found"))
        val kunde = (kunden filter (_.id == kundeId)).headOption map (_.bezeichnung) getOrElse (throw ParseException(s"Kunde not found for id:$kundeId"))

        val vertriebsartId = vertriebsartIdMapping getOrElse (vertriebsartIdInt, throw ParseException(s"Vertriebsart id $vertriebsartIdInt referenced from abo not found"))
        val vertriebsart = (vertriebsarten filter (_.id == vertriebsartId)).headOption getOrElse (throw ParseException(s"Vertriebsart not found for id:$vertriebsartId"))
        val vertriebId = vertriebsart.vertriebId
        val vertrieb = (vertriebe filter (_.id == vertriebId)).headOption getOrElse (throw ParseException(s"Vertrieb not found for id:$vertriebId"))
        val abotypId = vertrieb.abotypId;
        val abotypName = (abotypen filter (_.id == abotypId)).headOption map (_.name) getOrElse (throw ParseException(s"Abotyp not found for id:$abotypId"))
        val depotIdOpt = row.value[Option[Long]](depotIdIndex)
        val tourIdOpt = row.value[Option[Long]](tourIdIndex)
        val vertriebBeschrieb = vertrieb.beschrieb
        val aktiv = IAbo.calculateAktiv(start, ende)

        depotIdOpt map { depotIdInt =>
          val depotId = depotIdMapping getOrElse (depotIdInt, throw ParseException(s"Depot id $depotIdInt referenced from abo not found"))
          val depotName = (depots filter (_.id == depotId)).headOption map (_.name) getOrElse (s"Depot not found with id:$depotId")
          DepotlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, vertriebBeschrieb, abotypId, abotypName, depotId, depotName,
            start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
            anzahlLieferungen, anzahlEinsaetze, aktiv, erstelldat, ersteller, modifidat, modifikator)
        } getOrElse {
          tourIdOpt map { tourIdInt =>
            val tourId = tourIdMapping getOrElse (tourIdInt, throw ParseException(s"Tour id tourIdInt referenced from abo not found"))
            val tourName = (tours filter (_.id == tourId)).headOption map (_.name) getOrElse (s"Tour not found with id:$tourId")
            HeimlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, vertriebBeschrieb, abotypId, abotypName, tourId, tourName,
              start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
              anzahlLieferungen, anzahlEinsaetze, aktiv, erstelldat, ersteller, modifidat, modifikator)
          } getOrElse {
            PostlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, vertriebBeschrieb, abotypId, abotypName,
              start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
              anzahlLieferungen, anzahlEinsaetze, aktiv, erstelldat, ersteller, modifidat, modifikator)
          }
        }
    }
  }
}
