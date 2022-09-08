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
package ch.openolitor.buchhaltung

import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.models.{ KundeId, Person, PersonEmailData }
import ch.openolitor.core.models._
import ch.openolitor.mailtemplates.engine.MailTemplateService
import ch.openolitor.core.Macros._

import scala.util._
import scalikejdbc.DB
import scalikejdbc.DBSession
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.exceptions.InvalidStateException
import akka.actor.ActorSystem
import ch.openolitor.core._
import ch.openolitor.core.filestore.FileTypeFilenameMapping
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.buchhaltung.zahlungsimport.{ ZahlungsImportRecord, ZahlungsImportRecordResult }
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositorySyncComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositorySyncComponent
import ch.openolitor.core.Macros.copyTo

import scala.concurrent.ExecutionContext.Implicits.global

object BuchhaltungCommandHandler {
  case class RechnungVerschickenCommand(originator: PersonId, id: RechnungId) extends UserCommand
  case class RechnungenVerschickenCommand(originator: PersonId, ids: Seq[RechnungId]) extends UserCommand
  case class RechnungMahnungVerschickenCommand(originator: PersonId, id: RechnungId) extends UserCommand
  case class RechnungBezahlenCommand(originator: PersonId, id: RechnungId, entity: RechnungModifyBezahlt) extends UserCommand
  case class DeleteRechnungCommand(originator: PersonId, id: RechnungId) extends UserCommand
  case class SafeRechnungCommand(originator: PersonId, id: RechnungId, entiy: RechnungModify) extends UserCommand
  case class RechnungStornierenCommand(originator: PersonId, id: RechnungId) extends UserCommand

  case class RechnungPDFStoredCommand(originator: PersonId, id: RechnungId, fileStoreId: String) extends UserCommand
  case class MahnungPDFStoredCommand(originator: PersonId, id: RechnungId, fileStoreId: String) extends UserCommand

  case class RechnungVerschicktEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class RechnungMahnungVerschicktEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class RechnungBezahltEvent(meta: EventMetadata, id: RechnungId, entity: RechnungModifyBezahlt) extends PersistentEvent with JSONSerializable
  case class RechnungStorniertEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class RechnungDeleteEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class CreateRechnungenCommand(originator: PersonId, rechnungsPositionenCreateRechnungen: RechnungsPositionenCreateRechnungen) extends UserCommand
  case class CreateRechnungenEvent(originator: PersonId, createRechnungen: RechnungsPositionenCreateRechnungen) extends UserCommand
  case class DeleteRechnungsPositionCommand(originator: PersonId, rechnungsPositionId: RechnungsPositionId) extends UserCommand
  case class SafeRechnungsPositionCommand(originator: PersonId, rechnungsPositionId: RechnungsPositionId, entity: RechnungsPositionModify) extends UserCommand

  case class ZahlungsImportCreateCommand(originator: PersonId, file: String, zahlungsEingaenge: Seq[ZahlungsImportRecordResult]) extends UserCommand
  case class ZahlungsEingangErledigenCommand(originator: PersonId, entity: ZahlungsEingangModifyErledigt) extends UserCommand
  case class ZahlungsEingaengeErledigenCommand(originator: PersonId, entities: Seq[ZahlungsEingangModifyErledigt]) extends UserCommand
  case class ZahlungsEingangIgnoreCommand(originator: PersonId, entity: ZahlungsEingangModifyErledigt) extends UserCommand

  case class SendEmailToInvoicesSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[RechnungId], invoice: Boolean) extends UserCommand

  case class ZahlungsImportCreatedEvent(meta: EventMetadata, entity: ZahlungsImportCreate) extends PersistentEvent with JSONSerializable
  case class ZahlungsEingangErledigtEvent(meta: EventMetadata, entity: ZahlungsEingangModifyErledigt) extends PersistentEvent with JSONSerializable
  case class ZahlungsEingangIgnoreEvent(meta: EventMetadata, entity: ZahlungsEingangModifyErledigt) extends PersistentEvent with JSONSerializable

  case class ZahlungsExportCreateCommand(originator: PersonId, rechnungen: List[Rechnung], file: String) extends UserCommand
  case class ZahlungsExportCreatedEvent(meta: EventMetadata, entity: ZahlungsExportCreate) extends PersistentEvent with JSONSerializable

  case class RechnungPDFStoredEvent(meta: EventMetadata, id: RechnungId, fileStoreId: String) extends PersistentEvent with JSONSerializable
  case class MahnungPDFStoredEvent(meta: EventMetadata, id: RechnungId, fileStoreId: String) extends PersistentEvent with JSONSerializable
  case class SendEmailToInvoiceSubscribersEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], invoiceReference: Option[String], context: RechnungMailContext) extends PersistentGeneratedEvent with JSONSerializable
}

