/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.rejoin

import base.SpecBase
import connectors.RegistrationConnector
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.etmp.display.{EtmpDisplayRegistration, RegistrationWrapper}
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.rejoin.RejoinSchemePage
import play.api.inject.bind
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}

class StartRejoinJourneyControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate): RegistrationWrapper = {
    val exclusion = EtmpExclusion(
      exclusionReason = NoLongerSupplies,
      effectiveDate = effectiveDate,
      decisionDate = LocalDate.now(),
      quarantine = false
    )

    val etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
    registrationWrapper.copy(etmpDisplayRegistration = etmpDisplayRegistration.copy(exclusions = List(exclusion)))
  }

  override def beforeEach(): Unit =
    reset(mockRegistrationConnector)

  "StartRejoinJourney Controller" - {

    "must redirect to the RejoinScheme page when the registration request is successful" in {

      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

      when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture
      
      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {

        val request = FakeRequest(GET, controllers.rejoin.routes.StartRejoinJourneyController.onPageLoad(waypoints).url)
          .withSession("intermediaryNumber" -> intermediaryNumber)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RejoinSchemePage.route(waypoints).url
      }
    }

    "must throw an Exception when the connector fails to retrieve registrations detail" in {

      when(mockRegistrationConnector.displayRegistration(any())(any())).thenReturn(Left(NotFound).toFuture)

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {

        val request = FakeRequest(GET, controllers.rejoin.routes.StartRejoinJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe NotFound.body
        }

        verify(mockRegistrationConnector, times(1)).displayRegistration(any())(any())
      }
    }
  }
}
