package models.responses

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class AttachmentDetailsResponseSpec extends AnyWordSpec with Matchers {

  private val fullResponse = AttachmentDetailsResponse(
    attachmentNumber     = 11,
    fileName             = Some("test.pdf"),
    fileType             = Some("pdf"),
    fileDescription      = Some("pdf file attached"),
    fileSize             = Some(42),
    updateSequenceNumber = 10
  )

  "AttachmentDetailsResponse JSON format" should {

    "round-trip a fully populated response" in {
      Json.toJson(fullResponse).as[AttachmentDetailsResponse] shouldBe fullResponse
    }

    "omit absent optional fields from the JSON" in {
      val minimal = AttachmentDetailsResponse(
        attachmentNumber     = 11,
        fileName             = None,
        fileType             = None,
        fileDescription      = None,
        fileSize             = None,
        updateSequenceNumber = 10
      )

      Json.toJson(minimal) shouldBe Json.obj(
        "attachmentNumber"     -> 11,
        "updateSequenceNumber" -> 10
      )
      Json.toJson(minimal).as[AttachmentDetailsResponse] shouldBe minimal
    }
  }
}
