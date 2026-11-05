package com.example.data

object InitialLeaveSeedData {

  data class SeedEntry(
    val leaveType: String,
    val dateIso: String,
    val year: Long,
    val pairedDateIso: String = "",
    val notes: String = "",
    val highlightStatus: String = "NORMAL",
    val isHalfDay: Boolean = false
  )

  // Default Official Tamil Nadu RL List 2026 | RH Leave List 2026 (வரையறுக்கப்பட்ட விடுப்பு)
  val defaultRestrictedHolidays2026: List<TnHolidayItem> = listOf(
    TnHolidayItem("2026-01-03", "Saturday", "Arudhra Darisanam", "ஆருத்ரா தரிசனம்", true, "ஜனவரி"),
    TnHolidayItem("2026-01-14", "Wednesday", "Bhogi", "போகி", true, "ஜனவரி"),
    TnHolidayItem("2026-01-16", "Friday", "Shab-e-Miraj", "ஷபே மீரஜ்", true, "ஜனவரி"),
    TnHolidayItem("2026-02-01", "Sunday", "Thaipoosam (Govt Holiday)", "தைப்பூசம் (அரசு விடுமுறை)", true, "பிப்ரவரி"),
    TnHolidayItem("2026-02-03", "Tuesday", "Shab-e-Barat", "ஷபே பராத்", true, "பிப்ரவரி"),
    TnHolidayItem("2026-02-15", "Sunday", "Maha Shivaratri", "மஹா சிவராத்திரி", true, "பிப்ரவரி"),
    TnHolidayItem("2026-02-18", "Wednesday", "Ash Wednesday", "சாம்பல் புதன்", true, "பிப்ரவரி"),
    TnHolidayItem("2026-02-19", "Thursday", "First Day of Ramadan", "ரம்ஜான் முதல் நாள்", true, "பிப்ரவரி"),
    TnHolidayItem("2026-03-02", "Monday", "Maasi Magam", "மாசி மகம்", true, "மார்ச்"),
    TnHolidayItem("2026-03-04", "Wednesday", "Ayya Vaikunda Swami Birthday", "அய்யா வைகுண்ட சாமி பிறந்த நாள்", true, "மார்ச்"),
    TnHolidayItem("2026-03-16", "Monday", "Shab-e-Qadr", "ஷபே காதர்", true, "மார்ச்"),
    TnHolidayItem("2026-04-02", "Thursday", "Maundy Thursday", "பெரிய வியாழன்", true, "ஏப்ரல்"),
    TnHolidayItem("2026-04-05", "Sunday", "Easter Sunday", "ஈஸ்டர்", true, "ஏப்ரல்"),
    TnHolidayItem("2026-04-14", "Tuesday", "Dr. B.R. Ambedkar Birthday", "அம்பேத்கார் பிறந்த நாள்", true, "ஏப்ரல்"),
    TnHolidayItem("2026-05-01", "Friday", "Buddha Purnima / Chitra Pournami", "புத்த பூர்ணிமா/சித்ரா பௌர்ணமி", true, "மே"),
    TnHolidayItem("2026-05-27", "Wednesday", "Arfa - Hajj Pilgrimage", "அர்பா-ஹஜ் யாத்திரை", true, "மே"),
    TnHolidayItem("2026-06-17", "Wednesday", "Hijri New Year", "ஹிஜிரி வருடப்பிறப்பு", true, "ஜூன்"),
    TnHolidayItem("2026-08-03", "Monday", "Aadi Perukku", "ஆடி பெருக்கு", true, "ஆகஸ்ட்"),
    TnHolidayItem("2026-08-21", "Friday", "Varalakshmi Viratham", "வரலட்சுமி விரதம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2026-08-26", "Wednesday", "Onam", "ஓணம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2026-08-27", "Thursday", "Rig / Yajur Upakarma / Avani Avittam", "ரிக் உபகர்மா/யஜுர் உபகர்மா/ஆவணி அவிட்டம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2026-08-28", "Friday", "Gayathri Japam", "காயத்ரி ஜெபம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2026-09-12", "Saturday", "Sama Upakarma", "சாம உபகர்மா", true, "செப்டம்பர்"),
    TnHolidayItem("2026-09-22", "Tuesday", "Kyrgyz of Mohideen Abdul Kadir", "கர்வீன் ஆப் மொய்தீன் அப்துல் கதர்", true, "செப்டம்பர்"),
    TnHolidayItem("2026-10-10", "Saturday", "Mahalaya Amavasai", "மகாளய அமாவாசை", true, "அக்டோபர்"),
    TnHolidayItem("2026-11-01", "Monday", "All Souls' Day", "கல்லறைத் திருநாள்", true, "நவம்பர்"),
    TnHolidayItem("2026-11-08", "Sunday", "Deepavali Nonbu", "தீபாவளி நோம்பு", true, "நவம்பர்"),
    TnHolidayItem("2026-11-24", "Tuesday", "Karthigai Deepam", "கார்த்திகை தீபம்", true, "நவம்பர்"),
    TnHolidayItem("2026-12-20", "Sunday", "Vaikunta Ekadasi", "வைகுண்ட ஏகாதசி", true, "டிசம்பர்"),
    TnHolidayItem("2026-12-24", "Thursday", "Arudhra Darisanam / Christmas Eve", "ஆருத்ரா தரிசனம் / கிறிஸ்துமஸ் ஈவ்", true, "டிசம்பர்"),
    TnHolidayItem("2026-12-31", "Thursday", "New Year's Eve", "நியூ இயர் ஈவ்", true, "டிசம்பர்")
  )

