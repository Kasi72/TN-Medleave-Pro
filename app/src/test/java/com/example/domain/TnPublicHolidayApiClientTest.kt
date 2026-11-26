package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TnPublicHolidayApiClientTest {

  @Test
  fun parseNagerDateJson_filtersForNationalAndTamilNaduAndClassifiesTypes() {
    val payload = """
      [
        {
          "date": "2026-01-26",
          "localName": "Republic Day",
          "name": "Republic Day",
          "countryCode": "IN",
          "global": true,
          "counties": null,
          "types": ["Public"]
        },
        {
          "date": "2026-01-15",
          "localName": "Pongal",
          "name": "Thai Pongal",
          "countryCode": "IN",
          "global": false,
          "counties": ["IN-TN"],
          "types": ["Public"]
        },
        {
          "date": "2026-05-01",
          "localName": "Maharashtra Day",
          "name": "Maharashtra Day",
          "countryCode": "IN",
          "global": false,
          "counties": ["IN-MH"],
          "types": ["Public"]
        },
        {
          "date": "2026-03-30",
          "localName": "Bannari Amman Festival",
          "name": "Bannari Amman Festival",
          "countryCode": "IN",
          "global": false,
          "counties": ["IN-TN"],
          "types": ["Optional"]
        }
      ]
    """.trimIndent()

    val parsed = TnPublicHolidayApiClient.parseNagerDateJson(payload)
    assertEquals(3, parsed.size)
    assertTrue(parsed.none { it.nameEnglish == "Maharashtra Day" })

    val republicDay = parsed.first { it.dateIso == "2026-01-26" }
    assertFalse(republicDay.isRestrictedHoliday)
    assertEquals("ஜனவரி", republicDay.monthNameTamil)

    val pongal = parsed.first { it.dateIso == "2026-01-15" }
    assertFalse(pongal.isRestrictedHoliday)
    assertEquals("Thai Pongal", pongal.nameEnglish)

    val bannari = parsed.first { it.dateIso == "2026-03-30" }
    assertTrue(bannari.isRestrictedHoliday)
    assertEquals("மார்ச்", bannari.monthNameTamil)
  }

  @Test
  fun parseOpenHolidaysJson_extractsTamilNaduSubdivisionAndBilingualNames() {
    val payload = """
      [
        {
          "startDate": "2027-01-14",
          "type": "Public",
          "nationwide": false,
          "subdivisions": [{"code": "IN-TN", "shortName": "TN"}],
          "name": [
            {"language": "EN", "text": "Pongal"},
            {"language": "TA", "text": "பொங்கல்"}
          ]
        },
        {
          "startDate": "2027-04-14",
          "type": "Optional",
          "nationwide": false,
          "subdivisions": [{"code": "IN-KL", "shortName": "KL"}],
          "name": [
            {"language": "EN", "text": "Vishu"}
          ]
        },
        {
          "startDate": "2027-08-15",
          "type": "Public",
          "nationwide": true,
          "subdivisions": [],
          "name": [
            {"language": "EN", "text": "Independence Day"}
          ]
        }
      ]
    """.trimIndent()

    val parsed = TnPublicHolidayApiClient.parseOpenHolidaysJson(payload)
    assertEquals(2, parsed.size)
    assertTrue(parsed.none { it.nameEnglish == "Vishu" })

    val pongal = parsed.first { it.dateIso == "2027-01-14" }
    assertEquals("Pongal", pongal.nameEnglish)
    assertEquals("பொங்கல்", pongal.nameTamil)
    assertFalse(pongal.isRestrictedHoliday)
    assertEquals("ஜனவரி", pongal.monthNameTamil)

    val independence = parsed.first { it.dateIso == "2027-08-15" }
    assertEquals("ஆகஸ்ட்", independence.monthNameTamil)
  }

  @Test
  fun parseTnGovHtmlTable_parsesOfficialGovernmentTableDatesAndHolidayTypes() {
    val html = """
      <table>
        <tr><th>Sl.No</th><th>Holiday</th><th>Date</th><th>Day</th></tr>
        <tr><td>1</td><td>New Year's Day</td><td>01.01.2027</td><td>Friday</td></tr>
        <tr><td>2</td><td>Thai Pongal &amp; Harvest Festival</td><td>14-01-2027</td><td>Thursday</td></tr>
        <tr><td>3</td><td>Erode Local Festival (RH)</td><td>29/03/2027</td><td>Monday</td></tr>
      </table>
    """.trimIndent()

    val parsed = TnPublicHolidayApiClient.parseTnGovHtmlTable(html, fallbackYear = 2027)
    assertEquals(3, parsed.size)

    val newYear = parsed[0]
    assertEquals("2027-01-01", newYear.dateIso)
    assertEquals("New Year's Day", newYear.nameEnglish)
    assertFalse(newYear.isRestrictedHoliday)

    val pongal = parsed[1]
    assertEquals("2027-01-14", pongal.dateIso)
    assertEquals("Thai Pongal & Harvest Festival", pongal.nameEnglish)
    assertFalse(pongal.isRestrictedHoliday)

    val erodeLocal = parsed[2]
    assertEquals("2027-03-29", erodeLocal.dateIso)
    assertTrue(erodeLocal.isRestrictedHoliday)
  }

  @Test
  fun parseCustomHolidayJson_supportsWrappedAndRootArrayFormats() {
    val wrapped = """
      {
        "holidays": [
          {
            "dateIso": "2027-04-14",
            "nameEnglish": "Tamil New Year",
            "nameTamil": "தமிழ் புத்தாண்டு",
            "isRestrictedHoliday": false
          },
          {
            "date": "2027-11-08",
            "name": "Deepavali Eve",
            "type": "RH"
          }
        ]
      }
    """.trimIndent()

    val parsedWrapped = TnPublicHolidayApiClient.parseCustomHolidayJson(wrapped)
    assertEquals(2, parsedWrapped.size)
    assertEquals("தமிழ் புத்தாண்டு", parsedWrapped[0].nameTamil)
    assertFalse(parsedWrapped[0].isRestrictedHoliday)
    assertTrue(parsedWrapped[1].isRestrictedHoliday)

    val rootArray = """
      [
        {"date": "2027-12-25", "title": "Christmas", "type": "Public"}
      ]
    """.trimIndent()
    val parsedArray = TnPublicHolidayApiClient.parseCustomHolidayJson(rootArray)
    assertEquals(1, parsedArray.size)
    assertEquals("Christmas", parsedArray[0].nameEnglish)
    assertEquals("டிசம்பர்", parsedArray[0].monthNameTamil)
  }

  @Test
  fun knowledgeBaseRefresh_incrementsRevisionAndTracksSource() {
    val before = TnHolidays2027Catalog.knowledgeBaseRevision
    TnHolidays2027Catalog.notifyKnowledgeBaseRefreshed("date.nager.at (IN-TN)")
    assertTrue(TnHolidays2027Catalog.knowledgeBaseRevision > before)
    assertEquals("date.nager.at (IN-TN)", TnHolidays2027Catalog.lastKnowledgeBaseSource)
  }
}
