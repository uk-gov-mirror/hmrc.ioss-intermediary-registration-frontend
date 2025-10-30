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
import models.requests.AuthenticatedDataRequest
import models.{CheckMode, UserAnswers}
import pages.rejoin.RejoinSchemePage
import pages.{EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testutils.CheckYourAnswersSummaries.FluentSummaryListRow
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinSchemeView

class RejoinSchemeControllerSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, RejoinSchemePage.urlFragment))
  private val rejoinSchemePage = RejoinSchemePage
  private val previousIntermediaryRegistration = arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  "RejoinScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

      running(application) {

        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinSchemeController.onPageLoad().url)

        implicit val msgs: Messages = messages(application)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RejoinSchemeView]

        val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

        val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, vatInfoList, list)(request, msgs).toString
      }

    }

  }

  private def getChangeRegistrationVatRegistrationDetailsSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {

    implicit val authRequest: AuthenticatedDataRequest[AnyContent] =
      AuthenticatedDataRequest(
        fakeRequest, testCredentials, vrn, testEnrolments, answers, None, 0, None, None, None, None
      )

    Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(answers),
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowVatNumber(),
      VatRegistrationDetailsSummary.rowBusinessAddress(answers)
    ).flatten
  }

  private def getChangeRegistrationSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] =
    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, rejoinSchemePage)
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, answers, rejoinSchemePage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
      .checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage, Seq(previousIntermediaryRegistration))
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, rejoinSchemePage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, answers, rejoinSchemePage)
    val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, rejoinSchemePage)
    val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, rejoinSchemePage)
    val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, answers, rejoinSchemePage)
    val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, answers, rejoinSchemePage)
    val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, answers, rejoinSchemePage)

    Seq(
      niAddressSummaryRow,
      maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
        if (tradingNameSummaryRow.nonEmpty) {
          hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          hasTradingNameSummaryRow
        }
      },
      tradingNameSummaryRow,
      maybeHasPreviouslyRegisteredAsIntermediaryRow.map { hasPreviouslyRegisteredAsIntermediaryRow =>
        if (previouslyRegisteredAsIntermediaryRow.nonEmpty) {
          hasPreviouslyRegisteredAsIntermediaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          hasPreviouslyRegisteredAsIntermediaryRow
        }
      },
      previouslyRegisteredAsIntermediaryRow,
      maybeHasFixedEstablishmentSummaryRow.map { hasFixedEstablishmentSummaryRow =>
        if (euDetailsSummaryRow.nonEmpty) {
          hasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          hasFixedEstablishmentSummaryRow
        }
      },
      euDetailsSummaryRow,
      contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsEmailAddressRow,
      bankDetailsAccountNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsBicRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsIbanRow
    ).flatten
}