  // Default Official Tamil Nadu Government Public Holidays (GH) 2026
  val defaultGovtPublicHolidays2026: List<TnHolidayItem> = listOf(
    TnHolidayItem("2026-01-01", "Thursday", "New Year's Day", "ஆங்கிலப் புத்தாண்டு", false, "ஜனவரி"),
    TnHolidayItem("2026-01-15", "Thursday", "Pongal", "பொங்கல்", false, "ஜனவரி"),
    TnHolidayItem("2026-01-16", "Friday", "Thiruvalluvar Day", "திருவள்ளுவர் தினம்", false, "ஜனவரி"),
    TnHolidayItem("2026-01-17", "Saturday", "Uzhavar Thirunal", "உழவர் திருநாள்", false, "ஜனவரி"),
    TnHolidayItem("2026-01-26", "Monday", "Republic Day", "குடியரசு தினம்", false, "ஜனவரி"),
    TnHolidayItem("2026-02-01", "Sunday", "Thaipoosam", "தைப்பூசம்", false, "பிப்ரவரி"),
    TnHolidayItem("2026-03-19", "Thursday", "Telugu New Year / Ugadi", "தெலுங்கு வருடப்பிறப்பு", false, "மார்ச்"),
    TnHolidayItem("2026-03-21", "Saturday", "Ramzan (Id-ul-Fitr)", "ரம்ஜான்", false, "மார்ச்"),
    TnHolidayItem("2026-03-31", "Tuesday", "Mahavir Jayanti", "மகாவீரர் ஜெயந்தி", false, "மார்ச்"),
    TnHolidayItem("2026-04-03", "Friday", "Good Friday", "புனித வெள்ளி", false, "ஏப்ரல்"),
    TnHolidayItem("2026-04-14", "Tuesday", "Tamil New Year / Dr. Ambedkar Birthday", "தமிழ்ப் புத்தாண்டு", false, "ஏப்ரல்"),
    TnHolidayItem("2026-05-01", "Friday", "May Day", "மே தினம்", false, "மே"),
    TnHolidayItem("2026-05-28", "Thursday", "Bakrid (Id-ul-Azha)", "பக்ரீத்", false, "மே"),
    TnHolidayItem("2026-06-26", "Friday", "Muharram", "மொஹரம்", false, "ஜூன்"),
    TnHolidayItem("2026-08-15", "Saturday", "Independence Day", "சுதந்திர தினம்", false, "ஆகஸ்ட்"),
    TnHolidayItem("2026-08-26", "Wednesday", "Milad-un-Nabi", "மிலாடி நபி", false, "ஆகஸ்ட்"),
    TnHolidayItem("2026-09-04", "Friday", "Krishna Jayanthi", "கிருஷ்ண ஜெயந்தி", false, "செப்டம்பர்"),
    TnHolidayItem("2026-09-14", "Monday", "Vinayakar Chathurthi", "விநாயகர் சதுர்த்தி", false, "செப்டம்பர்"),
    TnHolidayItem("2026-10-02", "Friday", "Gandhi Jayanthi", "காந்தி ஜெயந்தி", false, "அக்டோபர்"),
    TnHolidayItem("2026-10-19", "Monday", "Ayutha Pooja", "ஆயுத பூஜை", false, "அக்டோபர்"),
    TnHolidayItem("2026-10-20", "Tuesday", "Vijaya Dasami", "விஜயதசமி", false, "அக்டோபர்"),
    TnHolidayItem("2026-11-08", "Sunday", "Deepavali", "தீபாவளி", false, "நவம்பர்"),
    TnHolidayItem("2026-12-25", "Friday", "Christmas", "கிறிஸ்துமஸ்", false, "டிசம்பர்")
  )

