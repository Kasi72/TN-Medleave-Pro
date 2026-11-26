package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.TnHolidayItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

data class PublicHolidayApiFetchResult(
  val holidays: List<TnHolidayItem>,
  val sourcesContacted: List<String>,
  val primarySourceLabel: String,
  val rawSnippetForAi: String,
  val fetchedFromNetwork: Boolean
)

/**
 * Deterministic multi-source Public API & official Tamil Nadu Government Holiday client.
 * Fetches and parses structured JSON/HTML calendars without relying solely on LLM scraping:
 *  1. Nager.Date Public Holidays API (`date.nager.at/api/v3/PublicHolidays/{year}/IN`, filtered for National + IN-TN)
 *  2. OpenHolidays API (`openholidaysapi.org/PublicHolidays`, subdivision `IN-TN`)
 *  3. Official Tamil Nadu Government Portal (`www.tn.gov.in/holiday_list.php`)
 *  4. Optional custom JSON feed URL supplied by the officer/department
 */
object TnPublicHolidayApiClient {

  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()
  }

  private val tamilMonthsByNumber: Map<Int, String> = mapOf(
    1 to "ஜனவரி",
    2 to "பிப்ரவரி",
    3 to "மார்ச்",
    4 to "ஏப்ரல்",
    5 to "மே",
    6 to "ஜூன்",
    7 to "ஜூலை",
    8 to "ஆகஸ்ட்",
    9 to "செப்டம்பர்",
    10 to "அக்டோபர்",
    11 to "நவம்பர்",
    12 to "டிசம்பர்"
  )

  fun fetchOfficialTamilNaduHolidays(
    years: List<Int>,
    customSourceUrl: String = ""
  ): PublicHolidayApiFetchResult {
    val parsedMap = linkedMapOf<String, TnHolidayItem>()
    val sourcesSucceeded = mutableListOf<String>()
    val rawSnippets = mutableListOf<String>()

    val existingTamilLookup = buildExistingTamilNameLookup()

    fun mergeItems(items: List<TnHolidayItem>, sourceLabel: String) {
      if (items.isEmpty()) return
      if (sourceLabel !in sourcesSucceeded) sourcesSucceeded.add(sourceLabel)
      items.forEach { item ->
        val key = "${item.dateIso}_${item.isRestrictedHoliday}"
        val enriched = enrichWithTamilMetadata(item, existingTamilLookup)
        // Prefer richer / TN-specific names if key already exists
        val existing = parsedMap[key]
        if (existing == null || enriched.nameTamil != enriched.nameEnglish) {
          parsedMap[key] = enriched
        }
      }
    }

    // 0. Custom official JSON / HTML URL if provided
    if (customSourceUrl.startsWith("http://") || customSourceUrl.startsWith("https://")) {
      val customBody = executeGet(customSourceUrl.trim())
      if (customBody.isNotBlank()) {
        val host = extractHost(customSourceUrl)
        val parsed = parseCustomHolidayJson(customBody)
          .ifEmpty { parseNagerDateJson(customBody) }
          .ifEmpty { parseTnGovHtmlTable(customBody, years.firstOrNull() ?: LocalDate.now().year) }
        mergeItems(parsed, host)
        rawSnippets.add("[$host]: ${cleanText(customBody).take(1400)}")
      }
    }

    // 1. Nager.Date Public Holidays API for India (National + Tamil Nadu IN-TN)
    for (yr in years.distinct().sorted()) {
      val nagerUrl = "https://date.nager.at/api/v3/PublicHolidays/$yr/IN"
      val body = executeGet(nagerUrl)
      if (body.isNotBlank()) {
        val parsed = parseNagerDateJson(body)
        mergeItems(parsed, "date.nager.at (IN-TN)")
        rawSnippets.add("[Nager.Date $yr IN]: ${cleanText(body).take(1200)}")
      }
    }

    // 2. OpenHolidays API for Tamil Nadu subdivision (IN-TN)
    for (yr in years.distinct().sorted()) {
      val openUrl =
        "https://openholidaysapi.org/PublicHolidays?countryIsoCode=IN&subdivisionCode=IN-TN&validFrom=$yr-01-01&validTo=$yr-12-31&languageIsoCode=EN"
      val body = executeGet(openUrl)
      if (body.isNotBlank()) {
        val parsed = parseOpenHolidaysJson(body)
        mergeItems(parsed, "openholidaysapi.org (IN-TN)")
        if (rawSnippets.size < 3) {
          rawSnippets.add("[OpenHolidays $yr IN-TN]: ${cleanText(body).take(1000)}")
        }
      }
    }

    // 3. Official Tamil Nadu Government Portal HTML Table (`tn.gov.in/holiday_list.php`)
    val tnGovUrl = "https://www.tn.gov.in/holiday_list.php"
    val tnHtml = executeGet(tnGovUrl)
    if (tnHtml.isNotBlank()) {
      val targetYear = years.firstOrNull() ?: LocalDate.now().year
      val parsed = parseTnGovHtmlTable(tnHtml, targetYear)
      mergeItems(parsed, "tn.gov.in (Official G.O.)")
      rawSnippets.add("[tn.gov.in]: ${cleanText(tnHtml).take(1400)}")
    }

    val primarySource = when {
      sourcesSucceeded.isNotEmpty() -> sourcesSucceeded.joinToString(" + ")
      else -> "Offline Seeded TN G.O. Catalog"
    }

    return PublicHolidayApiFetchResult(
      holidays = parsedMap.values.sortedBy { it.dateIso },
      sourcesContacted = sourcesSucceeded,
      primarySourceLabel = primarySource,
      rawSnippetForAi = rawSnippets.joinToString("\n\n").take(4000),
      fetchedFromNetwork = sourcesSucceeded.isNotEmpty()
    )
  }

  /**
   * Parses Nager.Date `/api/v3/PublicHolidays/{year}/IN` JSON payload.
   * Keeps national holidays (`global == true` or null/empty counties) and Tamil Nadu (`IN-TN`) holidays.
   */
  fun parseNagerDateJson(rawJson: String): List<TnHolidayItem> {
    val arr = parseJsonList(rawJson) ?: return emptyList()
    val out = mutableListOf<TnHolidayItem>()
    for (el in arr) {
      val obj = el as? Map<*, *> ?: continue
      val dateIso = obj.str("date").trim()
      val parsedDate = parseIsoDateSafe(dateIso) ?: continue

      val isGlobal = obj.bool("global", default = true)
      val countiesArr = obj.list("counties")
      val appliesToTn = isGlobal || countiesArr.isNullOrEmpty() ||
        countiesArr.any { entry ->
          val code = (entry as? String).orEmpty().uppercase(Locale.US)
          code == "IN-TN" || code == "TN" || code.contains("TAMIL")
        }
      if (!appliesToTn) continue

      val nameEn = obj.str("name").trim().ifBlank { obj.str("localName").trim() }
      if (nameEn.isBlank()) continue

      val localName = obj.str("localName").trim().ifBlank { nameEn }
      val typeStrings = obj.list("types").orEmpty()
        .mapNotNull { (it as? String)?.lowercase(Locale.US) }
      val isRestricted = typeStrings.isNotEmpty() &&
        typeStrings.none { it == "public" || it == "bank" } &&
        typeStrings.any { it == "optional" || it == "observance" || it == "restricted" }

      out.add(
        TnHolidayItem(
          dateIso = dateIso,
          dayOfWeek = parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
          nameEnglish = nameEn,
          nameTamil = localName,
          isRestrictedHoliday = isRestricted,
          monthNameTamil = tamilMonthsByNumber[parsedDate.monthValue].orEmpty()
        )
      )
    }
    return out
  }

  /**
   * Parses OpenHolidays API JSON payload (`openholidaysapi.org/PublicHolidays`).
   */
  fun parseOpenHolidaysJson(rawJson: String): List<TnHolidayItem> {
    val arr = parseJsonList(rawJson) ?: return emptyList()
    val out = mutableListOf<TnHolidayItem>()
    for (el in arr) {
      val obj = el as? Map<*, *> ?: continue
      val dateIso = obj.str("startDate").trim().take(10)
      val parsedDate = parseIsoDateSafe(dateIso) ?: continue

      val nationwide = obj.bool("nationwide", default = true)
      val subs = obj.list("subdivisions")
      val appliesToTn = nationwide || subs.isNullOrEmpty() ||
        subs.any { subEntry ->
          val subObj = subEntry as? Map<*, *>
          val code = subObj?.str("code").orEmpty().uppercase(Locale.US)
          val shortName = subObj?.str("shortName").orEmpty().uppercase(Locale.US)
          code == "IN-TN" || shortName == "TN"
        }
      if (!appliesToTn) continue

      val nameArr = obj.list("name").orEmpty()
      var nameEn = ""
      var nameTa = ""
      for (nEntry in nameArr) {
        val nObj = nEntry as? Map<*, *> ?: continue
        val lang = nObj.str("language").uppercase(Locale.US)
        val text = nObj.str("text").trim()
        if (lang == "EN" && nameEn.isBlank()) nameEn = text
        if (lang == "TA" && nameTa.isBlank()) nameTa = text
        if (nameEn.isBlank() && text.isNotBlank()) nameEn = text
      }
      if (nameEn.isBlank()) continue

      val typeStr = obj.str("type", "Public").lowercase(Locale.US)
      val isRestricted = typeStr.contains("optional") || typeStr.contains("restricted")

      out.add(
        TnHolidayItem(
          dateIso = dateIso,
          dayOfWeek = parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
          nameEnglish = nameEn,
          nameTamil = nameTa.ifBlank { nameEn },
          isRestrictedHoliday = isRestricted,
          monthNameTamil = tamilMonthsByNumber[parsedDate.monthValue].orEmpty()
        )
      )
    }
    return out
  }

  /**
   * Parses custom JSON feeds shaped as either `[{"dateIso"/"date": "...", "nameEnglish"/"name": "..."}]`
   * or `{"holidays": [...]}`.
   */
  fun parseCustomHolidayJson(rawJson: String): List<TnHolidayItem> {
    val parsed = runCatching { SimpleJsonParser(rawJson).parseValue() }.getOrNull() ?: return emptyList()
    val arr: List<*> = when (parsed) {
      is List<*> -> parsed
      is Map<*, *> -> {
        parsed.list("holidays")
          ?: parsed.list("publicHolidays")
          ?: parsed.list("data")
          ?: return emptyList()
      }
      else -> return emptyList()
    }

    val out = mutableListOf<TnHolidayItem>()
    for (el in arr) {
      val obj = el as? Map<*, *> ?: continue
      val dateIso = obj.str("dateIso")
        .ifBlank { obj.str("date") }
        .trim()
        .take(10)
      val parsedDate = parseIsoDateSafe(dateIso) ?: continue
      val nameEn = obj.str("nameEnglish")
        .ifBlank { obj.str("name") }
        .ifBlank { obj.str("title") }
        .trim()
      if (nameEn.isBlank()) continue

      val nameTa = obj.str("nameTamil")
        .ifBlank { obj.str("localName", nameEn) }
        .trim()
      val isRh = if (obj.containsKey("isRestrictedHoliday")) {
        obj.bool("isRestrictedHoliday", default = false)
      } else {
        val typeStr = obj.str("type").lowercase(Locale.US)
        typeStr.contains("rh") || typeStr.contains("rl") || typeStr.contains("restricted")
      }

      out.add(
        TnHolidayItem(
          dateIso = dateIso,
          dayOfWeek = parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
          nameEnglish = nameEn,
          nameTamil = nameTa.ifBlank { nameEn },
          isRestrictedHoliday = isRh,
          monthNameTamil = obj.str("monthNameTamil")
            .ifBlank { tamilMonthsByNumber[parsedDate.monthValue].orEmpty() }
        )
      )
    }
    return out
  }

  private fun parseJsonList(rawJson: String): List<*>? {
    val trimmed = rawJson.trim()
    if (!trimmed.startsWith("[")) return null
    return runCatching { SimpleJsonParser(trimmed).parseValue() as? List<*> }.getOrNull()
  }

  private fun Map<*, *>.str(key: String, default: String = ""): String {
    val v = this[key] ?: return default
    return (v as? String) ?: v.toString()
  }

  private fun Map<*, *>.bool(key: String, default: Boolean): Boolean {
    return when (val v = this[key]) {
      is Boolean -> v
      is String -> v.equals("true", ignoreCase = true)
      else -> default
    }
  }

  private fun Map<*, *>.list(key: String): List<*>? = this[key] as? List<*>

  private class SimpleJsonParser(private val src: String) {
    private var pos = 0

    fun parseValue(): Any? {
      skipWs()
      if (pos >= src.length) return null
      return when (src[pos]) {
        '{' -> parseObject()
        '[' -> parseArray()
        '"' -> parseString()
        't' -> consumeLiteral("true", true)
        'f' -> consumeLiteral("false", false)
        'n' -> consumeLiteral("null", null)
        else -> parseNumber()
      }
    }

    private fun parseObject(): Map<String, Any?> {
      pos++ // '{'
      val map = linkedMapOf<String, Any?>()
      skipWs()
      if (pos < src.length && src[pos] == '}') {
        pos++
        return map
      }
      while (pos < src.length) {
        skipWs()
        val key = parseString()
        skipWs()
        if (pos >= src.length || src[pos] != ':') throw IllegalArgumentException("Expected ':'")
        pos++
        map[key] = parseValue()
        skipWs()
        if (pos >= src.length) break
        when (src[pos]) {
          ',' -> pos++
          '}' -> {
            pos++
            return map
          }
          else -> throw IllegalArgumentException("Unexpected char '${src[pos]}' in object")
        }
      }
      return map
    }

    private fun parseArray(): List<Any?> {
      pos++ // '['
      val list = mutableListOf<Any?>()
      skipWs()
      if (pos < src.length && src[pos] == ']') {
        pos++
        return list
      }
      while (pos < src.length) {
        list.add(parseValue())
        skipWs()
        if (pos >= src.length) break
        when (src[pos]) {
          ',' -> pos++
          ']' -> {
            pos++
            return list
          }
          else -> throw IllegalArgumentException("Unexpected char '${src[pos]}' in array")
        }
      }
      return list
    }

    private fun parseString(): String {
      if (pos >= src.length || src[pos] != '"') throw IllegalArgumentException("Expected '\"'")
      pos++
      val sb = StringBuilder()
      while (pos < src.length) {
        val ch = src[pos++]
        when (ch) {
          '"' -> return sb.toString()
          '\\' -> {
            if (pos >= src.length) break
            when (val esc = src[pos++]) {
              '"', '\\', '/' -> sb.append(esc)
              'b' -> sb.append('\b')
              'f' -> sb.append('\u000C')
              'n' -> sb.append('\n')
              'r' -> sb.append('\r')
              't' -> sb.append('\t')
              'u' -> {
                if (pos + 4 <= src.length) {
                  val hex = src.substring(pos, pos + 4)
                  sb.append(hex.toInt(16).toChar())
                  pos += 4
                }
              }
              else -> sb.append(esc)
            }
          }
          else -> sb.append(ch)
        }
      }
      return sb.toString()
    }

    private fun consumeLiteral(lit: String, value: Any?): Any? {
      if (src.startsWith(lit, pos)) {
        pos += lit.length
        return value
      }
      throw IllegalArgumentException("Unexpected literal at $pos")
    }

    private fun parseNumber(): Number {
      val start = pos
      while (pos < src.length && src[pos] in "0123456789+-.eE") pos++
      val raw = src.substring(start, pos)
      return raw.toLongOrNull() ?: raw.toDouble()
    }

    private fun skipWs() {
      while (pos < src.length && src[pos].isWhitespace()) pos++
    }
  }

  /**
   * Parses official Tamil Nadu Government HTML holiday tables (e.g., `tn.gov.in/holiday_list.php`),
   * matching table rows containing dates in `dd.MM.yyyy`, `dd-MM-yyyy`, `dd/MM/yyyy`, or `YYYY-MM-DD`.
   */
  fun parseTnGovHtmlTable(rawHtml: String, fallbackYear: Int): List<TnHolidayItem> {
    if (rawHtml.isBlank() || !rawHtml.contains("<tr", ignoreCase = true)) return emptyList()
    val rowRegex = Regex("<tr[^>]*>([\\s\\S]*?)</tr>", RegexOption.IGNORE_CASE)
    val cellRegex = Regex("<t[dh][^>]*>([\\s\\S]*?)</t[dh]>", RegexOption.IGNORE_CASE)
    val dmyRegex = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](20\d{2})\b""")
    val isoRegex = Regex("""\b(20\d{2})-(\d{2})-(\d{2})\b""")

    val out = mutableListOf<TnHolidayItem>()
    val seenDates = mutableSetOf<String>()

    rowRegex.findAll(rawHtml).forEach { rowMatch ->
      val cells = cellRegex.findAll(rowMatch.groupValues[1])
        .map { cleanText(it.groupValues[1]) }
        .filter { it.isNotBlank() }
        .toList()
      if (cells.size < 2) return@forEach

      var parsedDate: LocalDate? = null
      var dateCellIdx = -1
      cells.forEachIndexed { idx, cell ->
        if (parsedDate == null) {
          val isoHit = isoRegex.find(cell)
          val dmyHit = dmyRegex.find(cell)
          parsedDate = when {
            isoHit != null -> runCatching { LocalDate.parse(isoHit.value) }.getOrNull()
            dmyHit != null -> runCatching {
              LocalDate.of(
                dmyHit.groupValues[3].toInt(),
                dmyHit.groupValues[2].toInt(),
                dmyHit.groupValues[1].toInt()
              )
            }.getOrNull()
            else -> null
          }
          if (parsedDate != null) dateCellIdx = idx
        }
      }

      val validDate = parsedDate ?: return@forEach
      if (validDate.year < fallbackYear - 1 || validDate.year > 2030) return@forEach

      val nameCandidate = cells.filterIndexed { idx, text ->
        idx != dateCellIdx &&
          text.length >= 3 &&
          !text.matches(Regex("^\\d+\\.?$")) &&
          !isWeekdayOnly(text)
      }.maxByOrNull { it.length } ?: return@forEach

      val dateIso = validDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
      if (seenDates.add(dateIso)) {
        val isRh = nameCandidate.contains("restricted", ignoreCase = true) ||
          nameCandidate.contains("(RH)", ignoreCase = true) ||
          nameCandidate.contains("(RL)", ignoreCase = true)
        out.add(
          TnHolidayItem(
            dateIso = dateIso,
            dayOfWeek = validDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            nameEnglish = nameCandidate,
            nameTamil = nameCandidate,
            isRestrictedHoliday = isRh,
            monthNameTamil = tamilMonthsByNumber[validDate.monthValue].orEmpty()
          )
        )
      }
    }
    return out
  }

  private fun executeGet(url: String): String {
    return runCatching {
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "TNMedLeavePro-HolidaySyncWorker/3.0 (Android; TamilNadu)")
        .header("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
        .build()
      okHttpClient.newCall(request).execute().use { resp ->
        if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
      }
    }.getOrDefault("")
  }

  private fun buildExistingTamilNameLookup(): Map<String, TnHolidayItem> {
    val combined = InitialLeaveSeedData.defaultGovtPublicHolidays2026 +
      InitialLeaveSeedData.defaultRestrictedHolidays2026 +
      TnHolidays2027Catalog.defaultGovtPublicHolidays2027 +
      TnHolidays2027Catalog.defaultRestrictedHolidays2027
    return combined.associateBy { it.dateIso }
  }

  private fun enrichWithTamilMetadata(
    item: TnHolidayItem,
    lookup: Map<String, TnHolidayItem>
  ): TnHolidayItem {
    val matched = lookup[item.dateIso]
    val parsedDate = parseIsoDateSafe(item.dateIso)
    val tamilMonth = item.monthNameTamil.ifBlank {
      matched?.monthNameTamil?.ifBlank {
        parsedDate?.let { tamilMonthsByNumber[it.monthValue] }.orEmpty()
      }.orEmpty()
    }
    val tamilName = if (item.nameTamil.isBlank() || item.nameTamil == item.nameEnglish) {
      matched?.nameTamil?.ifBlank { item.nameEnglish } ?: item.nameEnglish
    } else {
      item.nameTamil
    }
    return item.copy(
      nameTamil = tamilName,
      monthNameTamil = tamilMonth
    )
  }

  private fun parseIsoDateSafe(dateIso: String): LocalDate? {
    if (dateIso.length != 10) return null
    val parsed = runCatching { LocalDate.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
      ?: return null
    return if (parsed.year in 2026..2030) parsed else null
  }

  private fun isWeekdayOnly(text: String): Boolean {
    val clean = text.trim().lowercase(Locale.US)
    return clean in setOf(
      "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
      "mon", "tue", "wed", "thu", "fri", "sat", "sun"
    )
  }

  private fun extractHost(url: String): String {
    return url.removePrefix("https://")
      .removePrefix("http://")
      .substringBefore("/")
  }

  private fun cleanText(raw: String): String {
    return raw
      .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("<[^>]+>"), " ")
      .replace("&amp;", "&")
      .replace("&nbsp;", " ")
      .replace(Regex("\\s+"), " ")
      .trim()
  }
}
