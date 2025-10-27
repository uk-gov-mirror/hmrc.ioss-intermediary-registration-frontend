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

package models.etmp.display

import base.SpecBase
import models.etmp.{EtmpExclusion, EtmpExclusionReason}
import models.etmp.EtmpExclusionReason.*
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.time.LocalDate

class EtmpDisplayRegistrationSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  "EtmpDisplayRegistration" - {

    "must deserialise/serialise from and to EtmpDisplayRegistration" in {

      val json = Json.obj(
        "customerIdentification" -> etmpDisplayRegistration.customerIdentification,
        "tradingNames" -> etmpDisplayRegistration.tradingNames,
        "clientDetails" -> etmpDisplayRegistration.clientDetails,
        "intermediaryDetails" -> etmpDisplayRegistration.intermediaryDetails,
        "otherAddress" -> etmpDisplayRegistration.otherAddress,
        "schemeDetails" -> etmpDisplayRegistration.schemeDetails,
        "exclusions" -> etmpDisplayRegistration.exclusions,
        "bankDetails" -> etmpDisplayRegistration.bankDetails,
        "adminUse" -> etmpDisplayRegistration.adminUse
      )

      val expectedResult = EtmpDisplayRegistration(
        customerIdentification = etmpDisplayRegistration.customerIdentification,
        tradingNames = etmpDisplayRegistration.tradingNames,
        clientDetails = etmpDisplayRegistration.clientDetails,
        intermediaryDetails = etmpDisplayRegistration.intermediaryDetails,
        otherAddress = etmpDisplayRegistration.otherAddress,
        schemeDetails = etmpDisplayRegistration.schemeDetails,
        exclusions = etmpDisplayRegistration.exclusions,
        bankDetails = etmpDisplayRegistration.bankDetails,
        adminUse = etmpDisplayRegistration.adminUse
      )

      json.validate[EtmpDisplayRegistration] `mustBe` JsSuccess(expectedResult)
      Json.toJson(expectedResult) `mustBe` json
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "tradingNames" -> 123456
      )

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }

    ".canRejoinScheme" - {

      val currentDate = LocalDate.now()

      val nonReversalEtmpExclusionReasons = Table(
        "etmpExclusionReason",
        NoLongerSupplies,
        CeasedTrade,
        NoLongerMeetsConditions,
        FailsToComply,
        VoluntarilyLeaves,
        TransferringMSID
      )

      def createExclusion(etmpExclusionReason: EtmpExclusionReason,
                          effectiveDate: LocalDate = LocalDate.now(),
                          quarantine: Boolean = false): EtmpExclusion = {
        EtmpExclusion(
          exclusionReason = etmpExclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(),
          quarantine = quarantine)
      }

      "return true" - {

        "when it is not a reversal, not quarantined and the effective date is not in the future" in {
          forAll(nonReversalEtmpExclusionReasons) { nonReversalEtmpExclusionReason =>
            etmpDisplayRegistration.copy(exclusions = List(
                createExclusion(nonReversalEtmpExclusionReason, effectiveDate = currentDate)
              ))
              .canRejoinScheme(currentDate) mustBe true
          }
        }

        "when it is not a reversal, quarantined but the effectiveDate is more than 2 years ago" in {
          forAll(nonReversalEtmpExclusionReasons) { nonReversalEtmpExclusionReason =>
            etmpDisplayRegistration.copy(exclusions = List(
                createExclusion(nonReversalEtmpExclusionReason, effectiveDate = currentDate.minusYears(2).minusDays(1), quarantine = true))
              )
              .canRejoinScheme(currentDate) mustBe true
          }
        }
      }

      "return false" - {

        "for an empty exclusion list" in {
          etmpDisplayRegistration.copy(exclusions = List.empty).canRejoinScheme(currentDate) mustBe false
        }

        "when the exclusion reason is a Reversal" in {
          etmpDisplayRegistration.copy(exclusions = List(createExclusion(Reversal)))
            .canRejoinScheme(currentDate) mustBe false
        }

        "when the exclusion reason is not a Reversal and it is quarantined with an effective date is above or equal to 2 years ago" in {

          val dates = Table(
            "date",
            LocalDate.now().minusYears(2),
            LocalDate.now().minusYears(2).plusDays(1),
          )

          forAll(nonReversalEtmpExclusionReasons) { etmpExclusionReason =>
            forAll(dates) { date =>
              etmpDisplayRegistration.copy(exclusions = List(
                  createExclusion(etmpExclusionReason, effectiveDate = date, quarantine = true)
                ))
                .canRejoinScheme(currentDate) mustBe false
            }
          }
        }
      }
    }
  }
}
