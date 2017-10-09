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
package ch.openolitor.core.mailtemplates

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.security.Subject
import ch.openolitor.core.mailtemplates.repositories.MailTemplateReadRepositoryComponent
import ch.openolitor.core.mailtemplates.model._
import ch.openolitor.core.mailtemplates.repositories.MailTemplateDBMappings
import ch.openolitor.core.mailtemplates.eventsourcing._

trait MailTemplateRoutes extends HttpService
    with ActorReferences
    with AsyncConnectionPoolContextAware
    with SprayDeserializers
    with DefaultRouteService
    with LazyLogging
    with MailTemplateJsonProtocol
    with MailTemplateDBMappings
    with MailTemplateEventStoreSerializer {
  self: MailTemplateReadRepositoryComponent =>

  implicit val mailTemplateIdPath = long2BaseIdPathMatcher(MailTemplateId.apply)

  def mailTemplateRoute(implicit subject: Subject) =
    path("mailtemplatetypes") {
      get {
        complete(MailTemplateType.AllTemplateTypes.map(_.asInstanceOf[MailTemplateType]))
      }
    } ~
      path("mailtemplates") {
        get(list(mailTemplateReadRepositoryAsync.getMailTemplates())) ~
          post(create[MailTemplateModify, MailTemplateId](MailTemplateId.apply _))
      } ~
      path("mailtemplates" / mailTemplateIdPath) { mailTemplateId =>
        get(detail(mailTemplateReadRepositoryAsync.getById(mailTemplateMapping, mailTemplateId))) ~
          (put | post)(update[MailTemplateModify, MailTemplateId](mailTemplateId))
      } // ~
  //      path("mailtemplates" / mailTemplateIdPath / "upload") { mailTemplateId =>
  //       
  //      }
}