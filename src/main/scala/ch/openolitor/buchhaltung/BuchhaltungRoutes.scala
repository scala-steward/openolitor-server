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

import spray.routing._
import spray.http._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import akka.pattern.ask
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import stamina.Persister
import ch.openolitor.buchhaltung.models._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportParser
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportRecordResult
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.RechnungReportService
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.MahnungReportService
import java.io._
import java.io.ByteArrayInputStream

import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.Duration
import ch.openolitor.buchhaltung.rechnungsexport.iso20022._

import scala.concurrent.{ Await, Future }

trait BuchhaltungRoutes extends HttpService with ActorReferences
  with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
  with BuchhaltungJsonProtocol
  with BuchhaltungEventStoreSerializer
  with RechnungReportService
  with MahnungReportService
  with BuchhaltungDBMappings {
  self: BuchhaltungReadRepositoryAsyncComponent with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val rechnungsPositionIdPath = long2BaseIdPathMatcher(RechnungsPositionId.apply)
  implicit val zahlungsImportIdPath = long2BaseIdPathMatcher(ZahlungsImportId.apply)
  implicit val zahlungsEingangIdPath = long2BaseIdPathMatcher(ZahlungsEingangId.apply)
  implicit val zahlungsExportIdPath = long2BaseIdPathMatcher(ZahlungsExportId.apply)

  import EntityStore._

  def buchhaltungRoute(implicit subect: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      rechnungenRoute ~ rechnungspositionenRoute ~ zahlungsImportsRoute ~ mailingRoute ~ zahlungsExportsRoute
    }

  def rechnungenRoute(implicit subect: Subject, filter: Option[FilterExpr]) =
    path("rechnungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungen, exportFormat)) ~
        post(create[RechnungCreateFromRechnungsPositionen, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / "aktionen" / "downloadrechnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.fileStoreId.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download rechnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Rechnungen_" + System.currentTimeMillis + ".zip", GeneriertRechnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "downloadmahnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.mahnungFileStoreIds.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download mahnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Mahnungen_" + System.currentTimeMillis + ".zip", GeneriertMahnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "pain_008_001_07") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                generatePain008(rechnungen) match {
                  case Right(xmlData) => {
                    val bytes = xmlData.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    storeToFileStore(ZahlungsExportDaten, None, new ByteArrayInputStream(bytes), "pain_008_001_02") { (fileId, meta) =>
                      createZahlungExport(fileId, rechnungen, xmlData)
                    }
                  }
                  case Left(errorMessage) => {
                    logger.debug(s"Some data needs to be introduce in the system before creating the pain_008_001_02 : $errorMessage")
                    complete(StatusCodes.BadRequest, s"Some data needs to be introduce in the system before creating the pain_008_001_02 : $errorMessage")
                  }
                }
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "verschicken") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              verschicken(cont.ids)
            }
          }
        }
      } ~
      path("rechnungen" / "berichte" / "rechnungen") {
        (post)(rechnungBerichte())
      } ~
      path("rechnungen" / "berichte" / "mahnungen") {
        (post)(mahnungBerichte())
      } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get({
          detail(buchhaltungReadRepository.getRechnungDetail(id))
        }) ~
          delete(deleteRechnung(id)) ~
          (put | post)(entity(as[RechnungModify]) { entity => safeRechnung(id, entity) })
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "downloadrechnung") { id =>
        (get)(
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
            detail flatMap { rechnung =>
              rechnung.fileStoreId map { fileStoreId =>
                download(GeneriertRechnung, fileStoreId)
              }
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "download" / Segment) { (id, fileStoreId) =>
        (get)(
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
            detail map { rechnung =>
              download(GeneriertMahnung, fileStoreId)
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "verschicken") { id =>
        (post)(verschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "mahnungverschicken") { id =>
        (post)(mahnungVerschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "bezahlen") { id =>
        (post)(entity(as[RechnungModifyBezahlt]) { entity => bezahlen(id, entity) })
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "stornieren") { id =>
        (post)(stornieren(id))
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "rechnung") { id =>
        (post)(rechnungBericht(id))
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "mahnung") { id =>
        (post)(mahnungBericht(id))
      }

  def rechnungspositionenRoute(implicit subect: Subject, filter: Option[FilterExpr]) =
    path("rechnungspositionen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungsPositionen, exportFormat))
    } ~
      path("rechnungspositionen" / rechnungsPositionIdPath) { id =>
        delete(deleteRechnungsPosition(id)) ~
          (put | post)(entity(as[RechnungsPositionModify]) { entity => safeRechnungsPosition(id, entity) })
      } ~
      path("rechnungspositionen" / "aktionen" / "createrechnungen") {
        post {
          entity(as[RechnungsPositionenCreateRechnungen]) { rechnungenCreate =>
            createRechnungen(rechnungenCreate)
          }
        }
      }

  def zahlungsImportsRoute(implicit subect: Subject) =
    path("zahlungsimports") {
      get(list(buchhaltungReadRepository.getZahlungsImports)) ~
        (put | post)(upload { (form, content, fileName) =>
          // read the file once and pass the same content along
          val uploadData = Iterator continually content.read takeWhile (-1 !=) map (_.toByte) toArray

          ZahlungsImportParser.parse(uploadData) match {
            case Success(importResult) =>
              storeToFileStore(ZahlungsImportDaten, None, new ByteArrayInputStream(uploadData), fileName) { (fileId, meta) =>
                createZahlungsImport(fileId, importResult.records)
              }
            case Failure(e) => complete(StatusCodes.BadRequest, s"Die Datei konnte nicht gelesen werden: $e")
          }
        })
    } ~
      path("zahlungsimports" / zahlungsImportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsImportDetail(id)))
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / zahlungsEingangIdPath / "aktionen" / "erledigen") { (_, zahlungsEingangId) =>
        post(entity(as[ZahlungsEingangModifyErledigt]) { entity => zahlungsEingangErledigen(entity) })
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / "aktionen" / "automatischerledigen") { id =>
        post(entity(as[Seq[ZahlungsEingangModifyErledigt]]) { entities => zahlungsEingaengeErledigen(entities) })
      }

  private def mailingRoute(implicit subject: Subject): Route =
    path("mailing" / "sendEmailToInvoicesSubscribers") {
      post {
        requestInstance { request =>
          entity(as[RechnungMailRequest]) { rechnungMailRequest =>
            sendEmailsToInvoicesSubscribers(rechnungMailRequest.subject, rechnungMailRequest.body, rechnungMailRequest.replyTo, rechnungMailRequest.ids, rechnungMailRequest.attachInvoice)
          }
        }
      }
    }
  def zahlungsExportsRoute(implicit subect: Subject) =
    path("zahlungsexports") {
      get(list(buchhaltungReadRepository.getZahlungsExports))
    } ~
      path("zahlungsexports" / zahlungsExportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsExportDetail(id))) ~
          (put | post)(update[ZahlungsExportCreate, ZahlungsExportId](id))
      } ~
      path("zahlungsexports" / zahlungsExportIdPath / "download") { id =>
        get {
          onSuccess(buchhaltungReadRepository.getZahlungsExportDetail(id)) {
            case Some(zahlungExport) =>
              download(ZahlungsExportDaten, zahlungExport.fileName)
            case None =>
              complete(StatusCodes.NotFound, s"zahlung export nicht gefunden: $id")
          }
        }
      }

  def verschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def verschicken(ids: Seq[RechnungId])(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungenVerschickenCommand(subject.personId, ids)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten keine Rechnungen in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def mahnungVerschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungMahnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'MahnungVerschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def bezahlen(id: RechnungId, entity: RechnungModifyBezahlt)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungBezahlenCommand(subject.personId, id, entity)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def stornieren(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungStornierenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Storniert' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def createZahlungsImport(file: String, zahlungsEingaenge: Seq[ZahlungsImportRecordResult])(implicit idPersister: Persister[ZahlungsImportId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsImportCreateCommand(subject.personId, file, zahlungsEingaenge))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingangErledigen(entity: ZahlungsEingangModifyErledigt)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingangErledigenCommand(subject.personId, entity))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Der Zahlungseingang konnte nicht erledigt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingaengeErledigen(entities: Seq[ZahlungsEingangModifyErledigt])(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingaengeErledigenCommand(subject.personId, entities))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Zahlungseingänge erledigt werden")
      case _ =>
        complete("")
    }
  }

  def rechnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateRechnungReports _)(RechnungId.apply)
  }

  def rechnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateRechnungReports _)(RechnungId.apply)
  }

  def mahnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateMahnungReports _)(RechnungId.apply)
  }

  def mahnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateMahnungReports _)(RechnungId.apply)
  }

  def createRechnungen(rechnungenCreate: RechnungsPositionenCreateRechnungen)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.CreateRechnungenCommand(subject.personId, rechnungenCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen RechnungsPositionen erstellt werden.")
      case _ =>
        complete("")
    }
  }

  def createZahlungExport(file: String, rechnungen: List[Rechnung], fileContent: String)(implicit subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.ZahlungsExportCreateCommand(subject.personId, rechnungen, file)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"The file could not be exported. Make sure all the invoices have an Iban and a account holder name. The CSA needs also to have a valid Iban and Creditor Identifier")
      case _ => complete(fileContent)
    }
  }

  def deleteRechnung(rechnungId: RechnungId)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.DeleteRechnungCommand(subject.personId, rechnungId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnung kann nur gelöscht werden wenn sie im Status Erstellt ist.")
      case _ =>
        complete("")
    }
  }

  def safeRechnung(rechnungId: RechnungId, rechnungModify: RechnungModify)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SafeRechnungCommand(subject.personId, rechnungId, rechnungModify))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnung kann nur gespeichert werden wenn sie im Status Erstellt ist und keine Rechnungspositionen hat.")
      case _ =>
        complete("")
    }
  }

  def deleteRechnungsPosition(rechnungsPositionId: RechnungsPositionId)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.DeleteRechnungsPositionCommand(subject.personId, rechnungsPositionId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnungsposition kann nur gelöscht werden wenn sie im Status Offen ist.")
      case _ =>
        complete("")
    }
  }

  def safeRechnungsPosition(rechnungsPositionId: RechnungsPositionId, rechnungsPositionModify: RechnungsPositionModify)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SafeRechnungsPositionCommand(subject.personId, rechnungsPositionId, rechnungsPositionModify))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnungsposition kann nur gespeichert werden wenn sie im Status Offen ist.")
      case _ =>
        complete("")
    }
  }

  private def sendEmailsToInvoicesSubscribers(emailSubject: String, body: String, replyTo: Option[String], ids: Seq[RechnungId], attachInvoice: Boolean)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SendEmailToInvoicesSubscribersCommand(subject.personId, emailSubject, body, replyTo, ids, attachInvoice))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Something went wrong with the mail generation, please check the correctness of the template.")
      case _ =>
        complete("")
    }
  }

  def generatePain008(ids: List[Rechnung])(implicit subect: Subject): Either[String, String] = {
    val NbOfTxs = ids.size.toString

    val rechnungenWithFutures: Future[List[(Rechnung, KontoDaten)]] = Future.sequence(ids.map { rechnung =>
      stammdatenReadRepository.getKontoDatenKunde(rechnung.kundeId).map { k => (rechnung, k.get) }
    })
    val d = Duration(1, SECONDS)

    val rechnungen: List[(Rechnung, KontoDaten)] = Await.result(rechnungenWithFutures, d)
    val kontoDatenProjekt: KontoDaten = Await.result(stammdatenReadRepository.getKontoDatenProjekt, d).get
    val projekt: Projekt = Await.result(stammdatenReadRepository.getProjekt, d).get

    (kontoDatenProjekt.iban, kontoDatenProjekt.creditorIdentifier) match {
      case (Some(""), Some(_)) => { Left(s"The iban is not defined for the project") }
      case (Some(_), Some("")) => { Left("The creditorIdentifier is not defined for the project ") }
      case (Some(iban), Some(creditorIdentifier)) => {
        val emptyIbanList = checkEmptyIban(rechnungen)
        if (emptyIbanList.isEmpty) {
          val xmlText = Pain008_001_02_Export.exportPain008_001_02(rechnungen, kontoDatenProjekt, NbOfTxs, projekt)
          Right(xmlText)
        } else {
          val decoratedEmptyList = emptyIbanList.mkString(" ")
          Left(s"The iban or name account holder is not defined for the user: $decoratedEmptyList")
        }
      }
      case (None, Some(creditorIdentifier)) => {
        Left(s"The iban is not defined for the project")
      }
      case (Some(iban), None) => {
        Left("The creditorIdentifier is not defined for the project ")
      }
      case (None, None) => {
        Left("Neither the creditorIdentifier nor the iban is defined for the project")
      }
    }
  }

  def checkEmptyIban(rechnungen: List[(Rechnung, KontoDaten)])(implicit subect: Subject): List[KundeId] = {
    rechnungen flatMap { rechnung =>
      (rechnung._2.iban, rechnung._2.nameAccountHolder) match {
        case (None, _)          => Some(rechnung._1.kundeId)
        case (_, None)          => Some(rechnung._1.kundeId)
        case (Some(_), Some(_)) => None
      }
    }
  }
}

class DefaultBuchhaltungRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
  extends BuchhaltungRoutes
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with DefaultStammdatenReadRepositoryAsyncComponent
