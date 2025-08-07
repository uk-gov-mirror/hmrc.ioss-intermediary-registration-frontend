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

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import forms.ContactDetailsFormProvider
import models.ContactDetails
import models.emailVerification.PasscodeAttemptsStatus.*
import models.requests.AuthenticatedDataRequest
import pages.amend.ChangeRegistrationPage

import javax.inject.Inject
import pages.{BankDetailsPage, CheckYourAnswersPage, ContactDetailsPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContactDetailsView
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      emailVerificationService: EmailVerificationService,
                                      formProvider: ContactDetailsFormProvider,
                                      config: FrontendAppConfig,
                                      view: ContactDetailsView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndGetData(waypoints.inAmend) {
      implicit request =>

        val ossRegistration = request.latestOssRegistration
        val iossRegistration = request.latestIossRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations

        val preparedForm = request.userAnswers.get(ContactDetailsPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, ossRegistration, numberOfIossRegistrations, iossRegistration))
    }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] =  {
    cc.authAndGetData(waypoints.inAmend).async {
      implicit request =>

        val ossRegistration = request.latestOssRegistration
        val iossRegistration = request.latestIossRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations
        val messages = messagesApi.preferred(request)
        val bankDetailsCompleted = request.userAnswers.get(BankDetailsPage).isDefined

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, ossRegistration, numberOfIossRegistrations, iossRegistration)).toFuture,

          value => {
            val continueUrl = if (waypoints.inAmend) {
              s"${config.loginContinueUrl}${ChangeRegistrationPage.route(waypoints).url}"
            } else if (bankDetailsCompleted) {
              s"${config.loginContinueUrl}${CheckYourAnswersPage.route(waypoints).url}"
            } else {
              s"${config.loginContinueUrl}${BankDetailsPage.route(waypoints).url}"
            }

            if (config.emailVerificationEnabled) {
              verifyEmailAndRedirect(waypoints, messages, continueUrl, value)
            } else {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactDetailsPage, value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(ContactDetailsPage.navigate(waypoints, updatedAnswers, updatedAnswers).route)
            }
          }
        )
    }
  }

  private def verifyEmailAndRedirect(
                                    waypoints: Waypoints,
                                    messages: Messages,
                                    continueUrl: String,
                                    value: ContactDetails
                                    )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {

    lazy val emailVerificationRequest = emailVerificationService.createEmailVerificationRequest(
      waypoints,
      request.userId,
      value.emailAddress,
      Some(messages("service.name")),
      continueUrl
    )

    emailVerificationService.isEmailVerified(value.emailAddress, request.userId).flatMap {
      case Verified =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactDetailsPage, value))
          _ <- cc.sessionRepository.set(updatedAnswers)
        } yield Redirect(ContactDetailsPage.navigate(waypoints, updatedAnswers, updatedAnswers).route)

      case LockedPasscodeForSingleEmail =>
        Redirect(routes.EmailVerificationCodesExceededController.onPageLoad()).toFuture

      case LockedTooManyLockedEmails =>
        Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()).toFuture

      case NotVerified =>
        emailVerificationRequest
          .flatMap {
            case Right(validResponse) =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactDetailsPage, value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(s"${config.emailVerificationUrl}${validResponse.redirectUri}")
            case _ => Future.successful(Redirect(routes.ContactDetailsController.onPageLoad(waypoints).url))
          }
    }

  }
}
