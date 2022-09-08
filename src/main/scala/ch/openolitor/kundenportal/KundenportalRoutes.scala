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
package ch.openolitor.kundenportal

import akka.actor._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import ch.openolitor.arbeitseinsatz.{ ArbeitseinsatzCommandHandler, ArbeitseinsatzJsonProtocol }
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.core.Macros._
import ch.openolitor.core.filestore._
import ch.openolitor.core.security.Subject
import ch.openolitor.kundenportal.repositories.{ DefaultKundenportalReadRepositoryAsyncComponent, KundenportalReadRepositoryAsyncComponent }
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.util.parsing.{ FilterExpr, UriQueryParamFilterParser }

import scala.concurrent.ExecutionContext

trait KundenportalRoutes
  extends BaseRouteService
  with ActorReferences
  with AsyncConnectionPoolContextAware
  with StammdatenEventStoreSerializer
  with BuchhaltungJsonProtocol
  with ArbeitseinsatzJsonProtocol
  with StammdatenDBMappings {
  self: KundenportalReadRepositoryAsyncComponent with FileStoreComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val projektIdPath = long2BaseIdPathMatcher(ProjektId.apply)
  implicit val aboIdPath = long2BaseIdPathMatcher(AboId.apply)
  implicit val abotypIdPath = long2BaseIdPathMatcher(AbotypId.apply)
  implicit val vertriebIdPath = long2BaseIdPathMatcher(VertriebId.apply)
  implicit val zusatzabotypIdPath = long2BaseIdPathMatcher(AbotypId.apply)
  implicit val abwesenheitIdPath = long2BaseIdPathMatcher(AbwesenheitId.apply)
  implicit val lieferungIdPath = long2BaseIdPathMatcher(LieferungId.apply)
  implicit val arbeitsangebotIdPath = long2BaseIdPathMatcher(ArbeitsangebotId.apply)
  implicit val arbeitseinsatzIdPath = long2BaseIdPathMatcher(ArbeitseinsatzId.apply)
  implicit val personIdPath = long2BaseIdPathMatcher(PersonId.apply)

  import EntityStore._

  def kundenportalRoute(implicit subject: Subject) =
    parameter("f".?) { f =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      pathPrefix("kundenportal") {
        abosRoute ~ arbeitRoute ~ rechnungenRoute ~ projektRoute ~ kontoDatenRoute ~ personenRoute
      }
    }

  def projektRoute(implicit subject: Subject) = {
    path("projekt") {
      get {
        get(detail(kundenportalReadRepository.getProjekt))
      }
    } ~
      path("projekt" / projektIdPath / "logo") { id =>
        get(download(ProjektStammdaten, "logo"))
      }
  }

  def kontoDatenRoute(implicit subject: Subject) = {
    path("kontodaten") {
      get(detail(kundenportalReadRepository.getKontoDatenProjekt))
    }
  }

  def personenRoute(implicit subject: Subject): Route = {
    path("personContactVisibility" / personIdPath) { id =>
      post {
        onSuccess(entityStore ? ArbeitseinsatzCommandHandler.ChangeContactPermissionForUserCommand(subject.personId, subject, id)) {
          case UserCommandFailed =>
            complete(StatusCodes.BadRequest, s"error.")
          case _ =>
            complete("")
        }
      }
    }
  }

  def rechnungenRoute(implicit subject: Subject) = {
    path("rechnungen") {
      get {
        list(kundenportalReadRepository.getRechnungen)
      }
    } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get(detail(kundenportalReadRepository.getRechnungDetail(id)))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "downloadrechnung") { id =>
        (get)(
          onSuccess(kundenportalReadRepository.getRechnungDetail(id)) { detail =>
            detail flatMap { rechnung =>
              rechnung.fileStoreId map { fileStoreId =>
                download(GeneriertRechnung, fileStoreId)
              }
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "download" / Segment) { (id, fileStoreId) =>
        get {
          onSuccess(kundenportalReadRepository.getRechnungDetail(id)) { detail =>
            detail map { _ =>
              download(GeneriertMahnung, fileStoreId)
            } getOrElse complete(StatusCodes.BadRequest)
          }
        }
      }
  }

  def arbeitRoute(implicit subject: Subject, filter: Option[FilterExpr]) = {
    path("arbeitsangebote") {
      get {
        list(kundenportalReadRepository.getArbeitsangebote)
      } ~
        post {
          entity(as[ArbeitseinsatzCreate]) { arbein =>
            onSuccess(entityStore ? KundenportalCommandHandler.ArbeitseinsatzErstellenCommand(subject.personId, subject, arbein)) {
              case UserCommandFailed =>
                complete(StatusCodes.BadRequest, s"Arbeitseinsatz konnte nicht erstellt werden.")
              case _ =>
                complete("")
            }
          }
        }
    } ~
      path("arbeitseinsaetze") {
        get {
          list(kundenportalReadRepository.getArbeitseinsaetze)
        }
      } ~
      path("arbeitseinsaetze" / arbeitseinsatzIdPath) { arbeitseinsatzId =>
        post {
          entity(as[ArbeitseinsatzCreate]) { arbein =>
            onSuccess(entityStore ? KundenportalCommandHandler.ArbeitseinsatzModifizierenCommand(subject.personId, subject, arbeitseinsatzId, arbein)) {
              case UserCommandFailed =>
                complete(StatusCodes.BadRequest, s"Arbeitseinsatz konnte nicht modifiziert werden.")
              case _ =>
                complete("")
            }
          }
        } ~
          delete {
            onSuccess(entityStore ? KundenportalCommandHandler.ArbeitseinsatzLoeschenCommand(subject.personId, subject, arbeitseinsatzId)) {
              case UserCommandFailed =>
                complete(StatusCodes.BadRequest, s"Arbeitseinsatz konnte nicht gelöscht werden.")
              case _ =>
                complete("")
            }
          }
      }

  }

  def abosRoute(implicit subject: Subject, filter: Option[FilterExpr]) = {
    path("abos") {
      get {
        list(kundenportalReadRepository.getHauptabos)
      }
    } ~
      path("abos" / aboIdPath / "zusatzabos") { aboId =>
        get {
          list(kundenportalReadRepository.getZusatzAbosByHauptAbo(aboId))
        }
      } ~
      path("abos" / aboIdPath / "abwesenheiten") { aboId =>
        post {
          entity(as[AbwesenheitModify]) { abw =>
            onSuccess(entityStore ? KundenportalCommandHandler.AbwesenheitErstellenCommand(subject.personId, subject, copyTo[AbwesenheitModify, AbwesenheitCreate](abw, "aboId" -> aboId))) {
              case UserCommandFailed =>
                complete(StatusCodes.BadRequest, s"Abwesenheit konnte nicht erstellt werden.")
              case _ =>
                complete("")
            }
          }
        }
      } ~
      path("abos" / aboIdPath / "abwesenheiten" / abwesenheitIdPath) { (aboId, abwesenheitId) =>
        onSuccess(entityStore ? KundenportalCommandHandler.AbwesenheitLoeschenCommand(subject.personId, subject, aboId, abwesenheitId)) {
          case UserCommandFailed =>
            complete(StatusCodes.BadRequest, s"Abwesenheit konnte nicht gelöscht werden.")
          case _ =>
            complete("")
        }
      } ~
      path("abos" / abotypIdPath / "vertriebe" / vertriebIdPath / "lieferungen") { (abotypId, vertriebId) =>
        get {
          list(kundenportalReadRepository.getLieferungenDetails(abotypId, vertriebId))
        }
      } ~
      path("abos" / abotypIdPath / "vertriebe" / vertriebIdPath / "abo" / aboIdPath / "lieferungenMainAndAdditional") { (abotypId, vertriebId, aboId) =>
        get {
          list(kundenportalReadRepository.getLieferungenMainAndAdditionalDetails(abotypId, vertriebId, aboId))
        }
      } ~
      path("abos" / abotypIdPath / "vertriebe" / vertriebIdPath / "lieferungen" / lieferungIdPath) { (abotypId, vertriebId, lieferungId) =>
        get {
          get(detail(kundenportalReadRepository.getLieferungenDetail(lieferungId)))
        }
      }
  }
}

class DefaultKundenportalRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
  extends KundenportalRoutes
  with DefaultKundenportalReadRepositoryAsyncComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
