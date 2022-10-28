package ch.openolitor.stammdaten

import akka.http.scaladsl.model.StatusCodes
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models.{ Projekt, ProjektModify }
import ch.openolitor.core.Macros._
import ch.openolitor.core.models.EntityModified

import scala.concurrent.Await

class StammdatenRoutesProjektSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  sequential

  private val service = new MockStammdatenRoutes(sysConfig, system)
  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Projekt" should {
    "return Projekt" in {
      Get("/projekt") ~> service.stammdatenRoute ~> check {
        val response = responseAs[Option[Projekt]]
        status === StatusCodes.OK
        response.headOption.map { head =>
          head.bezeichnung === "Demo Projekt"
        }
        response must beSome
      }
    }

    "update Projekt" in {
      val projekt = Await.result(service.stammdatenReadRepository.getProjekt, defaultTimeout).get
      val update = copyTo[Projekt, ProjektModify](projekt).copy(bezeichnung = "Updated")

      val probe = dbEventProbe()

      Post(s"/projekt/${projekt.id.id}", update) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Accepted

        // wait for modification to happen
        probe.expectMsgType[EntityModified[Projekt]]

        val updated = Await.result(service.stammdatenReadRepository.getProjekt, defaultTimeout).get
        updated.bezeichnung === "Updated"
      }
    }
  }
}
