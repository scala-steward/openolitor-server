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
package ch.openolitor.arbeitseinsatz.repositories

import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.models.KundeId
import ch.openolitor.stammdaten.repositories.StammdatenProjektRepositoryQueries
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.{ GeschaeftsjahrFilter, QueryFilter }
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scalikejdbc._

import scala.language.postfixOps

trait ArbeitseinsatzRepositoryQueries extends LazyLogging with ArbeitseinsatzDBMappings with StammdatenDBMappings with StammdatenProjektRepositoryQueries {

  lazy val arbeitskategorie = arbeitskategorieMapping.syntax("arbeitskategorie")
  lazy val arbeitsangebot = arbeitsangebotMapping.syntax("arbeitsangebot")
  lazy val arbeitseinsatz = arbeitseinsatzMapping.syntax("arbeitseinsatz")

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val person = personMapping.syntax("person")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")

  protected def getArbeitskategorienQuery = {
    withSQL {
      select
        .from(arbeitskategorieMapping as arbeitskategorie)
    }.map(arbeitskategorieMapping(arbeitskategorie)).list
  }

  protected def getArbeitsangeboteQuery(gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]) = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .join(projektMapping as projekt)
        .where.append(UriQueryParamToSQLSyntaxBuilder.build[Arbeitsangebot](gjFilter, arbeitsangebot, "zeitVon"))
        .and.withRoundBracket(_.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "titel", arbeitsangebot))
          .or.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "bezeichnung", arbeitsangebot)))
        .orderBy(arbeitsangebot.zeitVon)
    }.map(arbeitsangebotMapping(arbeitsangebot)).list
  }

  protected def getArbeitsangebotQuery(arbeitsangebotId: ArbeitsangebotId) = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .where.eq(arbeitsangebot.id, arbeitsangebotId)
    }.map(arbeitsangebotMapping(arbeitsangebot)).single
  }

  protected def getFutureArbeitsangeboteQuery = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .where.ge(arbeitsangebot.zeitVon, new DateTime())
        .orderBy(arbeitsangebot.zeitVon)
    }.map(arbeitsangebotMapping(arbeitsangebot)).list
  }

  protected def getArbeitseinsaetzeQuery(queryString: Option[QueryFilter]) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "arbeitsangebot_titel", arbeitseinsatz))
        .or.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "kunde_bezeichnung", arbeitseinsatz))
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getArbeitseinsatzQuery(arbeitseinsatzId: ArbeitseinsatzId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.eq(arbeitseinsatz.id, arbeitseinsatzId)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).single
  }

  protected def getArbeitseinsatzDetailQuery(arbeitseinsatzId: ArbeitseinsatzId) = {
    withSQL[Arbeitseinsatz] {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .leftJoin(arbeitsangebotMapping as arbeitsangebot).on(arbeitseinsatz.arbeitsangebotId, arbeitsangebot.id)
        .where.eq(arbeitseinsatz.id, arbeitseinsatzId)
    }.one(arbeitseinsatzMapping(arbeitseinsatz))
      .toMany(
        rs => arbeitsangebotMapping.opt(arbeitsangebot)(rs)
      )
      .map({ (arbeitseinsatz, arbeitsangebote) =>
        val coworkersContact = PersonContact(arbeitseinsatz.personName.getOrElse(""), arbeitseinsatz.email)
        val arbeitsangebot = arbeitsangebote.head

        copyTo[Arbeitseinsatz, ArbeitseinsatzDetail](arbeitseinsatz, "arbeitsangebot" -> arbeitsangebot, "coworkers" -> coworkersContact)
      }).single
  }

  protected def getArbeitseinsaetzeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.eq(arbeitseinsatz.kundeId, kundeId)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getArbeitseinsaetzeQuery(arbeitsangebotId: ArbeitsangebotId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.eq(arbeitseinsatz.arbeitsangebotId, arbeitsangebotId)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getFutureArbeitseinsaetzeQuery = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.ge(arbeitseinsatz.zeitVon, new DateTime())
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getFutureArbeitseinsaetzeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.ge(arbeitseinsatz.zeitVon, new DateTime())
        .and.eq(arbeitseinsatz.kundeId, kundeId)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getArbeitseinsatzabrechnungQuery(queryString: Option[QueryFilter]) = {
    withSQL[Kunde] {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(aboTyp.id, depotlieferungAbo.abotypId).or.eq(aboTyp.id, heimlieferungAbo.abotypId).or.eq(aboTyp.id, postlieferungAbo.abotypId))
        .leftJoin(arbeitseinsatzMapping as arbeitseinsatz).on(kunde.id, arbeitseinsatz.kundeId)
        .where.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "bezeichnung", kunde))
        .orderBy(kunde.bezeichnung)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => arbeitseinsatzMapping.opt(arbeitseinsatz)(rs)
      )
      .map { (kunde, pl, hl, dl, abotypen, arbeitseinsaetze) =>
        val summeEinsaetzeSoll = abotypen map (at => at.anzahlEinsaetze.getOrElse(BigDecimal(0.0))) sum
        val summeEinsaetzeIst = arbeitseinsaetze map (ae => ae.einsatzZeit.getOrElse(BigDecimal(0.0))) sum
        val summeEinsaetzeDelta = summeEinsaetzeSoll - summeEinsaetzeIst

        ArbeitseinsatzAbrechnung(kunde.id, kunde.bezeichnung, summeEinsaetzeSoll, summeEinsaetzeIst, summeEinsaetzeDelta)
      }.list
  }

  protected def getArbeitseinsatzabrechnungOnlyAktivKundenQuery(queryString: Option[QueryFilter]) = {
    withSQL[Kunde] {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(aboTyp.id, depotlieferungAbo.abotypId).or.eq(aboTyp.id, heimlieferungAbo.abotypId).or.eq(aboTyp.id, postlieferungAbo.abotypId))
        .leftJoin(arbeitseinsatzMapping as arbeitseinsatz).on(kunde.id, arbeitseinsatz.kundeId)
        .where.append(UriQueryParamToSQLSyntaxBuilder.build(queryString, "bezeichnung", kunde))
        .and.withRoundBracket(_.eq(depotlieferungAbo.aktiv, true).or.eq(heimlieferungAbo.aktiv, true).or.eq(postlieferungAbo.aktiv, true))
        .orderBy(kunde.bezeichnung)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => arbeitseinsatzMapping.opt(arbeitseinsatz)(rs)
      )
      .map { (kunde, pl, hl, dl, abotypen, arbeitseinsaetze) =>
        val summeEinsaetzeSoll = abotypen map (at => at.anzahlEinsaetze.getOrElse(BigDecimal(0.0))) sum
        val summeEinsaetzeIst = arbeitseinsaetze map (ae => ae.einsatzZeit.getOrElse(BigDecimal(0.0))) sum
        val summeEinsaetzeDelta = summeEinsaetzeSoll - summeEinsaetzeIst

        ArbeitseinsatzAbrechnung(kunde.id, kunde.bezeichnung, summeEinsaetzeSoll, summeEinsaetzeIst, summeEinsaetzeDelta)
      }.list
  }

  protected def getArbeitsangebotCompletedQuery = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .where.lt(arbeitsangebot.zeitBis, new DateTime())
        .and.eq(arbeitsangebot.status, Offen)
        .orderBy(arbeitsangebot.zeitVon)
    }.map(arbeitsangebotMapping(arbeitsangebot)).list
  }

  protected def getArbeitseinsatzDetailByArbeitsangebotQuery(arbeitsangebotId: ArbeitsangebotId) = {
    withSQL[Arbeitseinsatz] {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .leftJoin(arbeitsangebotMapping as arbeitsangebot).on(arbeitseinsatz.arbeitsangebotId, arbeitsangebot.id)
        .where.eq(arbeitseinsatz.arbeitsangebotId, arbeitsangebotId)
    }.one(arbeitseinsatzMapping(arbeitseinsatz))
      .toMany(
        rs => arbeitsangebotMapping.opt(arbeitsangebot)(rs)
      )
      .map({ (arbeitseinsatz, arbeitsangebote) =>
        val arbeitsangebot = arbeitsangebote.head
        val coworkersContact = PersonContact(arbeitseinsatz.personName.getOrElse(""), arbeitseinsatz.email)

        copyTo[Arbeitseinsatz, ArbeitseinsatzDetail](arbeitseinsatz, "arbeitsangebot" -> arbeitsangebot, "coworkers" -> coworkersContact)
      }).list
  }

  protected def getPersonenByArbeitsangebotQuery(arbeitsangebotId: ArbeitsangebotId) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(arbeitseinsatzMapping as arbeitseinsatz).on(arbeitseinsatz.personId, person.id)
        .where.eq(arbeitseinsatz.arbeitsangebotId, arbeitsangebotId)
    }.map(personMapping(person)).list
  }
}
