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
package ch.openolitor.core.domain

import ch.openolitor.buchhaltung.DefaultBuchhaltungCommandHandler
import ch.openolitor.core.SystemConfig
import ch.openolitor.kundenportal.DefaultKundenportalCommandHandler
import ch.openolitor.arbeitseinsatz.DefaultArbeitseinsatzCommandHandler
import ch.openolitor.stammdaten.DefaultStammdatenCommandHandler
import ch.openolitor.reports.DefaultReportsCommandHandler
import org.apache.pekko.actor.ActorSystem
import ch.openolitor.mailtemplates.{ DefaultMailTemplateCommandHanlder }

trait CommandHandlerComponent {
  val stammdatenCommandHandler: CommandHandler
  val buchhaltungCommandHandler: CommandHandler
  val arbeitseinsatzCommandHandler: CommandHandler
  val reportsCommandHandler: CommandHandler
  val kundenportalCommandHandler: CommandHandler
  val mailTemplateCommandHandler: CommandHandler
  val baseCommandHandler: CommandHandler
}

trait DefaultCommandHandlerComponent extends CommandHandlerComponent {
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val stammdatenCommandHandler = new DefaultStammdatenCommandHandler(sysConfig, system)
  override val buchhaltungCommandHandler = new DefaultBuchhaltungCommandHandler(sysConfig, system)
  override val arbeitseinsatzCommandHandler = new DefaultArbeitseinsatzCommandHandler(sysConfig, system)
  override val reportsCommandHandler = new DefaultReportsCommandHandler(sysConfig, system)
  override val mailTemplateCommandHandler: CommandHandler = new DefaultMailTemplateCommandHanlder(sysConfig, system)
  override val kundenportalCommandHandler = new DefaultKundenportalCommandHandler(sysConfig, system)
  override val baseCommandHandler = new BaseCommandHandler()
}
