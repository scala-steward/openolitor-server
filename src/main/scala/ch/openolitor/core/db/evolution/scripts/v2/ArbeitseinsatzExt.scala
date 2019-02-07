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
package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._
import scala.util.{ Success, Try }
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import scala.collection.Seq
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings

object ArbeitseinsatzExt {
  val arbeitseinsatzScripts = new Script with LazyLogging with ArbeitseinsatzDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(arbeitseinsatzMapping, "email", "VARCHAR(100)", "bemerkungen")
      alterTableAddColumnIfNotExists(arbeitseinsatzMapping, "telefon_mobil", "VARCHAR(50)", "email")

      Success(true)

    }
  }
  val scripts = Seq(arbeitseinsatzScripts)
}
