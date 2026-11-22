package models.responses

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class DeleteAttachmentResponseSpec extends AnyWordSpec with Matchers {
  "DeleteAttachmentResponse JSON format" should {

    "serialize to JSON correctly" in {
      Json.toJson(DeleteAttachmentResponse(42)) shouldBe Json.obj("updateSequenceNumber" -> 42)
    }

    "deserialize from JSON correctly" in {
      Json.obj("updateSequenceNumber" -> 42).as[DeleteAttachmentResponse] shouldBe DeleteAttachmentResponse(42)
    }

    "fail to deserialize when updateSequenceNumber is missing" in {
      Json.obj().validate[DeleteAttachmentResponse].isError shouldBe true
    }
  }
}
