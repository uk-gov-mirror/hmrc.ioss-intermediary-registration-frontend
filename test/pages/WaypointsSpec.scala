/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pages

import models.{Index, NormalMode}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable

import scala.collection.immutable.Seq

class WaypointsSpec extends AnyFreeSpec with Matchers with OptionValues with EitherValues {

  "EmptyWaypoints" - {

    ".setNextWaypoint" - {

      "must return Waypoints with this waypoint at the head" in new Fixture {

        EmptyWaypoints.setNextWaypoint(testWaypoint1) mustEqual Waypoints(List(testWaypoint1))

      }
    }
    }
}

private class Fixture {

  object RegularPage1 extends Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  object RegularPage2 extends Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  object CheckAnswersPage1 extends CheckAnswersPage {
    override val urlFragment: String = "check-1"

    override def route(waypoints: Waypoints): Call = Call("", "")

    override def isTheSamePage(other: Page): Boolean = other match {
      case CheckAnswersPage1 => true
      case _ => false
    }
  }

  object CheckAnswersPage2 extends CheckAnswersPage {
    override val urlFragment: String = "check-2"

    override def route(waypoints: Waypoints): Call = Call("", "")

    override def isTheSamePage(other: Page): Boolean = other match {
      case CheckAnswersPage2 => true
      case _ => false
    }
  }

  object Section1 extends AddToListSection

  object Section2 extends AddToListSection

  case class AddItemPage1(override val index: Option[Index] = None) extends AddItemPage(index) {
    override val normalModeUrlFragment: String = "add-page-1"
    override val checkModeUrlFragment: String = "change-page-1"

    override def route(waypoints: Waypoints): Call = Call("", "")

    override def path: JsPath = JsPath \ "addItemPage1"

    override def isTheSamePage(other: Page): Boolean = other match {
      case _: AddItemPage1 => true
      case _ => false
    }

    override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = ???

    object Derive2 extends Derivable[Seq[JsObject], Int] {
      override val derive: Seq[JsObject] => Int = _.size

      override def path: JsPath = JsPath \ "addItemPage1"
    }
  }

  case class AddItemPage2(override val index: Option[Index] = None) extends AddItemPage(index) {
    override val normalModeUrlFragment: String = "add-page-2"
    override val checkModeUrlFragment: String = "change-page-2"

    override def route(waypoints: Waypoints): Call = Call("", "")

    override def path: JsPath = JsPath \ "addItemPage2"

    override def isTheSamePage(other: Page): Boolean = other match {
      case _: AddItemPage2 => true
      case _ => false
    }

    override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = Derive2

    object Derive2 extends Derivable[Seq[JsObject], Int] {
      override val derive: Seq[JsObject] => Int = _.size

      override def path: JsPath = JsPath \ "addItemPage2"
    }
  }

  trait AddToListSection1Page extends QuestionPage[Nothing] with AddToListQuestionPage {
    override val section: AddToListSection = Section1
    override val addItemWaypoint: Waypoint = AddItemPage1().waypoint(NormalMode)

    override def path: JsPath = ???
  }

  trait AddToListSection2Page extends QuestionPage[Nothing] with AddToListQuestionPage {
    override val section: AddToListSection = Section2
    override val addItemWaypoint: Waypoint = AddItemPage2().waypoint(NormalMode)

    override def path: JsPath = ???
  }

  object AddToListSection1Page1 extends AddToListSection1Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  object AddToListSection1Page2 extends AddToListSection1Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  object AddToListSection2Page1 extends AddToListSection2Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  object AddToListSection2Page2 extends AddToListSection2Page {
    override def route(waypoints: Waypoints): Call = Call("", "")
  }


  val testWaypoint1: Waypoint = CheckAnswersPage1.waypoint
  val testWaypoint2: Waypoint = CheckAnswersPage2.waypoint
}