  @Volatile
  var tnRestrictedHolidays2026: List<TnHolidayItem> = defaultRestrictedHolidays2026
    private set

  @Volatile
  var tnGovtPublicHolidays2026: List<TnHolidayItem> = defaultGovtPublicHolidays2026
    private set

  fun updateLiveHolidayCache(
    ghList: List<TnHolidayItem>,
    rhList: List<TnHolidayItem>
  ) {
    if (ghList.isNotEmpty()) {
      tnGovtPublicHolidays2026 = ghList.sortedBy { it.dateIso }
    }
    if (rhList.isNotEmpty()) {
      tnRestrictedHolidays2026 = rhList.sortedBy { it.dateIso }
    }
  }

  fun getAllHistoricalSeeds(): List<SeedEntry> {
    val list = mutableListOf<SeedEntry>()

    // =========================================================================
    // 2023 SECTION (from Screenshot 1 rows 2-23)
    // =========================================================================
    // CL 2023 (12 days)
    listOf(
      "2023-04-12", "2023-06-07", "2023-06-13", "2023-06-30",
      "2023-07-22", "2023-08-12", "2023-09-16", "2023-10-13",
      "2023-10-19", "2023-10-27", "2023-12-17", "2023-12-18"
    ).forEach { date ->
      list.add(SeedEntry("CL", date, 2023L, notes = "Casual Leave 2023"))
    }

    // COL 2023 & In Lieu of 2023 (9 paired entries)
    val col2023 = listOf(
      "2023-01-05" to "2022-10-05",
      "2023-02-03" to "2022-10-08",
      "2023-04-15" to "2022-10-08",
      "2023-04-21" to "2022-11-12",
      "2023-05-25" to "2023-01-26",
      "2023-05-26" to "2023-04-04",
      "2023-06-08" to "2023-04-07",
      "2023-09-23" to "2023-06-10",
      "2023-11-10" to "2023-07-08"
    )
    col2023.forEach { (colDate, inLieuDate) ->
      list.add(SeedEntry("COL", colDate, 2023L, pairedDateIso = inLieuDate, notes = "COL against In-Lieu duty on $inLieuDate"))
      list.add(SeedEntry("IN_LIEU", inLieuDate, 2023L, pairedDateIso = colDate, notes = "Turn Duty worked (availed COL on $colDate)"))
    }

    // PL 2023 (22 entries) & GH Worked 2023 (8 entries)
    val pl2023 = listOf(
      "2023-01-14", "2023-01-16", "2023-01-17", "2023-02-11",
      "2023-03-11", "2023-03-22", "2023-04-08", "2023-04-14",
      "2023-04-22", "2023-05-01", "2023-05-13", "2023-06-29",
      "2023-08-03", "2023-08-15", "2023-09-06", "2023-09-11",
      "2023-10-02", "2023-10-14", "2023-10-23", "2023-10-24",
      "2023-11-11", "2023-11-13"
    )
    val ghWorked2023 = listOf(
      "2022-10-08", "2022-11-12", "2023-01-26", "2023-04-04",
      "2023-04-07", "2023-06-10", "2023-07-08", "2023-12-09"
    )
    pl2023.forEachIndexed { idx, plDate ->
      val pairedGh = ghWorked2023.getOrNull(idx) ?: ""
      list.add(SeedEntry("PL", plDate, 2023L, pairedDateIso = pairedGh, notes = "Public Holiday Leave 2023"))
    }
    ghWorked2023.forEachIndexed { idx, ghDate ->
      val pairedPl = pl2023.getOrNull(idx) ?: ""
      list.add(SeedEntry("GH_WORKED", ghDate, 2023L, pairedDateIso = pairedPl, notes = "GH Hospital Duty Worked"))
    }

    // ML 2023 (6 days)
    listOf("2023-07-03", "2023-07-04", "2023-07-05", "2023-12-04", "2023-12-05", "2023-12-06").forEach { date ->
      list.add(SeedEntry("ML", date, 2023L, notes = "Medical Leave spell"))
    }

    // RH 2023 (1 day)
    list.add(SeedEntry("RH", "2023-08-25", 2023L, notes = "Varalakshmi Viratham RH"))

    // EL 2023 (3 days)
    listOf("2023-01-17", "2023-02-14", "2023-03-11").forEach { date ->
      list.add(SeedEntry("EL", date, 2023L, notes = "Earned Leave 2023"))
    }

    // =========================================================================
    // 2024 SECTION (from Screenshot 1 rows 25-44)
    // =========================================================================
    // CL 2024 (12 days)
    listOf(
      "2024-02-06", "2024-02-22", "2024-04-04", "2024-05-06",
      "2024-05-27", "2024-06-14", "2024-07-01", "2024-07-23",
      "2024-08-17", "2024-09-21", "2024-10-09", "2024-11-16"
    ).forEach { date ->
      list.add(SeedEntry("CL", date, 2024L, notes = "Casual Leave 2024"))
    }

    // COL 2024 & In Lieu of 2024 (8 paired entries)
    val col2024 = listOf(
      "2024-01-02" to "2023-12-09",
      "2024-04-08" to "2024-01-13",
      "2024-06-01" to "2024-01-17",
      "2024-05-31" to "2024-01-25",
      "2024-07-10" to "2024-01-26",
      "2024-08-07" to "2024-02-10",
      "2024-08-16" to "2024-08-11",
      "2024-11-26" to "2024-11-04"
    )
    col2024.forEach { (colDate, inLieuDate) ->
      list.add(SeedEntry("COL", colDate, 2024L, pairedDateIso = inLieuDate, notes = "COL against In-Lieu duty on $inLieuDate"))
      list.add(SeedEntry("IN_LIEU", inLieuDate, 2024L, pairedDateIso = colDate, notes = "Turn Duty worked (availed COL on $colDate)"))
    }

    // PL 2024 (20 entries; 1-1-24 and 1-15-24 highlighted RED in spreadsheet)
    val pl2024 = listOf(
      "2024-01-01", "2024-01-15", "2024-01-16", "2024-03-09",
      "2024-03-29", "2024-04-09", "2024-04-19", "2024-05-01",
      "2024-05-11", "2024-06-08", "2024-07-13", "2024-07-17",
      "2024-08-15", "2024-08-26", "2024-09-14", "2024-09-17",
      "2024-10-11", "2024-10-12", "2024-10-31", "2024-11-01"
    )
    val ghWorked2024 = listOf(
      "2024-01-13", "2024-01-17", "2024-01-25", "2024-01-26",
      "2024-02-10", "2024-04-11", "2024-06-17", "2024-08-11",
      "2024-08-12", "2024-11-04"
    )
    pl2024.forEachIndexed { idx, plDate ->
      val status = if (plDate == "2024-01-01" || plDate == "2024-01-15") "ALERT_RED" else "NORMAL"
      val paired = ghWorked2024.getOrNull(idx) ?: ""
      list.add(SeedEntry("PL", plDate, 2024L, pairedDateIso = paired, notes = if (status == "ALERT_RED") "Highlighted Red in Sheet (Verify GH Credit)" else "Public Holiday Leave 2024", highlightStatus = status))
    }
    ghWorked2024.forEachIndexed { idx, ghDate ->
      val pairedPl = pl2024.getOrNull(idx) ?: ""
      list.add(SeedEntry("GH_WORKED", ghDate, 2024L, pairedDateIso = pairedPl, notes = "GH Hospital Duty Worked 2024"))
    }

    // ML 2024 (4 days)
    listOf("2024-09-26", "2024-09-27", "2024-09-28", "2024-09-29").forEach { date ->
      list.add(SeedEntry("ML", date, 2024L, notes = "Medical Leave spell Sep 2024"))
    }

    // RH 2024 (1 day)
    list.add(SeedEntry("RH", "2024-03-08", 2024L, notes = "Maha Shivaratri RH 2024"))

    // EL 2024 (4 days)
    listOf("2024-01-18", "2024-01-19", "2024-01-20", "2024-01-21").forEach { date ->
      list.add(SeedEntry("EL", date, 2024L, notes = "Earned Leave Pongal Bridge 2024"))
    }

    // =========================================================================
    // 2025 SECTION (from Screenshot 2 rows 46-68)
    // =========================================================================
    // CL 2025 (14 entries in sheet)
    listOf(
      "2025-02-15", "2025-02-17", "2025-03-12", "2025-04-22",
      "2025-05-24", "2025-07-14", "2025-09-13", "2025-10-28",
      "2025-10-27", "2025-11-15", "2025-11-29", "2025-12-20",
      "2025-12-23", "2025-12-30"
    ).forEach { date ->
      list.add(SeedEntry("CL", date, 2025L, notes = "Casual Leave 2025"))
    }

    // COL 2025 (10 entries) & In Lieu of 2025 (9 entries)
    val col2025Dates = listOf(
      "2025-02-03", "2025-04-05", "2025-07-03", "2025-08-14",
      "2025-09-27", "2025-09-08", "2025-10-25", "2025-11-04",
      "2025-11-03", "2026-09-07"
    )
    val inLieu2025Dates = listOf(
      "2024-11-09", "2025-01-01", "2025-01-17", "2025-03-13",
      "2025-04-14", "2025-04-10", "2025-10-21", "2025-09-05",
      "2025-06-14"
    )
    col2025Dates.forEachIndexed { idx, colDate ->
      val paired = inLieu2025Dates.getOrNull(idx) ?: ""
      list.add(SeedEntry("COL", colDate, 2025L, pairedDateIso = paired, notes = if (paired.isNotEmpty()) "COL against $paired" else "COL (Carryover entry)"))
    }
    inLieu2025Dates.forEachIndexed { idx, ilDate ->
      val paired = col2025Dates.getOrNull(idx) ?: ""
      list.add(SeedEntry("IN_LIEU", ilDate, 2025L, pairedDateIso = paired, notes = "In Lieu Turn Duty Worked 2025"))
    }

    // PL 2025 (21 entries)
    listOf(
      "2025-02-08", "2025-04-08", "2025-04-12", "2025-05-15",
      "2025-05-10", "2025-07-12", "2025-06-07", "2025-03-08",
      "2025-02-11", "2025-10-02", "2025-10-11", "2025-10-20",
      "2025-12-25", "2025-12-13", "2025-11-08", "2025-08-27",
      "2025-08-09", "2025-01-11", "2025-01-14", "2025-01-15",
      "2025-01-16"
    ).forEach { plDate ->
      list.add(SeedEntry("PL", plDate, 2025L, notes = "Public Holiday Leave 2025"))
    }

    // GH Worked 2025 (9 entries with color highlights from sheet)
    val gh2025 = listOf(
      Triple("2025-01-01", "ALERT_RED", "New Year GH Worked (Red Highlight)"),
      Triple("2025-01-17", "ALERT_RED", "Pongal GH Worked (Red Highlight)"),
      Triple("2025-04-10", "VERIFIED_GREEN", "Mahavir Jayanti GH Worked (Green Verified)"),
      Triple("2025-04-14", "VERIFIED_GREEN", "Tamil New Year GH Worked (Green Verified)"),
      Triple("2025-04-18", "VERIFIED_GREEN", "Good Friday GH Worked (Green Verified)"),
      Triple("2025-06-14", "PENDING_ORANGE", "Weekend/Holiday Duty (Orange Highlight)"),
      Triple("2025-03-31", "PENDING_ORANGE", "Ramzan GH Worked (Orange Highlight)"),
      Triple("2025-09-05", "PENDING_ORANGE", "Milad-un-Nabi GH Worked (Orange Highlight)"),
      Triple("2025-10-21", "PENDING_ORANGE", "Deepavali Holiday Duty (Orange Highlight)")
    )
    gh2025.forEach { (date, status, note) ->
      list.add(SeedEntry("GH_WORKED", date, 2025L, notes = note, highlightStatus = status))
    }

    // ML 2025 (3 days)
    listOf("2025-07-16", "2025-07-17", "2025-07-18").forEach { date ->
      list.add(SeedEntry("ML", date, 2025L, notes = "Medical Leave spell Jul 2025"))
    }

    // RH 2025 (3 days)
    listOf(
      "2025-08-08" to "Varalakshmi Viratham RH",
      "2025-12-24" to "Christmas Eve RH",
      "2025-12-31" to "New Year's Eve RH"
    ).forEach { (date, note) ->
      list.add(SeedEntry("RH", date, 2025L, notes = note))
    }

    // EL 2025 (7 days)
    listOf(
      "2025-05-26", "2025-05-27", "2025-05-28", "2025-05-29",
      "2025-05-30", "2025-05-31", "2025-06-01"
    ).forEach { date ->
      list.add(SeedEntry("EL", date, 2025L, notes = "Earned Leave Summer Vacation Spell"))
    }

    // =========================================================================
    // 2026 SECTION (from Screenshot 2 rows 70-92 - Current Year)
    // =========================================================================
    // CL 2026 (9 days taken out of 12 -> 3 CL Remaining!)
    listOf(
      "2026-03-12", "2026-04-27", "2026-05-07", "2026-06-27",
      "2026-07-15", "2026-07-31", "2026-09-17", "2026-09-25",
      "2026-10-15"
    ).forEach { date ->
      list.add(SeedEntry("CL", date, 2026L, notes = "Casual Leave 2026"))
    }

    // COL 2026 (7 days) & In Lieu of 2026 (7 days)
    val col2026 = listOf(
      "2026-05-29" to "2026-01-01",
      "2026-05-30" to "2026-01-10",
      "2026-09-07" to "2026-01-26",
      "2026-09-08" to "2026-03-19",
      "2026-11-02" to "2026-04-03",
      "2026-11-03" to "2026-06-26",
      "2026-11-07" to "2026-04-09"
    )
    col2026.forEach { (colDate, ilDate) ->
      list.add(SeedEntry("COL", colDate, 2026L, pairedDateIso = ilDate, notes = "COL paired with In-Lieu duty on $ilDate", highlightStatus = "VERIFIED_GREEN"))
      list.add(SeedEntry("IN_LIEU", ilDate, 2026L, pairedDateIso = colDate, notes = "In-Lieu Turn Duty Worked (availed COL on $colDate)", highlightStatus = "VERIFIED_GREEN"))
    }

    // PL 2026 (23 entries) & GH Worked 2026 (8 entries)
    val pl2026 = listOf(
      "2026-01-15", "2026-01-16", "2026-01-17", "2026-02-18",
      "2026-03-14", "2026-03-21", "2026-03-31", "2026-04-11",
      "2026-04-14", "2026-04-23", "2026-05-09", "2026-05-28",
      "2026-06-13", "2026-07-11", "2026-08-08", "2026-08-15",
      "2026-08-26", "2026-09-12", "2026-09-14", "2026-10-02",
      "2026-10-19", "2026-10-20", "2026-11-09"
    )
    val ghWorked2026 = listOf(
      "2026-01-01", "2026-01-10", "2026-01-26", "2026-03-19",
      "2026-04-03", "2026-06-26", "2026-09-04", "2026-10-10"
    )
    pl2026.forEachIndexed { idx, plDate ->
      val pairedGh = ghWorked2026.getOrNull(idx) ?: ""
      list.add(SeedEntry("PL", plDate, 2026L, pairedDateIso = pairedGh, notes = "Public Holiday Leave 2026"))
    }
    ghWorked2026.forEachIndexed { idx, ghDate ->
      val pairedPl = pl2026.getOrNull(idx) ?: ""
      list.add(SeedEntry("GH_WORKED", ghDate, 2026L, pairedDateIso = pairedPl, notes = "GH Duty Worked at Govt. Erode Medical College"))
    }

    // ML 2026 (3 days)
    listOf("2026-05-20", "2026-05-21", "2026-05-22").forEach { date ->
      list.add(SeedEntry("ML", date, 2026L, notes = "Medical Leave May 2026"))
    }

    // RH 2026 (1 day taken -> 2 RH Remaining!)
    list.add(SeedEntry("RH", "2026-08-03", 2026L, notes = "Aadi Perukku (ஆடி பெருக்கு) Restricted Holiday"))

    // ON_DUTY 2026 (4 days)
    listOf(
      "2026-02-20" to "TN Dr. MGR Medical University Exam / HOD Inspection Duty",
      "2026-02-21" to "TN Dr. MGR Medical University Exam / HOD Inspection Duty",
      "2026-06-25" to "DME Psychiatry HODs State Review / Official OD",
      "2026-06-26" to "DME Psychiatry HODs State Review / Official OD"
    ).forEach { (date, note) ->
      list.add(SeedEntry("ON_DUTY", date, 2026L, notes = note))
    }

    // WEEK_OFF 2026 (1 day)
    list.add(SeedEntry("WEEK_OFF", "2026-06-16", 2026L, notes = "Scheduled Post-Duty Weekly Off"))

    return list
  }
}
