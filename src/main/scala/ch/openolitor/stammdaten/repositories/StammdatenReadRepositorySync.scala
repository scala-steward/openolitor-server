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
package ch.openolitor.stammdaten.repositories

import scalikejdbc._
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime
import org.joda.time.LocalDate
import com.typesafe.scalalogging.LazyLogging

trait StammdatenReadRepositorySync extends BaseReadRepositorySync with ProjektReadRepositorySync {
  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp]
  def getZusatzAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[ZusatzAbotyp]
  def getAboDetail(id: AboId)(implicit session: DBSession): Option[AboDetail]
  def getAboDetailAusstehend(id: AboId)(implicit session: DBSession): Option[AboDetail]
  def getAbosByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Abo]
  def getAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[Abo]
  def getZusatzAbosByHauptAbo(hauptAboId: AboId)(implicit session: DBSession): List[ZusatzAbo]
  def getZusatzAbosByZusatzabotyp(zusatzabotyp: AbotypId)(implicit session: DBSession): List[ZusatzAbo]
  def getAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo]
  def getDepotAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo]
  def getPostAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo]
  def getHeimAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo]
  def getHauptAbo(id: AboId)(implicit session: DBSession): Option[HauptAbo]
  def getExistingZusatzAbotypen(lieferungId: LieferungId)(implicit session: DBSession): List[ZusatzAbotyp]
  def getAbotypById(id: AbotypId)(implicit session: DBSession): Option[IAbotyp]
  @deprecated("Exists for compatibility purposes only", "OO 2.2 (Arbeitseinsatz)")
  def getProjektV1(implicit session: DBSession): Option[ProjektV1]
  def getKontoDatenProjekt(implicit session: DBSession): Option[KontoDaten]
  def getKontoDatenKunde(kundeId: KundeId)(implicit session: DBSession): Option[KontoDaten]
  def getKunden(implicit session: DBSession): List[Kunde]
  def getKundenByKundentyp(kundentyp: KundentypId)(implicit session: DBSession): List[Kunde]
  def getCustomKundentypen(implicit session: DBSession): List[CustomKundentyp]
  def getPersonen(implicit session: DBSession): List[Person]
  def getPersonen(kundeId: KundeId)(implicit session: DBSession): List[Person]
  def getPersonenForAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Person]
  def getPersonenForZusatzabotyp(abotypId: AbotypId)(implicit session: DBSession): List[Person]
  def getPersonen(tourId: TourId)(implicit session: DBSession): List[Person]
  def getPersonen(DepotId: DepotId)(implicit session: DBSession): List[Person]
  def getPersonByCategory(category: PersonCategoryNameId)(implicit session: DBSession): List[Person]
  def getPersonByEmail(email: String)(implicit session: DBSession): Option[Person]
  def getPersonCategory(implicit session: DBSession): List[PersonCategory]
  def getPendenzen(id: KundeId)(implicit session: DBSession): List[Pendenz]

  def getLieferplanung(abotypName: String)(implicit session: DBSession): List[Lieferplanung]
  def getLatestLieferplanung(implicit session: DBSession): Option[Lieferplanung]
  def getOpenLieferplanung(implicit session: DBSession): List[Lieferplanung]
  def getLieferungenNext()(implicit session: DBSession): List[Lieferung]
  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit session: DBSession): Option[Lieferung]
  def getLieferplanung(id: LieferplanungId)(implicit session: DBSession): Option[Lieferplanung]
  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Lieferposition]
  def getLieferpositionenByLieferplanAndProduzent(id: LieferplanungId, produzentId: ProduzentId, datum: DateTime)(implicit session: DBSession): List[Lieferposition]
  def getLieferpositionenByLieferung(id: LieferungId)(implicit session: DBSession): List[Lieferposition]
  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung]
  def getProduktProduzenten(id: ProduktId)(implicit session: DBSession): List[ProduktProduzent]
  def getProduzentDetail(id: ProduzentId)(implicit session: DBSession): Option[Produzent]
  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit session: DBSession): Option[Produzent]
  def getProduktProduktekategorien(id: ProduktId)(implicit session: DBSession): List[ProduktProduktekategorie]
  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit session: DBSession): Option[Produktekategorie]
  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit session: DBSession): List[Produkt]
  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Korb]
  def getZusatzAboKorb(hauptlieferungId: LieferungId, zusatzAboId: AboId)(implicit session: DBSession): List[Korb]
  def getKoerbe(lieferungId: LieferungId)(implicit session: DBSession): List[Korb]
  def getNichtGelieferteKoerbe(lieferungId: LieferungId)(implicit session: DBSession): List[Korb]
  def getKoerbe(datum: DateTime, vertriebsartId: VertriebsartId, status: KorbStatus)(implicit session: DBSession): List[Korb]
  def getKoerbe(datum: DateTime, vertriebsartIds: List[VertriebsartId], status: KorbStatus)(implicit session: DBSession): List[Korb]
  def getKoerbe(auslieferungId: AuslieferungId)(implicit session: DBSession): List[Korb]
  def getKoerbeNichtAusgeliefertByAbo(aboId: AboId)(implicit session: DBSession): List[Korb]
  def getKoerbeNichtAusgeliefertLieferungClosedByAbo(aboId: AboId)(implicit session: DBSession): List[Korb]
  def getKorbLatestWirdGeliefert(aboId: AboId, beforeDate: DateTime)(implicit session: DBSession): Option[Korb]
  def getKorbeLaterWirdGeliefert(korbId: KorbId)(implicit session: DBSession): List[Korb]
  def countKoerbe(auslieferungId: AuslieferungId)(implicit session: DBSession): Option[Int]
  def getAktiveAbos(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[Abo]
  def getAktiveZusatzAbos(abotypId: AbotypId, hauptvertriebId: VertriebId, lieferdatum: DateTime, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[Abo]
  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Int]
  def countAbwesend(aboId: AboId, datum: LocalDate)(implicit session: DBSession): Option[Int]
  def getLieferung(id: AbwesenheitId)(implicit session: DBSession): Option[Lieferung]
  def getExistingZusatzaboLieferung(zusatzAbotypId: AbotypId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit session: DBSession): Option[Lieferung]
  def getLieferungen(id: LieferplanungId)(implicit session: DBSession): List[Lieferung]
  def getLieferungen(id: VertriebId)(implicit session: DBSession): List[Lieferung]
  def getLieferungen(abotypId: AbotypId, vertriebId: VertriebId, datum: DateTime)(implicit session: DBSession): Option[Lieferung]
  def getLieferungenDetails(id: LieferplanungId)(implicit session: DBSession): List[LieferungDetail]
  def sumPreisTotalGeplanteLieferungenVorher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime, startGeschaeftsjahr: DateTime)(implicit session: DBSession): Option[BigDecimal]
  def getGeplanteLieferungVorher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime)(implicit session: DBSession): Option[Lieferung]
  def getGeplanteLieferungNachher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime)(implicit session: DBSession): Option[Lieferung]
  def countEarlierLieferungOffen(id: LieferplanungId)(implicit session: DBSession): Option[Int]
  def getSammelbestellungen(id: LieferplanungId)(implicit session: DBSession): List[Sammelbestellung]
  def getSammelbestellungen(id: LieferungId)(implicit session: DBSession): List[Sammelbestellung]
  def getSammelbestellungenByProduzent(produzent: ProduzentId, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[Sammelbestellung]
  def getBestellung(id: SammelbestellungId, adminProzente: BigDecimal)(implicit session: DBSession): Option[Bestellung]
  def getBestellungen(id: SammelbestellungId)(implicit session: DBSession): List[Bestellung]
  def getBestellpositionen(id: BestellungId)(implicit session: DBSession): List[Bestellposition]
  def getBestellpositionenBySammelbestellung(id: SammelbestellungId)(implicit session: DBSession): List[Bestellposition]
  def getVertriebsarten(vertriebId: VertriebId)(implicit session: DBSession): List[VertriebsartDetail]
  def getVertrieb(vertriebId: VertriebId)(implicit session: DBSession): Option[Vertrieb]
  def getVertriebe(abotypId: AbotypId)(implicit session: DBSession): List[VertriebVertriebsarten]
  def getVertriebByDate(datum: DateTime)(implicit session: DBSession): List[Vertrieb]
  def getKundeDetail(kundeId: KundeId)(implicit session: DBSession): Option[KundeDetail]
  def getLieferungenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung]
  def getLieferungenOffenOrAbgeschlossenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung]
  def getLieferungenOffenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung]
  def getLieferungenOffenByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[Lieferung]

  def getAbwesenheit(aboId: AboId, datum: DateTime)(implicit session: DBSession): List[Abwesenheit]
  def getAbwesenheiten(aboId: AboId)(implicit session: DBSession): List[Abwesenheit]

  def getTourlieferungenByKunde(id: KundeId)(implicit session: DBSession): List[Tourlieferung]

  def getDepotAuslieferung(depotId: DepotId, datum: DateTime)(implicit session: DBSession): Option[DepotAuslieferung]
  def getTourAuslieferung(tourId: TourId, datum: DateTime)(implicit session: DBSession): Option[TourAuslieferung]
  def getPostAuslieferung(datum: DateTime)(implicit session: DBSession): Option[PostAuslieferung]
  def getDepotlieferungAbosByDepot(id: DepotId)(implicit session: DBSession): List[DepotlieferungAbo]

  def getTourlieferungen(id: TourId)(implicit session: DBSession): List[Tourlieferung]
  def getHeimlieferung(tourId: TourId)(implicit session: DBSession): List[Heimlieferung]
  def getDepotlieferung(depotId: DepotId)(implicit session: DBSession): List[Depotlieferung]

  def getDepotlieferungAbo(id: AboId)(implicit session: DBSession): Option[DepotlieferungAboDetail]
  def getHeimlieferungAbo(id: AboId)(implicit session: DBSession): Option[HeimlieferungAboDetail]
  def getPostlieferungAbo(id: AboId)(implicit session: DBSession): Option[PostlieferungAboDetail]

  def getAbo(id: AboId)(implicit session: DBSession): Option[Abo]
}

