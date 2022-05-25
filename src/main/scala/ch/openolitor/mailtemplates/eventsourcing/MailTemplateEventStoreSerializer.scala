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
package ch.openolitor.mailtemplates.eventsourcing

import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.eventsourcing.CoreEventStoreSerializer
import ch.openolitor.mailtemplates._
import ch.openolitor.mailtemplates.model._
import stamina.json._

trait MailTemplateEventStoreSerializer extends MailTemplateJsonProtocol with EntityStoreJsonProtocol with CoreEventStoreSerializer {
  //V1 persisters
  implicit val mailTemplateModifyPersister = persister[MailTemplateModify]("mail-template-modify")
  implicit val mailTemplateIdPersister = persister[MailTemplateId]("mail-template-id")

  val mailTemplatePersisters = List(
    mailTemplateModifyPersister,
    mailTemplateIdPersister
  )
}
