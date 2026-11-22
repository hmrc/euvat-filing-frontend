package models.requests

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class DeleteAttachmentRequestSpec extends AnyWordSpec with Matchers {

  "DeleteAttachmentRequest JSON format" should {

    "serialize to JSON correctly" in {
      val request = DeleteAttachmentRequest(
        applicationId        = 123L,
        attachmentNumber     = 4,
        updateSequenceNumber = 1
      )
      Json.toJson(request) shouldBe Json.obj(
        "applicationId"        -> 123,
        "attachmentNumber"     -> 4,
        "updateSequenceNumber" -> 1
      )
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "applicationId"        -> 123,
        "attachmentNumber"     -> 4,
        "updateSequenceNumber" -> 1
      )
      json.as[DeleteAttachmentRequest] shouldBe DeleteAttachmentRequest(
        applicationId        = 123L,
        attachmentNumber     = 4,
        updateSequenceNumber = 1
      )
    }

    "fail to deserialize when applicationId is missing" in {
      Json.obj("attachmentNumber" -> 4, "updateSequenceNumber" -> 1).validate[DeleteAttachmentRequest].isError shouldBe true
    }

    "fail to deserialize when attachmentNumber is missing" in {
      Json.obj("applicationId" -> 123, "updateSequenceNumber" -> 1).validate[DeleteAttachmentRequest].isError shouldBe true
    }

    "fail to deserialize when updateSequenceNumber is missing" in {
      Json.obj("applicationId" -> 123, "attachmentNumber" -> 4).validate[DeleteAttachmentRequest].isError shouldBe true
    }
  }
}
