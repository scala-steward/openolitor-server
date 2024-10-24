/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.buchhaltung.repositories

import ch.openolitor.buchhaltung.models._
import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.repositories.StammdatenProjektRepositoryQueries
import ch.openolitor.util.parsing.{ FilterAttributeList, FilterExpr, GeschaeftsjahrFilter, QueryFilter }
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

trait BuchhaltungRepositoryQueries extends LazyLogging with BuchhaltungDBMappings with StammdatenDBMappings with StammdatenProjektRepositoryQueries {
  lazy val rechnung = rechnungMapping.syntax("rechnung")
  lazy val rechnungsPosition = rechnungsPositionMapping.syntax("rechnungsPosition")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val zahlungsImport = zahlungsImportMapping.syntax("zahlungsImport")
  lazy val zahlungsEingang = zahlungsEingangMapping.syntax("zahlungsEingang")
  lazy val zahlungsExport = zahlungsExportMapping.syntax("zahlungsExport")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val zusatzAbo = zusatzAboMapping.syntax("zusatzAbo")
  lazy val kontoDaten = kontoDatenMapping.syntax("kontoDaten")
  lazy val person = personMapping.syntax("pers")

  protected def getRechnungenQuery(filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]) = {
    queryString match {
      case None =>
        withSQL {
          select
            .from(rechnungMapping as rechnung)
            .join(projektMapping as projekt)
            .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
            .where.append(
              UriQueryParamToSQLSyntaxBuilder.build[Rechnung](gjFilter, rechnung, "rechnungsDatum")
            ).and(
                UriQueryParamToSQLSyntaxBuilder.build(filter, rechnung)
              ).orderBy(rechnung.rechnungsDatum)
        }.map(rechnungMapping(rechnung)).list
      case Some(_) =>
        withSQL {
          select
            .from(rechnungMapping as rechnung)
            .join(projektMapping as projekt)
            .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
            .where.append(
              UriQueryParamToSQLSyntaxBuilder.build[Rechnung](gjFilter, rechnung, "rechnungsDatum")
            ).and(
                UriQueryParamToSQLSyntaxBuilder.build(filter, rechnung)
              ).and.append(
                  UriQueryParamToSQLSyntaxBuilder.build(queryString, "titel", rechnung)
                    .append(sqls"""or""")
                    .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "bezeichnung", kunde))
                    .append(sqls"""or""")
                    .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "id", rechnung))
                    .append(sqls"""or""")
                    .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "kundeId", rechnung))
                    .append(sqls"""or""")
                    .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "betrag", rechnung))
                )
            .orderBy(rechnung.rechnungsDatum)
        }.map(rechnungMapping(rechnung)).list
    }
  }

  protected def getRechnungsPositionQuery(filter: Option[FilterExpr], queryString: Option[QueryFilter]) = {
    queryString match {
      case None =>
        withSQL {
          select
            .from(rechnungsPositionMapping as rechnungsPosition)
            .leftJoin(kundeMapping as kunde).on(rechnungsPosition.kundeId, kunde.id)
            .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnungsPosition.aboId, depotlieferungAbo.id)
            .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnungsPosition.aboId, heimlieferungAbo.id)
            .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnungsPosition.aboId, postlieferungAbo.id)
            .leftJoin(zusatzAboMapping as zusatzAbo).on(rechnungsPosition.aboId, zusatzAbo.id)
            .where(UriQueryParamToSQLSyntaxBuilder.build(filter, rechnungsPosition))
            .orderBy(rechnungsPosition.id)
        }.map(rechnungsPositionMapping(rechnungsPosition)).list
      case Some(_) =>
        withSQL {
          select
            .from(rechnungsPositionMapping as rechnungsPosition)
            .leftJoin(kundeMapping as kunde).on(rechnungsPosition.kundeId, kunde.id)
            .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnungsPosition.aboId, depotlieferungAbo.id)
            .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnungsPosition.aboId, heimlieferungAbo.id)
            .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnungsPosition.aboId, postlieferungAbo.id)
            .leftJoin(zusatzAboMapping as zusatzAbo).on(rechnungsPosition.aboId, zusatzAbo.id)
            .where.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "beschrieb", rechnungsPosition))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "id", rechnungsPosition))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "abotypName", depotlieferungAbo))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "abotypName", heimlieferungAbo))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "abotypName", postlieferungAbo))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "abotypName", zusatzAbo))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "id", kunde))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "bezeichnung", kunde))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "aboId", rechnungsPosition))
            .append(sqls"""or""")
            .append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "betrag", rechnungsPosition))
            .and(UriQueryParamToSQLSyntaxBuilder.build(filter, rechnungsPosition))
            .orderBy(rechnungsPosition.id)
        }.map(rechnungsPositionMapping(rechnungsPosition)).list
    }
  }

  protected def getRechnungsPositionenByRechnungsIdQuery(rechnungId: RechnungId) = {
    withSQL {
      select
        .from(rechnungsPositionMapping as rechnungsPosition)
        .where.eq(rechnungsPosition.rechnungId, rechnungId)
        .orderBy(rechnungsPosition.id)
    }.map(rechnungsPositionMapping(rechnungsPosition)).list
  }

  protected def getKundenRechnungenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where.eq(rechnung.kundeId, kundeId)
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).list
  }

  protected def getRechnungDetailQuery(id: RechnungId) = {
    withSQL[Rechnung] {
      select
        .from(rechnungMapping as rechnung)
        .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
        .leftJoin(rechnungsPositionMapping as rechnungsPosition).on(rechnung.id, rechnungsPosition.rechnungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnungsPosition.aboId, depotlieferungAbo.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnungsPosition.aboId, heimlieferungAbo.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnungsPosition.aboId, postlieferungAbo.id)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(rechnungsPosition.aboId, zusatzAbo.id)
        .leftJoin(kontoDatenMapping as kontoDaten).on(kontoDaten.kunde, rechnung.kundeId)
        .where.eq(rechnung.id, id)
        .orderBy(rechnung.rechnungsDatum)
    }.one(rechnungMapping(rechnung))
      .toManies(
        rs => kundeMapping.opt(kunde)(rs),
        rs => rechnungsPositionMapping.opt(rechnungsPosition)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs),
        rs => kontoDatenMapping.opt(kontoDaten)(rs)
      )
      .map({ (rechnung, kunden, rechnungsPositionen, pl, hl, dl, zusatzAbos, kontoDaten) =>
        val kunde = kunden.head
        val abos = pl ++ hl ++ dl ++ zusatzAbos
        val kundeKontoDaten = kontoDaten.head
        val rechnungsPositionenDetail = {
          for {
            rechnungsPosition <- rechnungsPositionen
            abo <- abos.find(_.id == rechnungsPosition.aboId.orNull)
          } yield {
            copyTo[RechnungsPosition, RechnungsPositionDetail](rechnungsPosition, "abo" -> abo)
          }
        }.sortBy(_.sort.getOrElse(0))

        copyTo[Rechnung, RechnungDetail](rechnung, "kunde" -> kunde, "rechnungsPositionen" -> rechnungsPositionenDetail, "kundeKontoDaten" -> kundeKontoDaten)
      }).single
  }

  protected def getRechnungByReferenznummerQuery(referenzNummer: String) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where.eq(rechnung.referenzNummer, referenzNummer)
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).single
  }

  protected def getZahlungsImportsQuery(filter: Option[FilterExpr], queryString: Option[QueryFilter]) = {
    queryString match {
      case None =>
        withSQL {
          select
            .from(zahlungsImportMapping as zahlungsImport)
            .where(UriQueryParamToSQLSyntaxBuilder.build(filter, zahlungsImport))
        }.map(zahlungsImportMapping(zahlungsImport)).list
      case _ =>
        withSQL {
          select
            .from(zahlungsImportMapping as zahlungsImport)
            .where.withRoundBracket(
              _.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "id", zahlungsImport))
                .or.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "anzahl_zahlungs_eingaenge", zahlungsImport))
                .or.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "anzahl_zahlungs_eingaenge_erledigt", zahlungsImport))
            )
            .and(UriQueryParamToSQLSyntaxBuilder.build(filter, zahlungsImport))
        }.map(zahlungsImportMapping(zahlungsImport)).list
    }
  }

  protected def getZahlungsImportDetailQuery(id: ZahlungsImportId) = {
    withSQL[ZahlungsImport] {
      select
        .from(zahlungsImportMapping as zahlungsImport)
        .leftJoin(zahlungsEingangMapping as zahlungsEingang).on(zahlungsImport.id, zahlungsEingang.zahlungsImportId)
        .where.eq(zahlungsImport.id, id)
    }.one(zahlungsImportMapping(zahlungsImport))
      .toMany(
        rs => zahlungsEingangMapping.opt(zahlungsEingang)(rs)
      )
      .map({ (zahlungsImport, zahlungsEingaenge) =>
        copyTo[ZahlungsImport, ZahlungsImportDetail](zahlungsImport, "zahlungsEingaenge" -> zahlungsEingaenge)
      }).single
  }

  protected def getZahlungsExportsQuery = {
    withSQL {
      select
        .from(zahlungsExportMapping as zahlungsExport)
    }.map(zahlungsExportMapping(zahlungsExport)).list
  }

  protected def getZahlungsExportQuery(id: ZahlungsExportId) = {
    withSQL {
      select
        .from(zahlungsExportMapping as zahlungsExport)
        .where.eq(zahlungsExport.id, id)
    }.map(zahlungsExportMapping(zahlungsExport)).single
  }

  protected def getZahlungsEingangByReferenznummerQuery(referenzNummer: String) = {
    withSQL {
      select
        .from(zahlungsEingangMapping as zahlungsEingang)
        .where.eq(zahlungsEingang.referenzNummer, referenzNummer)
        .orderBy(zahlungsEingang.modifidat).desc
    }.map(zahlungsEingangMapping(zahlungsEingang)).first
  }

  protected def getKontoDatenProjektQuery = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
        .where.isNull(kontoDaten.kunde)
    }.map(kontoDatenMapping(kontoDaten)).single
  }

  protected def getPersonQuery(rechnungId: RechnungId) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(rechnungMapping as rechnung).on(rechnung.kundeId, person.kundeId)
        .where.eq(rechnung.id, rechnungId)
    }.map(personMapping(person)).list
  }

  protected def getKontoDatenKundeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
        .where.eq(kontoDaten.kunde, kundeId)
    }.map(kontoDatenMapping(kontoDaten)).single
  }
}
