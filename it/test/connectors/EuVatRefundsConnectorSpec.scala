/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import models.{ApplicationDetails, EncryptionStatus, PurchaseImport}
import models.requests.*
import models.responses.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterEach, LoneElement}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BankDetailsEncryption

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class EuVatRefundsConnectorSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures with BeforeAndAfterEach with LoneElement {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockConfig: ServicesConfig = mock[ServicesConfig]
  val mockCryptoProvider: BankDetailsEncryption = mock[BankDetailsEncryption]
  val baseUrl = "http://localhost:9000/euvat-refunds"

  when(mockConfig.baseUrl("euvat-refunds")).thenReturn("http://localhost:9000")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttp, mockRequestBuilder, mockCryptoProvider)
  }

  val connector = new EuVatRefundsConnector(mockConfig, mockHttp, mockCryptoProvider)

  "EuVatRefundsConnector.retrieveTradersKnownFacts" should {

    "call the correct URL and return the expected response" in {
      val expected = TraderKnownFactsResponse(
        vatRegNumber = 123,
        traderName   = Some("ABC GmbH"),
        tradeClass   = Some("49200")
      )
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[TraderKnownFactsResponse](any(), any()))
        .thenReturn(Future.successful(expected))

      val result = connector.retrieveTradersKnownFacts().futureValue
      result shouldBe expected

      verify(mockHttp).get(url"$baseUrl/traders/get-known-facts")
      verify(mockRequestBuilder).execute[TraderKnownFactsResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[TraderKnownFactsResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.retrieveTradersKnownFacts()
      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.getLatestApplications" should {
    val request = LatestApplicationRequest(
      applicantVatRegNumber = "123456789",
      refundingCountry      = Some("LV"),
      startDate             = Some(LocalDateTime.of(2025, 2, 1, 0, 0)),
      endDate               = Some(LocalDateTime.of(2025, 5, 31, 0, 0)),
      representativeId      = Some("rep123"),
      maxNumber             = 10,
      orderBy               = None,
      sortOrder             = None,
      startAt               = None
    )
    val expectedResponse = LatestApplicationResponse(
      applications     = List.empty,
      totalApplication = 0
    )

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[LatestApplicationResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getLatestApplications(request).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/get-latest-application")
      verify(mockRequestBuilder).execute[LatestApplicationResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[LatestApplicationResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.getLatestApplications(request)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.getApplicationDetails" should {

    val applicationId = 1L
    val unEncryptedApplicationDetails = ApplicationDetails(
      applicationId              = 1L,
      applicationNumber          = None,
      refundingCountryCode       = None,
      applicationVersion         = None,
      periodStartDate            = None,
      periodEndDate              = None,
      applicantVatRegNumber      = None,
      applicantEmailAddress      = None,
      applicantTelephoneNumber   = None,
      representativeId           = None,
      representativeIdType       = None,
      representativeCountry      = None,
      representativeEmailAddress = None,
      representativeTelephoneNum = None,
      applicationLanguage        = None,
      bankAccountOwnerName       = None,
      bankAccountOwnerType       = None,
      ibanCode                   = Some("plain IBAN code"),
      bicCode                    = Some("plain BIC code"),
      bankAccountCurrencyCode    = Some("GBP"),
      businessActivityCode1      = None,
      businessActivityCode2      = None,
      businessActivityCode3      = None,
      messageId                  = None,
      correlationId              = None,
      cipherText                 = Option("plain cipherText"),
      encryptionStatus           = Some(EncryptionStatus.M.toString),
      updateSeqNumber            = 2,
      applicationStatus          = None,
      submissionStatus           = None
    )

    "call the correct URL and NOT decrypt the bank details in the response when encryptionStatus = M" in {
      val backendResponse = ApplicationDetailsResponse(applicationDetails = List(unEncryptedApplicationDetails))

      when(mockHttp.get(mockitoEq(url"$baseUrl/get-application-details/$applicationId"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ApplicationDetailsResponse](any(), any()))
        .thenReturn(Future.successful(backendResponse))

      val result = connector.getApplicationDetails(applicationId).futureValue

      result shouldBe backendResponse
      verifyNoInteractions(mockCryptoProvider)
    }

    "call the correct URL and decrypt the bank details in the response when encryptionStatus = E" in {
      val encryptedBackendResponse = ApplicationDetailsResponse(
        applicationDetails = List(
          unEncryptedApplicationDetails.copy(
            ibanCode         = Some("encrypted IBAN code"),
            bicCode          = Some("encrypted BIC code"),
            cipherText       = Some("encrypted cipherText"),
            encryptionStatus = Some(EncryptionStatus.E.toString)
          )
        )
      )

      val decryptedApplicationDetails = unEncryptedApplicationDetails.copy(
        ibanCode         = Some("decrypted IBAN code"),
        bicCode          = Some("decrypted BIC code"),
        cipherText       = Some("decrypted cipherText"),
        encryptionStatus = Some(EncryptionStatus.M.toString)
      )

      when(mockCryptoProvider.decrypt("encrypted IBAN code")).thenReturn(Some("decrypted IBAN code"))
      when(mockCryptoProvider.decrypt("encrypted BIC code")).thenReturn(Some("decrypted BIC code"))
      when(mockCryptoProvider.decrypt("encrypted cipherText")).thenReturn(Some("decrypted cipherText"))

      when(mockHttp.get(mockitoEq(url"$baseUrl/get-application-details/$applicationId"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ApplicationDetailsResponse](any(), any()))
        .thenReturn(Future.successful(encryptedBackendResponse))

      val result = connector.getApplicationDetails(applicationId).futureValue

      result.applicationDetails.loneElement shouldBe decryptedApplicationDetails
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("Backend error")

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ApplicationDetailsResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.getApplicationDetails(applicationId)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.getSupplierVrnCount" should {
    val request = SupplierVrnCountRequest(
      applicationId = 133,
      itemNumber    = 4,
      vatNumber     = "500000881",
      invoiceNumber = "a444"
    )
    val expectedResponse = SupplierVrnCountResponse(duplicateCount = 1)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SupplierVrnCountResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getSupplierVrnCount(request).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/get-supplier-vrn-count")
      verify(mockRequestBuilder).execute[SupplierVrnCountResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SupplierVrnCountResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.getSupplierVrnCount(request)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.addPurchase" should {
    val request = AddPurchaseRequest(
      applicationId            = 123456,
      goodsDescriptionCategory = "1",
      goodsDescriptionText     = Some("Fuel"),
      updateSequenceNumber     = 1
    )

    val expectedResponse = AddPurchaseResponse(itemNumber = 4, updateSequenceNumber = 1)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[AddPurchaseResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.addPurchase(request).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/add-purchase")
      verify(mockRequestBuilder).execute[AddPurchaseResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[AddPurchaseResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(connector.addPurchase(request).failed) { ex =>
        ex shouldBe a[RuntimeException]
      }
    }

  }

  "EuVatRefundsConnector.addImport" should {

    val request = AddImportRequest(
      applicationId            = 123456,
      goodsDescriptionCategory = "1",
      goodsDescriptionText     = Some("Fuel"),
      updateSequenceNumber     = 1
    )

    val expectedResponse = AddImportResponse(itemNumber = 4, updateSequenceNumber = 1)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[AddImportResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.addImport(request).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/add-import")
      verify(mockRequestBuilder).execute[AddImportResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[AddImportResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(connector.addImport(request).failed) { ex =>
        ex shouldBe a[RuntimeException]
      }
    }

  }

  "EuVatRefundsConnector.getSupplierTaxIdentifierCount" should {
    val requestPayload = SupplierTaxIdentifierCountRequest(applicationId = 123L, itemNumber = 1, taxIdentifier = "TAX123", invoiceNumber = "INV1")
    val expectedResponse = SupplierTaxIdentifierCountResponse(duplicateCount = 2)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SupplierTaxIdentifierCountResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getSupplierTaxIdentifierCount(requestPayload).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/get-supplier-taxIdentifier-count")
      verify(mockRequestBuilder).execute[SupplierTaxIdentifierCountResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SupplierTaxIdentifierCountResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(connector.getSupplierTaxIdentifierCount(requestPayload).failed) { ex =>
        ex shouldBe a[RuntimeException]
      }
    }
  }

  "EuVatRefundsConnector.updatePurchase" should {
    val updateRequest = models.requests.UpdatePurchaseRequest(
      applicationId               = 123L,
      itemNumber                  = 1,
      goodsDescriptionCategory    = "1.2",
      goodsDescriptionSubCategory = None,
      goodsDescriptionText        = Some("Fuel"),
      simplifiedInvoiceIndicator  = Some("Y"),
      supplierName                = Some("Supplier"),
      supplierAddress1            = None,
      supplierAddress2            = None,
      supplierAddress3            = None,
      supplierVatRegNumber        = None,
      supplierTaxIdentifier       = None,
      invoiceDate                 = None,
      invoiceNumber               = None,
      currencyCode                = None,
      taxableAmount               = None,
      vatAmount                   = None,
      deductibleVatAmount         = None,
      updateSequenceNumber        = 1
    )

    val expectedResponse = UpdatePurchaseResponse(updateSequenceNumber = 42)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.put(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[UpdatePurchaseResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.updatePurchase(updateRequest).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).put(url"$baseUrl/update-purchase-details")
      verify(mockRequestBuilder).execute[UpdatePurchaseResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.put(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[UpdatePurchaseResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.updatePurchase(updateRequest)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }

    "EuVatRefundsConnector.getPurchaseDetails" should {
      val expectedResponse = PurchaseDetailsResponse(
        goodsDescriptionCode       = "1",
        goodsDescriptionSubCode    = Some("1.2"),
        goodsDescriptionText       = Some("Fuel"),
        simplifiedInvoiceIndicator = Some("false"),
        supplierName               = Some("Supplier"),
        supplierAddressLine1       = Some("Line 1"),
        supplierAddressLine2       = None,
        supplierAddressLine3       = None,
        supplierVatNumber          = Some("FR123"),
        supplierTaxIdentifier      = None,
        invoiceDate                = None,
        invoiceNumber              = Some("INV-1"),
        currencyCode               = Some("EUR"),
        taxableAmount              = Some(BigDecimal(100)),
        vatAmount                  = Some(BigDecimal(20)),
        deductibleVatAmount        = Some(BigDecimal(10)),
        updateSequenceNumber       = 1
      )

      "call the correct URL and return the expected response" in {
        reset(mockHttp, mockRequestBuilder)

        when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[PurchaseDetailsResponse](any(), any()))
          .thenReturn(Future.successful(expectedResponse))

        val result = connector.getPurchaseDetails(123L, 4).futureValue

        result shouldBe expectedResponse

        verify(mockHttp).get(url"$baseUrl/get-purchase-details/123/4")
        verify(mockRequestBuilder).execute[PurchaseDetailsResponse](any(), any())
      }

      "propagate failures from the HTTP client" in {
        val failure = new RuntimeException("boom")

        when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[PurchaseDetailsResponse](any(), any()))
          .thenReturn(Future.failed(failure))

        whenReady(connector.getPurchaseDetails(123L, 4).failed) { ex =>
          ex shouldBe failure
        }
      }
    }
  }

  "EuVatRefundsConnector.getPurchaseImportList" should {
    val requestPayload = PurchaseImportListRequest(applicationId = 123)
    val expectedResponse = PurchaseImportListResponse(
      totalVatClaims = BigDecimal(334),
      totalItems     = 2,
      purchaseImportList = List(
        PurchaseImport(
          itemNumber                  = 123,
          itemType                    = "P",
          goodsDescriptionCategory    = "1",
          goodsDescriptionSubCategory = Some("1.2.3"),
          currencyCode                = Some("EU"),
          taxableAmount               = BigDecimal(300),
          vatAmount                   = BigDecimal(200),
          deductibleVatAmount         = BigDecimal(100)
        ),
        PurchaseImport(
          itemNumber                  = 456,
          itemType                    = "I",
          goodsDescriptionCategory    = "7",
          goodsDescriptionSubCategory = Some("7.9"),
          currencyCode                = Some("EU"),
          taxableAmount               = BigDecimal(456),
          vatAmount                   = BigDecimal(345),
          deductibleVatAmount         = BigDecimal(234)
        )
      )
    )

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[PurchaseImportListResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getPurchaseImportList(requestPayload).futureValue
      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/get-purchases-and-imports")
      verify(mockRequestBuilder).execute[PurchaseImportListResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[PurchaseImportListResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(connector.getPurchaseImportList(requestPayload).failed) { ex =>
        ex shouldBe a[RuntimeException]
      }
    }
  }

  "EuVatRefundsConnector.getImportDetails" should {

    "call the item details URL and return the import details response" in {
      reset(mockHttp, mockRequestBuilder)
      val expectedResponse = ImportDetailsResponse(
        applicationId               = 123,
        itemNumber                  = 4,
        issuingDate                 = Some(java.time.LocalDateTime.of(2026, 10, 21, 0, 0)),
        supplierName                = Some("Supplier"),
        supplierAddressLine1        = Some("Address line 1"),
        supplierAddressLine2        = None,
        supplierAddressLine3        = None,
        supplierCountryCode         = Some("FR"),
        goodsDescriptionCategory    = "1",
        currencyCode                = Some("EUR"),
        taxableAmount               = Some(BigDecimal(300)),
        vatAmount                   = Some(BigDecimal(200)),
        deductibleVatAmount         = Some(BigDecimal(100)),
        hasSadReferenceNumber       = Some("N"),
        sadReferenceNumber          = None,
        referenceInformation        = None,
        goodsDescriptionText        = Some("Import details"),
        goodsDescriptionSubcategory = Some("1.2.3"),
        updateSequenceNumber        = 7
      )

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ImportDetailsResponse](any(), any())).thenReturn(Future.successful(expectedResponse))

      connector.getImportDetails(123, 4).futureValue shouldBe expectedResponse
      verify(mockHttp).get(url"$baseUrl/get-import-details/123/4")
      verify(mockRequestBuilder).execute[ImportDetailsResponse](any(), any())
    }
  }

  "EuVatRefundsConnector.getPurchaseDetails" should {

    "call the item details URL and return the purchase details response" in {
      reset(mockHttp, mockRequestBuilder)
      val expectedResponse = PurchaseDetailsResponse(
        goodsDescriptionCode       = "1",
        goodsDescriptionSubCode    = Some("1.2.3"),
        goodsDescriptionText       = Some("Purchase details"),
        simplifiedInvoiceIndicator = Some("N"),
        supplierName               = Some("Supplier"),
        supplierAddressLine1       = Some("Address line 1"),
        supplierAddressLine2       = Some("Address line 2"),
        supplierAddressLine3       = Some("Address line 3"),
        supplierVatNumber          = Some("VAT123"),
        supplierTaxIdentifier      = Some("TAX123"),
        invoiceDate                = Some(java.time.LocalDateTime.of(2026, 10, 21, 0, 0)),
        invoiceNumber              = Some("INV123"),
        currencyCode               = Some("EUR"),
        taxableAmount              = Some(BigDecimal(300)),
        vatAmount                  = Some(BigDecimal(200)),
        deductibleVatAmount        = Some(BigDecimal(100)),
        updateSequenceNumber       = 7
      )

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[PurchaseDetailsResponse](any(), any())).thenReturn(Future.successful(expectedResponse))

      connector.getPurchaseDetails(123, 4).futureValue shouldBe expectedResponse
      verify(mockHttp).get(url"$baseUrl/get-purchase-details/123/4")
      verify(mockRequestBuilder).execute[PurchaseDetailsResponse](any(), any())
    }
  }

  "EuVatRefundsConnector.deleteApplication" should {

    "call the delete endpoint with JSON body and return Unit" in {
      reset(mockHttp, mockRequestBuilder)
      val req = DeleteApplicationRequest(123L, 1)

      when(mockHttp.delete(any())(any())).thenReturn(mockRequestBuilder)

      doReturn(mockRequestBuilder).when(mockRequestBuilder).withBody(any[JsValue]())(any(), any(), any())
      doReturn(Future.successful(())).when(mockRequestBuilder).execute[Unit](any(), any())

      val result: Unit = connector.deleteApplication(req).futureValue

      result shouldBe ()

      verify(mockHttp).delete(url"$baseUrl/delete-application")
      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockRequestBuilder).withBody(bodyCaptor.capture())(any(), any(), any())
      bodyCaptor.getValue shouldBe Json.toJson(req)
      verify(mockRequestBuilder).execute[Unit](any(), any())
    }
  }

  "EuVatRefundsConnector.deletePurchase" should {

    "call the purchase delete endpoint with the JSON body and return the sequence number" in {
      reset(mockHttp, mockRequestBuilder)
      val request = DeleteItemRequest(applicationId = 123, itemNumber = 4, updateSequenceNumber = 7)
      val response = DeleteItemResponse(updateSequenceNumber = 8)

      when(mockHttp.delete(any())(any())).thenReturn(mockRequestBuilder)
      doReturn(mockRequestBuilder).when(mockRequestBuilder).withBody(any[JsValue]())(any(), any(), any())
      doReturn(Future.successful(response)).when(mockRequestBuilder).execute[DeleteItemResponse](any(), any())

      connector.deletePurchase(request).futureValue shouldBe response
      verify(mockHttp).delete(url"$baseUrl/delete-purchase")
      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockRequestBuilder).withBody(bodyCaptor.capture())(any(), any(), any())
      bodyCaptor.getValue shouldBe Json.toJson(request)
      verify(mockRequestBuilder).execute[DeleteItemResponse](any(), any())
    }
  }

  "EuVatRefundsConnector.deleteImport" should {

    "call the import delete endpoint with the JSON body and return the sequence number" in {
      reset(mockHttp, mockRequestBuilder)
      val request = DeleteItemRequest(applicationId = 123, itemNumber = 5, updateSequenceNumber = 9)
      val response = DeleteItemResponse(updateSequenceNumber = 10)

      when(mockHttp.delete(any())(any())).thenReturn(mockRequestBuilder)
      doReturn(mockRequestBuilder).when(mockRequestBuilder).withBody(any[JsValue]())(any(), any(), any())
      doReturn(Future.successful(response)).when(mockRequestBuilder).execute[DeleteItemResponse](any(), any())

      connector.deleteImport(request).futureValue shouldBe response
      verify(mockHttp).delete(url"$baseUrl/delete-import")
      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockRequestBuilder).withBody(bodyCaptor.capture())(any(), any(), any())
      bodyCaptor.getValue shouldBe Json.toJson(request)
      verify(mockRequestBuilder).execute[DeleteItemResponse](any(), any())
    }
  }

  "EuVatRefundsConnector.updateApplicationDetails" should {

    val updateRequest = UpdateApplicationDetailsRequest(
      applicationId              = 133,
      applicationLanguage        = "en",
      refundingCountry           = "LV",
      periodStartDate            = LocalDateTime.of(2011, 6, 1, 0, 0),
      periodEndDate              = LocalDateTime.of(2011, 10, 31, 23, 59, 59),
      applicantEmailAddress      = "test@hotmail.com",
      applicantPhoneNumber       = None,
      representativeCountry      = None,
      representativeEmailAddress = None,
      representativePhoneNumber  = None,
      bankAccountOwnerName       = None,
      bankAccountOwnerType       = None,
      ibanCode                   = None,
      bicCode                    = None,
      bankAccountCurrencyCode    = None,
      businessActivityCode2      = None,
      businessActivityCode3      = None,
      cipherText                 = None,
      encryptionStatus           = None,
      updateSequenceNumber       = 30
    )

    val expectedResponse = UpdateApplicationDetailsResponse(updateSequenceNumber = 32)

    "call the correct URL, encrypt bank details and return the expected response when encryptionStatus = M" in {
      val requestWithEncryptionStatusM = updateRequest.copy(
        ibanCode         = Some("plain IBAN code"),
        bicCode          = Some("plain BIC code"),
        cipherText       = Some("plain cipherText"),
        encryptionStatus = Some(EncryptionStatus.M.toString)
      )

      when(mockCryptoProvider.encrypt("plain IBAN code")).thenReturn("encrypted IBAN code")
      when(mockCryptoProvider.encrypt("plain BIC code")).thenReturn("encrypted BIC code")
      when(mockCryptoProvider.knownCipherText).thenReturn("encrypted cipherText")
      when(mockHttp.put(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[UpdateApplicationDetailsResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.updateApplicationDetails(requestWithEncryptionStatusM).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).put(url"$baseUrl/update-application-details")
      verify(mockRequestBuilder).execute[UpdateApplicationDetailsResponse](any(), any())
      verify(mockCryptoProvider).encrypt("plain IBAN code")
      verify(mockCryptoProvider).encrypt("plain BIC code")
      verify(mockCryptoProvider).knownCipherText
    }

    "fail when encryptionStatus = E" in {
      val requestWithEncryptionStatusE = updateRequest.copy(
        ibanCode = Some("encrypted IBAN code"),
        bicCode = Some("encrypted BIC code"),
        cipherText = Some("encrypted cipherText"),
        encryptionStatus = Some(EncryptionStatus.E.toString)
      )

      val failure = the [Exception] thrownBy connector.updateApplicationDetails(requestWithEncryptionStatusE).futureValue
      failure should have message "Failed to update application request"
    }

    "fail when encryptionStatus is None" in {
      val requestWithEncryptionStatusE = updateRequest.copy(encryptionStatus = None)

      val failure = the[Exception] thrownBy connector.updateApplicationDetails(requestWithEncryptionStatusE).futureValue
      failure should have message "Failed to update application request"
    }

    "propagate failures from the HTTP client" in {
      val requestWithEncryptionStatusM = updateRequest.copy(
        ibanCode         = Some("plain IBAN code"),
        bicCode          = Some("plain BIC code"),
        cipherText       = Some("plain cipherText"),
        encryptionStatus = Some(EncryptionStatus.M.toString)
      )

      val failure = new RuntimeException("boom")

      when(mockCryptoProvider.encrypt("plain IBAN code")).thenReturn("encrypted IBAN code")
      when(mockCryptoProvider.encrypt("plain BIC code")).thenReturn("encrypted BIC code")
      when(mockCryptoProvider.knownCipherText).thenReturn("encrypted cipherText")

      when(mockHttp.put(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[UpdateApplicationDetailsResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.updateApplicationDetails(requestWithEncryptionStatusM)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.getSadReferenceCount" should {
    val request = SadReferenceCountRequest(
      applicationId      = 133,
      itemNumber         = 4,
      sadReferenceNumber = "12345"
    )
    val expectedResponse = SadReferenceCountResponse(duplicateCount = 1)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SadReferenceCountResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getSadReferenceCount(request).futureValue

      result shouldBe expectedResponse

      verify(mockHttp).post(url"$baseUrl/get-sad-reference-count")
      verify(mockRequestBuilder).execute[SadReferenceCountResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[SadReferenceCountResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.getSadReferenceCount(request)

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }

  "EuVatRefundsConnector.getReferenceInfoCount" should {
    val itemNumber = 3
    val applicationId = 123L
    val url = url"$baseUrl/get-reference-information-count"
    val request = ReferenceInfoCountRequest(applicationId, itemNumber)

    "call the correct URL and return the expected response" in {
      reset(mockHttp, mockRequestBuilder)

      val expectedResponse = ReferenceInfoCountResponse(2)

      when(mockHttp.post(url)(hc)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any[ExecutionContext]())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ReferenceInfoCountRequest](any(), any[ExecutionContext]()))
        .thenReturn(Future.successful(expectedResponse))

      val result = connector.getReferenceInfoCount(request).futureValue

      result shouldBe expectedResponse
    }

    "propagate failures from the HTTP client" in {
      reset(mockHttp, mockRequestBuilder)
      val failure = new RuntimeException("failure")

      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[ReferenceInfoCountRequest](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.getReferenceInfoCount(request)

      result.failed.futureValue shouldBe failure
    }
  }

}