trait StammdatenReadRepositorySyncImpl extends StammdatenReadRepositorySync with LazyLogging with StammdatenRepositoryQueries with ProjektReadRepositorySyncImpl {

  def getAbotypById(id: AbotypId)(implicit session: DBSession): Option[IAbotyp] = {
    getById(abotypMapping, id) orElse getById(zusatzAbotypMapping, id)
  }

  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp] = {
    getAbotypDetailQuery(id).apply()
  }

  def getZusatzAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[ZusatzAbotyp] = {
    getZusatzAbotypDetailQuery(id).apply()
  }

  def getAbosByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Abo] = {
    getDepotlieferungAbos(abotypId) ::: getHeimlieferungAbos(abotypId) ::: getPostlieferungAbos(abotypId)
  }

  def getDepotlieferungAbos(abotypId: AbotypId)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getDepotlieferungAbosQuery(abotypId).apply()
  }

  def getHeimlieferungAbos(abotypId: AbotypId)(implicit session: DBSession): List[HeimlieferungAbo] = {
    getHeimlieferungAbosQuery(abotypId).apply()
  }

  def getPostlieferungAbos(abotypId: AbotypId)(implicit session: DBSession): List[PostlieferungAbo] = {
    getPostlieferungAbosQuery(abotypId).apply()
  }

  def getAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[Abo] = {
    getDepotlieferungAbosByVertrieb(vertriebId) :::
      getHeimlieferungAbosByVertrieb(vertriebId) :::
      getPostlieferungAbosByVertrieb(vertriebId) :::
      getZusatzAbosByVertrieb(vertriebId)
  }

  def getDepotlieferungAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getDepotlieferungAbosByVertriebQuery(vertriebId).apply()
  }

  def getHeimlieferungAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[HeimlieferungAbo] = {
    getHeimlieferungAbosByVertriebQuery(vertriebId).apply()
  }

  def getPostlieferungAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[PostlieferungAbo] = {
    getPostlieferungAbosByVertriebQuery(vertriebId).apply()
  }

  def getZusatzAbosByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[ZusatzAbo] = {
    getZusatzAbosByVertriebQuery(vertriebId).apply()
  }

  def getZusatzAbosByHauptAbo(hauptAboId: AboId)(implicit session: DBSession): List[ZusatzAbo] = {
    getZusatzAbosByHauptAboQuery(hauptAboId).apply()
  }

  def getZusatzAbosByZusatzabotyp(zusatzabotyp: AbotypId)(implicit session: DBSession): List[ZusatzAbo] = {
    getZusatzAbosByZusatzabotypQuery(zusatzabotyp).apply()
  }

  def getAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo] = {
    getDepotAbosByZusatzAboId(zusatzaboId) :::
      getPostAbosByZusatzAboId(zusatzaboId) :::
      getHeimAbosByZusatzAboId(zusatzaboId)
  }

  def getDepotAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo] = {
    getDepotAbosByZusatzAboIdQuery(zusatzaboId).apply()
  }

  def getPostAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo] = {
    getPostAbosByZusatzAboIdQuery(zusatzaboId).apply()
  }

  def getHeimAbosByZusatzAboId(zusatzaboId: AboId)(implicit session: DBSession): List[HauptAbo] = {
    getHeimAbosByZusatzAboIdQuery(zusatzaboId).apply()
  }

  def getHauptAbo(id: AboId)(implicit session: DBSession): Option[HauptAbo] = {
    val hauptAboId = getZusatzAboDetail(id).get.hauptAboId
    getById(depotlieferungAboMapping, hauptAboId) orElse getById(heimlieferungAboMapping, hauptAboId) orElse getById(postlieferungAboMapping, hauptAboId)
  }

  def getExistingZusatzAbotypen(lieferungId: LieferungId)(implicit session: DBSession): List[ZusatzAbotyp] = {
    getExistingZusatzAbotypenQuery(lieferungId).apply()
  }

  @deprecated("Exists for compatibility purposes only", "OO 2.2 (Arbeitseinsatz)")
  def getProjektV1(implicit session: DBSession): Option[ProjektV1] = {
    getProjektV1Query.apply()
  }

  def getKontoDatenProjekt(implicit session: DBSession): Option[KontoDaten] = {
    getKontoDatenProjektQuery.apply()
  }

  def getKontoDatenKunde(kundeId: KundeId)(implicit session: DBSession): Option[KontoDaten] = {
    getKontoDatenKundeQuery(kundeId).apply()
  }

  def getAboDetail(id: AboId)(implicit session: DBSession): Option[AboDetail] = {
    getDepotlieferungAbo(id) orElse getHeimlieferungAbo(id) orElse getPostlieferungAbo(id)
  }

  def getZusatzAboDetail(id: AboId)(implicit session: DBSession): Option[ZusatzAbo] = {
    getZusatzAboDetailQuery(id).apply()
  }

  def getDepotlieferungAbo(id: AboId)(implicit session: DBSession): Option[DepotlieferungAboDetail] = {
    getDepotlieferungAboQuery(id).apply()
  }

  def getHeimlieferungAbo(id: AboId)(implicit session: DBSession): Option[HeimlieferungAboDetail] = {
    getHeimlieferungAboQuery(id).apply()
  }

  def getPostlieferungAbo(id: AboId)(implicit session: DBSession): Option[PostlieferungAboDetail] = {
    getPostlieferungAboQuery(id).apply()
  }

  def getAboDetailAusstehend(id: AboId)(implicit session: DBSession): Option[AboDetail] = {
    getDepotlieferungAboAusstehend(id) orElse getHeimlieferungAboAusstehend(id) orElse getPostlieferungAboAusstehend(id)
  }

  def getDepotlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[DepotlieferungAboDetail] = {
    getDepotlieferungAboAusstehendQuery(id).apply()
  }

  def getHeimlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[HeimlieferungAboDetail] = {
    getHeimlieferungAboAusstehendQuery(id).apply()
  }

  def getPostlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[PostlieferungAboDetail] = {
    getPostlieferungAboAusstehendQuery(id).apply()
  }

  def getKunden(implicit session: DBSession): List[Kunde] = {
    getKundenQuery.apply()
  }
  def getKundenByKundentyp(kundentyp: KundentypId)(implicit session: DBSession): List[Kunde] = {
    getKundenByKundentypQuery(kundentyp).apply()
  }

  def getCustomKundentypen(implicit session: DBSession): List[CustomKundentyp] = {
    getCustomKundentypenQuery.apply()
  }

  def getPersonen(implicit session: DBSession): List[Person] = {
    getPersonenQuery.apply()
  }

  def getPersonByCategory(category: PersonCategoryNameId)(implicit session: DBSession): List[Person] = {
    getPersonByCategoryQuery(category).apply()
  }

  def getPersonByEmail(email: String)(implicit session: DBSession): Option[Person] = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.email, email)
    }.map(personMapping(person)).single.apply()
  }

  def getPersonCategory(implicit session: DBSession): List[PersonCategory] = {
    getPersonCategoryQuery.apply()
  }

  def getPersonen(kundeId: KundeId)(implicit session: DBSession): List[Person] = {
    getPersonenQuery(kundeId).apply()
  }

  def getPersonenForAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Person] = {
    getPersonenForAbotypQuery(abotypId).apply()
  }

  def getPersonenForZusatzabotyp(abotypId: AbotypId)(implicit session: DBSession): List[Person] = {
    getPersonenForZusatzabotypQuery(abotypId).apply()
  }

  def getPersonen(tourId: TourId)(implicit session: DBSession): List[Person] = {
    getPersonenQuery(tourId).apply()
  }

  def getPersonen(depotId: DepotId)(implicit session: DBSession): List[Person] = {
    getPersonenQuery(depotId).apply()
  }

  def getPendenzen(id: KundeId)(implicit session: DBSession): List[Pendenz] = {
    getPendenzenQuery(id).apply()
  }

  def getLieferplanung(abotypName: String)(implicit session: DBSession): List[Lieferplanung] = {
    getLieferplanungenQuery(abotypName).apply()
  }

  def getLatestLieferplanung(implicit session: DBSession): Option[Lieferplanung] = {
    getLatestLieferplanungQuery.apply()
  }

  def getOpenLieferplanung(implicit session: DBSession): List[Lieferplanung] = {
    getOpenLieferplanungQuery.apply()
  }

  def getLieferung(id: AbwesenheitId)(implicit session: DBSession): Option[Lieferung] = {
    getLieferungQuery(id).apply()
  }

  def getExistingZusatzaboLieferung(zusatzAbotypId: AbotypId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit session: DBSession): Option[Lieferung] = {
    getExistingZusatzaboLieferungQuery(zusatzAbotypId, lieferplanungId, datum).apply()
  }

  def getLieferungenNext()(implicit session: DBSession): List[Lieferung] = {
    getLieferungenNextQuery.apply()
  }

  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit session: DBSession): Option[Lieferung] = {
    getLastGeplanteLieferungQuery(abotypId).apply()
  }

  def getLieferplanung(id: LieferplanungId)(implicit session: DBSession): Option[Lieferplanung] = {
    getLieferplanungQuery(id).apply()
  }

  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Lieferposition] = {
    getLieferpositionenByLieferplanQuery(id).apply()
  }

  def getLieferpositionenByLieferplanAndProduzent(id: LieferplanungId, produzentId: ProduzentId, datum: DateTime)(implicit session: DBSession): List[Lieferposition] = {
    getLieferpositionenByLieferplanAndProduzentQuery(id, produzentId, datum).apply()
  }

  def getLieferpositionenByLieferung(id: LieferungId)(implicit session: DBSession): List[Lieferposition] = {
    getLieferpositionenByLieferungQuery(id).apply()
  }

  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung] = {
    getUngeplanteLieferungenQuery(abotypId).apply()
  }

  def getProduktProduzenten(id: ProduktId)(implicit session: DBSession): List[ProduktProduzent] = {
    getProduktProduzentenQuery(id).apply()
  }

  def getProduzentDetail(id: ProduzentId)(implicit session: DBSession): Option[Produzent] = {
    getProduzentDetailQuery(id).apply()
  }

  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit session: DBSession): Option[Produzent] = {
    getProduzentDetailByKurzzeichenQuery(kurzzeichen).apply()
  }

  def getProduktProduktekategorien(id: ProduktId)(implicit session: DBSession): List[ProduktProduktekategorie] = {
    getProduktProduktekategorienQuery(id).apply()
  }

  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit session: DBSession): Option[Produktekategorie] = {
    getProduktekategorieByBezeichnungQuery(bezeichnung).apply()
  }

  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit session: DBSession): List[Produkt] = {
    getProdukteByProduktekategorieBezeichnungQuery(bezeichnung).apply()
  }

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Korb] = {
    getKorbQuery(lieferungId, aboId).apply()
  }

  def getZusatzAboKorb(hauptlieferungId: LieferungId, hauptAboId: AboId)(implicit session: DBSession): List[Korb] = {
    getZusatzAboKorbQuery(hauptlieferungId, hauptAboId).apply()
  }

  def getKoerbe(lieferungId: LieferungId)(implicit session: DBSession): List[Korb] = {
    getKoerbeQuery(lieferungId).apply()
  }

  def getNichtGelieferteKoerbe(lieferungId: LieferungId)(implicit session: DBSession): List[Korb] = {
    getNichtGelieferteKoerbeQuery(lieferungId).apply()
  }

  def getKoerbe(datum: DateTime, vertriebsartId: VertriebsartId, status: KorbStatus)(implicit session: DBSession): List[Korb] = {
    getKoerbeQuery(datum, vertriebsartId :: Nil, status).apply()
  }

  def getKoerbe(datum: DateTime, vertriebsartIds: List[VertriebsartId], status: KorbStatus)(implicit session: DBSession): List[Korb] = {
    getKoerbeQuery(datum, vertriebsartIds, status).apply()
  }

  def getKoerbe(auslieferungId: AuslieferungId)(implicit session: DBSession): List[Korb] = {
    getKoerbeQuery(auslieferungId).apply()
  }

  def getKoerbeNichtAusgeliefertByAbo(aboId: AboId)(implicit session: DBSession): List[Korb] = {
    getKoerbeNichtAusgeliefertByAboQuery(aboId)()
  }

  def getKoerbeNichtAusgeliefertLieferungClosedByAbo(aboId: AboId)(implicit session: DBSession): List[Korb] = {
    getKoerbeNichtAusgeliefertLieferungClosedByAboQuery(aboId)()
  }

  def getKorbLatestWirdGeliefert(aboId: AboId, beforeDate: DateTime)(implicit session: DBSession): Option[Korb] = {
    getKorbLatestWirdGeliefertQuery(aboId, beforeDate)()
  }

  def getKorbeLaterWirdGeliefert(korbId: KorbId)(implicit session: DBSession): List[Korb] = {
    getKorbeLaterWirdGeliefertQuery(korbId)()
  }

  def getAktiveAbos(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[Abo] = {
    getAktiveDepotlieferungAbos(abotypId, vertriebId, lieferdatum) :::
      getAktiveHeimlieferungAbos(abotypId, vertriebId, lieferdatum) :::
      getAktivePostlieferungAbos(abotypId, vertriebId, lieferdatum) :::
      getAktiveZusatzAbos(abotypId, vertriebId, lieferdatum, lieferplanungId)
  }

  def getAktiveDepotlieferungAbos(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getAktiveDepotlieferungAbosQuery(abotypId, vertriebId, lieferdatum).apply()
  }

  def getAktiveHeimlieferungAbos(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[HeimlieferungAbo] = {
    getAktiveHeimlieferungAbosQuery(abotypId, vertriebId, lieferdatum).apply()
  }

  def getAktivePostlieferungAbos(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[PostlieferungAbo] = {
    getAktivePostlieferungAbosQuery(abotypId, vertriebId, lieferdatum).apply()
  }

  def getAktiveZusatzAbos(abotypId: AbotypId, hauptAboVertriebId: VertriebId, lieferdatum: DateTime, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[ZusatzAbo] = {
    getAktiveZusatzAbosQuery(abotypId, hauptAboVertriebId, lieferdatum, lieferplanungId).apply()
  }

  def countKoerbe(auslieferungId: AuslieferungId)(implicit session: DBSession): Option[Int] = {
    countKoerbeQuery(auslieferungId).apply()
  }

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Int] = {
    countAbwesendQuery(lieferungId, aboId).apply()
  }

  def countAbwesend(aboId: AboId, datum: LocalDate)(implicit session: DBSession): Option[Int] = {
    countAbwesendQuery(aboId, datum).apply()
  }

  def getLieferungen(id: LieferplanungId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenQuery(id).apply()
  }

  def getLieferungen(abotypId: AbotypId, vertriebId: VertriebId, datum: DateTime)(implicit session: DBSession): Option[Lieferung] = {
    getLieferungenQuery(abotypId, vertriebId, datum).apply()
  }
  def getLieferungen(id: VertriebId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenQuery(id).apply()
  }

  def getLieferungenDetails(id: LieferplanungId)(implicit session: DBSession): List[LieferungDetail] = {
    getLieferungenDetailsQuery(id)()
  }

  def sumPreisTotalGeplanteLieferungenVorher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime, startGeschaeftsjahr: DateTime)(implicit session: DBSession): Option[BigDecimal] = {
    sumPreisTotalGeplanteLieferungenVorherQuery(vertriebId, abotypId, datum, startGeschaeftsjahr).apply()
  }

  def getGeplanteLieferungVorher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime)(implicit session: DBSession): Option[Lieferung] = {
    getGeplanteLieferungVorherQuery(vertriebId, abotypId, datum).apply()
  }

  def getGeplanteLieferungNachher(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime)(implicit session: DBSession): Option[Lieferung] = {
    getGeplanteLieferungNachherQuery(vertriebId, abotypId, datum).apply()
  }

  def countEarlierLieferungOffen(id: LieferplanungId)(implicit session: DBSession): Option[Int] = {
    countEarlierLieferungOffenQuery(id).apply()
  }

  def getSammelbestellungen(id: LieferplanungId)(implicit session: DBSession): List[Sammelbestellung] = {
    getSammelbestellungenQuery(id)()
  }

  def getSammelbestellungen(id: LieferungId)(implicit session: DBSession): List[Sammelbestellung] = {
    getSammelbestellungenQuery(id)()
  }

  def getSammelbestellungenByProduzent(produzent: ProduzentId, lieferplanungId: LieferplanungId)(implicit session: DBSession): List[Sammelbestellung] = {
    getSammelbestellungenByProduzentQuery(produzent, lieferplanungId)()
  }
  def getBestellung(id: SammelbestellungId, adminProzente: BigDecimal)(implicit session: DBSession): Option[Bestellung] = {
    getBestellungQuery(id, adminProzente)()
  }

  def getBestellungen(id: SammelbestellungId)(implicit session: DBSession): List[Bestellung] = {
    getBestellungenQuery(id)()
  }

  def getBestellpositionen(id: BestellungId)(implicit session: DBSession): List[Bestellposition] = {
    getBestellpositionenQuery(id).apply()
  }

  def getBestellpositionenBySammelbestellung(id: SammelbestellungId)(implicit session: DBSession): List[Bestellposition] = {
    getBestellpositionenBySammelbestellungQuery(id).apply()
  }

  def getTourlieferungenByKunde(id: KundeId)(implicit session: DBSession): List[Tourlieferung] = {
    getTourlieferungenByKundeQuery(id).apply()
  }

  def getVertriebsarten(vertriebId: VertriebId)(implicit session: DBSession): List[VertriebsartDetail] = {
    getDepotlieferung(vertriebId) ++ getHeimlieferung(vertriebId) ++ getPostlieferung(vertriebId)
  }

  def getVertrieb(vertriebId: VertriebId)(implicit session: DBSession): Option[Vertrieb] = {
    getVertriebQuery(vertriebId).apply()
  }

  def getVertriebByDate(datum: DateTime)(implicit session: DBSession): List[Vertrieb] = {
    getVertriebByDateQuery(datum).apply()
  }

  def getVertriebe(abotypId: AbotypId)(implicit session: DBSession): List[VertriebVertriebsarten] = {
    getVertriebeQuery(abotypId).apply()
  }

  def getDepotlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[DepotlieferungDetail] = {
    getDepotlieferungQuery(vertriebId).apply()
  }

  def getDepotlieferung(depotId: DepotId)(implicit session: DBSession): List[Depotlieferung] = {
    getDepotlieferungQuery(depotId).apply()
  }

  def getHeimlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[HeimlieferungDetail] = {
    getHeimlieferungQuery(vertriebId).apply()
  }

  def getHeimlieferung(tourId: TourId)(implicit session: DBSession): List[Heimlieferung] = {
    getHeimlieferungQuery(tourId).apply()
  }

  def getPostlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[PostlieferungDetail] = {
    getPostlieferungQuery(vertriebId).apply()
  }

  def getDepotAuslieferung(depotId: DepotId, datum: DateTime)(implicit session: DBSession): Option[DepotAuslieferung] = {
    getDepotAuslieferungQuery(depotId, datum).apply()
  }

  def getTourAuslieferung(tourId: TourId, datum: DateTime)(implicit session: DBSession): Option[TourAuslieferung] = {
    getTourAuslieferungQuery(tourId, datum).apply()
  }

  def getPostAuslieferung(datum: DateTime)(implicit session: DBSession): Option[PostAuslieferung] = {
    getPostAuslieferungQuery(datum).apply()
  }

  def getDepotlieferungAbosByDepot(id: DepotId)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getDepotlieferungAbosByDepotQuery(id).apply()
  }

  def getTourlieferungen(id: TourId)(implicit session: DBSession): List[Tourlieferung] = {
    getTourlieferungenQuery(id).apply()
  }

  def getKundeDetail(kundeId: KundeId)(implicit session: DBSession): Option[KundeDetail] = {
    getKundeDetailQuery(kundeId).apply()
  }

  def getAbo(id: AboId)(implicit session: DBSession): Option[Abo] = {
    getSingleDepotlieferungAboQuery(id)() orElse getSingleHeimlieferungAboQuery(id)() orElse getSinglePostlieferungAboQuery(id)() orElse getSingleZusatzAboQuery(id)()
  }

  def getLieferungenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenByAbotypQuery(abotypId)()
  }

  def getLieferungenOffenOrAbgeschlossenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenOffenOrAbgeschlossenByAbotypQuery(abotypId)()
  }

  def getLieferungenOffenByAbotyp(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenOffenByAbotypQuery(abotypId)()
  }

  def getLieferungenOffenByVertrieb(vertriebId: VertriebId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenOffenByVertriebQuery(vertriebId)()
  }

  def getAbwesenheit(aboId: AboId, datum: DateTime)(implicit session: DBSession): List[Abwesenheit] = {
    getAbwesenheitQuery(aboId, datum)()
  }

  def getAbwesenheiten(aboId: AboId)(implicit session: DBSession): List[Abwesenheit] = {
    getAbwesenheitenQuery(aboId)()
  }

}