trait BuchhaltungCommandHandler extends CommandHandler with BuchhaltungDBMappings with ConnectionPoolContextAware with AsyncConnectionPoolContextAware
  with FileTypeFilenameMapping
  with MailTemplateService {
  self: BuchhaltungReadRepositorySyncComponent =>
  import BuchhaltungCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    case RechnungVerschickenCommand(personId, id: RechnungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Erstellt =>
              Success(Seq(DefaultResultingEvent(factory => RechnungVerschicktEvent(factory.newMetadata(), id))))
            case _ =>
              Failure(new InvalidStateException("Eine Rechnung kann nur im Status 'Erstellt' verschickt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungenVerschickenCommand(personId, ids: Seq[RechnungId]) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getByIds(rechnungMapping, ids) filter (_.status == Erstellt) match {
          case Seq() => Failure(new InvalidStateException("Keine Rechnung im Status 'Erstellt' selektiert"))
          case validatedRechnungen =>
            Success(validatedRechnungen.map(r => DefaultResultingEvent(factory => RechnungVerschicktEvent(factory.newMetadata(), r.id))))
        }
      }

    case RechnungMahnungVerschickenCommand(personId, id: RechnungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Verschickt =>
              Success(Seq(DefaultResultingEvent(factory => RechnungMahnungVerschicktEvent(factory.newMetadata(), id))))
            case _ =>
              Failure(new InvalidStateException("Eine Mahnung kann nur im Status 'Verschickt' verschickt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungBezahlenCommand(personId, id: RechnungId, entity: RechnungModifyBezahlt) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Verschickt | MahnungVerschickt =>
              Success(Seq(DefaultResultingEvent(factory => RechnungBezahltEvent(factory.newMetadata(), id, entity))))
            case _ =>
              Failure(new InvalidStateException("Eine Rechnung kann nur im Status 'Verschickt' oder 'MahnungVerschickt' bezahlt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungStornierenCommand(personId, id: RechnungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Bezahlt =>
              Failure(new InvalidStateException("Eine Rechnung im Status 'Bezahlt' kann nicht mehr storniert werden"))
            case _ =>
              Success(Seq(DefaultResultingEvent(factory => RechnungStorniertEvent(factory.newMetadata(), id))))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case DeleteRechnungCommand(personId, rechnungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(rechnungMapping, rechnungId) map { rechnung =>
          if (rechnung.status == Erstellt) {
            Success(Seq(DefaultResultingEvent(factory => RechnungDeleteEvent(factory.newMetadata(), rechnungId))))
          } else {
            Failure(new InvalidStateException(s"Die Rechnung Nr. $rechnungId muss im State Erstellt sein"))
          }
        }
      } getOrElse Failure(new InvalidStateException(s"Kein Rechnung mit id $rechnungId gefunden"))

    case SafeRechnungCommand(personId, rechnungId, rechnungModify) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getRechnungDetail(rechnungId) map { rechnung =>
          if (rechnung.status == Erstellt && (rechnung.betrag == rechnungModify.betrag || rechnung.rechnungsPositionen.length == 0)) {
            Success(Seq(EntityUpdateEvent(rechnungId, rechnungModify)))
          } else {
            Failure(new InvalidStateException(s"Die Rechnung Nr. $rechnungId muss im State Erstellt sein"))
          }
        }
      } getOrElse Failure(new InvalidStateException(s"Kein Rechnung mit id $rechnungId gefunden"))

    case ZahlungsImportCreateCommand(personId, file, zahlungsEingaengeRecords) => idFactory => meta =>
      val id = idFactory.newId(ZahlungsImportId.apply)
      val zahlungsEingaenge = zahlungsEingaengeRecords collect {
        // ignoring total records for now
        case result: ZahlungsImportRecord =>
          val zahlungsEingangId = idFactory.newId(ZahlungsEingangId.apply)

          ZahlungsEingangCreate(
            zahlungsEingangId,
            id,
            None,
            None,
            None,
            result.transaktionsart.toString,
            result.teilnehmerNummer,
            result.iban,
            result.debitor,
            result.referenzNummer,
            result.waehrung,
            result.betrag,
            result.aufgabeDatum,
            result.verarbeitungsDatum,
            result.gutschriftsDatum,
            Ok
          )
      }

      Success(Seq(DefaultResultingEvent(factory => ZahlungsImportCreatedEvent(factory.newMetadata(), ZahlungsImportCreate(id, file, zahlungsEingaenge)))))

    case ZahlungsExportCreateCommand(personId, rechnungen, file) => idFactory => meta =>
      DB readOnly { implicit session =>
        val NbOfTxs = rechnungen.size.toString

        val id = idFactory.newId(ZahlungsExportId.apply)
        val rechnungIdList = rechnungen.map { rechnung =>
          rechnung.id
        }
        val zahlungsExportEvent = DefaultResultingEvent(factory => ZahlungsExportCreatedEvent(factory.newMetadata(), ZahlungsExportCreate(id, file, rechnungIdList.toSet, Created)))
        val verschicktEvents = rechnungIdList.map(r => DefaultResultingEvent(factory => RechnungVerschicktEvent(factory.newMetadata(), r)))
        Success(zahlungsExportEvent :: verschicktEvents)
      }

    case ZahlungsEingangErledigenCommand(personId, entity) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(zahlungsEingangMapping, entity.id) map { eingang =>
          if (!eingang.erledigt) {
            Success(Seq(DefaultResultingEvent(factory => ZahlungsEingangErledigtEvent(factory.newMetadata(), entity))))
          } else {
            Success(Seq())
          }
        } getOrElse (Failure(new InvalidStateException(s"Kein Zahlungseingang mit der Nr. ${entity.id} gefunden")))
      }

    case ZahlungsEingangIgnoreCommand(personId, entity) => idFactory => meta =>
      DB readOnly { implicit session =>
        buchhaltungReadRepository.getById(zahlungsEingangMapping, entity.id) map { eingang =>
          if (!eingang.erledigt) {
            Success(Seq(DefaultResultingEvent(factory => ZahlungsEingangIgnoreEvent(factory.newMetadata(), entity))))
          } else {
            Success(Seq())
          }
        } getOrElse (Failure(new InvalidStateException(s"Kein Zahlungseingang mit der Nr. ${entity.id} gefunden")))
      }

    case ZahlungsEingaengeErledigenCommand(userId, entities) => idFactory => meta =>
      val (successfuls, failures) = entities map { entity =>
        handle(ZahlungsEingangErledigenCommand(userId, entity))(idFactory)(meta)
      } partition (_.isSuccess)

      if (successfuls.isEmpty) {
        Failure(new InvalidStateException(s"Keiner der Zahlungseingänge konnte abgearbeitet werden"))
      } else {
        Success(successfuls flatMap (_.get))
      }

    case SendEmailToInvoicesSubscribersCommand(personId, subject, body, replyTo, ids, attachInvoice) => idFactory => meta =>
      DB readOnly { implicit session =>
        if (checkTemplateInvoice(body, subject, ids)) {
          val events = ids flatMap { rechnungId: RechnungId =>
            buchhaltungReadRepository.getById(rechnungMapping, rechnungId) map { rechnung =>
              buchhaltungReadRepository.getPerson(rechnung.id) map { person =>
                val personEmailData = copyTo[Person, PersonEmailData](person)
                val mailContext = RechnungMailContext(personEmailData, rechnung)
                if (attachInvoice) {
                  DefaultResultingEvent(factory => SendEmailToInvoiceSubscribersEvent(factory.newMetadata(), subject, body, replyTo, rechnung.fileStoreId, mailContext))
                } else {
                  DefaultResultingEvent(factory => SendEmailToInvoiceSubscribersEvent(factory.newMetadata(), subject, body, replyTo, None, mailContext))
                }
              }
            }
          }
          Success(events.flatten)
        } else {
          Failure(new InvalidStateException("The template is not valid"))
        }
      }

    case RechnungPDFStoredCommand(personId, id, fileStoreId) => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => RechnungPDFStoredEvent(factory.newMetadata(), id, fileStoreId))))

    case MahnungPDFStoredCommand(personId, id, fileStoreId) => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => MahnungPDFStoredEvent(factory.newMetadata(), id, fileStoreId))))

    case CreateRechnungenCommand(personeId, rechnungsPositionenCreateRechnungen) => idFactory => meta =>
      DB readOnly { implicit session =>

        val offeneRechnungsPositionenByKunde: Map[KundeId, Seq[RechnungsPosition]] = rechnungsPositionenCreateRechnungen.ids.map { id =>
          buchhaltungReadRepository.getById(rechnungsPositionMapping, id)
        }.flatten.filter(_.status == RechnungsPositionStatus.Offen).groupBy { _.kundeId }

        val createRechnungen: Seq[Seq[ResultingEvent]] =
          for {
            (kundeId, rechnungsPositionen) <- offeneRechnungsPositionenByKunde.toSeq
            kunde <- buchhaltungReadRepository.getById(kundeMapping, kundeId)
          } yield {
            val rechnungCreate = EntityInsertEvent(
              idFactory.newId(RechnungId.apply),
              RechnungCreateFromRechnungsPositionen(
                kundeId,
                rechnungsPositionenCreateRechnungen.titel,
                rechnungsPositionen.head.waehrung,
                rechnungsPositionen.map(_.betrag).sum,
                rechnungsPositionenCreateRechnungen.rechnungsDatum,
                rechnungsPositionenCreateRechnungen.faelligkeitsDatum,
                None,
                kunde.strasse,
                kunde.hausNummer,
                kunde.adressZusatz,
                kunde.plz,
                kunde.ort,
                kunde.paymentType
              )
            )

            // create an ordering of the form: parent sort 100, sub sort 101, 102, parent sort 200, subsort 201
            val (zusatzabos, abos) = rechnungsPositionen sortBy (_.id.id) partition (_.parentRechnungsPositionId.isDefined)

            val aboIds = abos map (_.id)
            val zusatzabosByAbo: Map[RechnungsPositionId, Seq[RechnungsPositionId]] =
              zusatzabos.map(z => (z.id, z.parentRechnungsPositionId.get)).groupBy(_._2).view.mapValues(_.map(_._1)).toMap

            val abosWithSort = Range(100, Int.MaxValue, 100) zip (aboIds)
            val allAbosWithSort = abosWithSort.flatMap {
              case (sort, aboId) =>
                (sort, aboId) +: (Range(sort + 1, Int.MaxValue) zip (zusatzabosByAbo getOrElse (aboId, List())))
            }
            val zusatzaboOrphansByAbo = zusatzabosByAbo.filterNot { case (p, c) => aboIds.contains(p.id) }
            val latestUntilNow = abosWithSort.lastOption.map(_._1).getOrElse(100)
            val allAbosAlsoOrphansWithSort: Iterable[(Int, RechnungsPositionId)] = {
              Range(latestUntilNow + 100, Int.MaxValue, 100) zip (zusatzaboOrphansByAbo) flatMap {
                case (sort, (_, children)) => (Range(sort + 1, Int.MaxValue) zip (children))
              }
            } ++ allAbosWithSort

            val assignRechnungsPositionen = allAbosAlsoOrphansWithSort.map {
              case (sort, aboId) =>
                rechnungsPositionAssignmentUpdateEvent(rechnungCreate.id, aboId, sort)
            }

            Seq(rechnungCreate) ++ assignRechnungsPositionen
          }

        Success(createRechnungen.flatten)
      }

    case DeleteRechnungsPositionCommand(personId, rechnungsPositionId) => idFactory => meta =>
      DB readOnly { implicit session =>
        val rechnungsPosition = buchhaltungReadRepository.getById(rechnungsPositionMapping, rechnungsPositionId)
        rechnungsPosition.map { rp =>
          if (rp.status == RechnungsPositionStatus.Offen) {
            Success(Seq(EntityDeleteEvent(rechnungsPositionId)))
          } else {
            Failure(new InvalidStateException(s"Die Rechnungsposition Nr. $rechnungsPositionId muss im State Offen sein"))
          }
        }
      } getOrElse Failure(new InvalidStateException(s"Kein Rechnungsposition mit id $rechnungsPositionId gefunden"))

    case SafeRechnungsPositionCommand(personId, rechnungsPositionId, modify) => idFactory => meta =>
      DB readOnly { implicit session =>
        val rechnungsPosition = buchhaltungReadRepository.getById(rechnungsPositionMapping, rechnungsPositionId)
        rechnungsPosition.map { rp =>
          if (rp.status == RechnungsPositionStatus.Offen) {
            Success(Seq(EntityUpdateEvent(rechnungsPositionId, modify)))
          } else {
            Failure(new InvalidStateException(s"Die Rechnungsposition Nr. $rechnungsPositionId muss im State Offen sein"))
          }
        }
      } getOrElse Failure(new InvalidStateException(s"Kein Rechnungsposition mit id $rechnungsPositionId gefunden"))

    /*
       * Insert command handling
       */
    case e @ InsertEntityCommand(personId, entity: RechnungCreateFromRechnungsPositionen) => idFactory => meta =>
      handleEntityInsert[RechnungCreateFromRechnungsPositionen, RechnungId](idFactory, meta, entity, RechnungId.apply)
  }

  private def rechnungsPositionAssignmentUpdateEvent(rechnungId: RechnungId, rechnungsPositionId: RechnungsPositionId, index: Int) = {
    EntityUpdateEvent(
      rechnungsPositionId,
      RechnungsPositionAssignToRechnung(rechnungId, index)
    )
  }

  private def checkTemplateInvoice(body: String, subject: String, ids: Seq[RechnungId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { rechnungId: RechnungId =>
      buchhaltungReadRepository.getPerson(rechnungId) flatMap { person =>
        buchhaltungReadRepository.getById(rechnungMapping, rechnungId) map { rechnung =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = RechnungMailContext(personEmailData, rechnung)
          generateMail(subject, body, mailContext).isSuccess
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }
}

class DefaultBuchhaltungCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BuchhaltungCommandHandler
  with DefaultBuchhaltungReadRepositorySyncComponent {
}
