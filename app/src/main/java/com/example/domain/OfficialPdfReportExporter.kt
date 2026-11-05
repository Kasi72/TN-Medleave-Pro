package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.data.ColAuditStatus
import com.example.data.ColGhAuditSummary
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfReportConfig(
  val includeAllYearsAudit: Boolean = false,
  val includeChronologicalLedger: Boolean = true,
  val includeHolidaySummary2026: Boolean = true,
  val officerName: String = "Dr. Kasi, M.D. (Psychiatry)",
  val designation: String = "Professor & Head of Department (HOD), Dept. of Psychiatry",
  val institution: String = "Government Erode Medical College & Hospital, Perundurai, Erode - 638053",
  val departmentGov: String = "GOVERNMENT OF TAMIL NADU • DIRECTORATE OF MEDICAL EDUCATION & RESEARCH"
)

data class GeneratedPdfBundle(
  val pdfFile: File,
  val fileName: String,
  val pageCount: Int,
  val previewBitmaps: List<Bitmap>
)

object OfficialPdfReportExporter {

  private const val A4_WIDTH = 595
  private const val A4_HEIGHT = 842
  private const val MARGIN_H = 36f
  private const val CONTENT_WIDTH = A4_WIDTH - (MARGIN_H * 2f)

  // Palette colors for crisp print-ready PDF
  private val COLOR_NAVY_HEADER = Color.rgb(10, 37, 64)
  private val COLOR_EMERALD_ACCENT = Color.rgb(5, 150, 105)
  private val COLOR_GOLD_SEAL = Color.rgb(180, 83, 9)
  private val COLOR_SLATE_DARK = Color.rgb(30, 41, 59)
  private val COLOR_SLATE_MUTED = Color.rgb(100, 116, 139)
  private val COLOR_TABLE_HEADER_BG = Color.rgb(241, 245, 249)
  private val COLOR_TABLE_ALT_ROW = Color.rgb(248, 250, 252)
  private val COLOR_BORDER_LIGHT = Color.rgb(203, 213, 225)
  private val COLOR_VERIFIED_GREEN = Color.rgb(21, 128, 61)
  private val COLOR_ALERT_RED = Color.rgb(185, 28, 28)
  private val COLOR_AMBER_WARN = Color.rgb(180, 83, 9)

  suspend fun generateOfficialPdfBundle(
    context: Context,
    selectedYear: Long,
    yearSummary: YearCreditSummary,
    summary2026: YearCreditSummary,
    colGhAuditYear: ColGhAuditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    allRecords: List<LeaveRecord>,
    config: PdfReportConfig = PdfReportConfig()
  ): Result<GeneratedPdfBundle> = withContext(Dispatchers.IO) {
    try {
      val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
      val scopeLabel = if (config.includeAllYearsAudit) "2023_2026" else selectedYear.toString()
      val fileName = "GEMCH_Psychiatry_HOD_Leave_Audit_${scopeLabel}.pdf"
      val outFile = File(reportsDir, fileName)

      val pdfDocument = PdfDocument()
      var currentPageNumber = 1
      var currentPage = pdfDocument.startPage(
        PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, currentPageNumber).create()
      )
      var canvas = currentPage.canvas
      var cursorY = drawOfficialFirstPageHeader(
        canvas = canvas,
        selectedYear = selectedYear,
        config = config
      )

      fun startNextPage() {
        drawPageFooter(canvas, currentPageNumber)
        pdfDocument.finishPage(currentPage)
        currentPageNumber++
        currentPage = pdfDocument.startPage(
          PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, currentPageNumber).create()
        )
        canvas = currentPage.canvas
        cursorY = drawContinuationPageHeader(canvas, selectedYear, config, currentPageNumber)
      }

      fun ensureVerticalSpace(requiredHeight: Float) {
        if (cursorY + requiredHeight > A4_HEIGHT - 64f) {
          startNextPage()
        }
      }

      // =========================================================================
      // SECTION I: EXECUTIVE LEAVE CREDIT & STATUTORY QUOTA SUMMARY
      // =========================================================================
      ensureVerticalSpace(190f)
      cursorY = drawSectionBanner(
        canvas = canvas,
        topY = cursorY,
        sectionCode = "SECTION I",
        title = "EXECUTIVE LEAVE CREDIT & STATUTORY QUOTA STATEMENT ($selectedYear)"
      )

      cursorY = drawLeaveQuotaSummaryTable(
        canvas = canvas,
        topY = cursorY,
        summary = yearSummary,
        audit = colGhAuditYear
      )
      cursorY += 14f

      // =========================================================================
      // SECTION II: 3-WAY COL <-> IN LIEU OF <-> GH WORKED RECONCILIATION AUDIT
      // =========================================================================
      val targetAudit = if (config.includeAllYearsAudit) colGhAuditAllYears else colGhAuditYear
      val auditScopeTitle = if (config.includeAllYearsAudit) {
        "CONSOLIDATED 2023–2026 COL ↔ GH WORKED RECONCILIATION AUDIT"
      } else {
        "$selectedYear COL ↔ IN LIEU OF ↔ GH WORKED RECONCILIATION AUDIT"
      }

      ensureVerticalSpace(95f)
      cursorY = drawSectionBanner(
        canvas = canvas,
        topY = cursorY,
        sectionCode = "SECTION II",
        title = auditScopeTitle
      )

      // Audit KPI Strip inside PDF
      cursorY = drawAuditKpiSummaryStrip(canvas, cursorY, targetAudit)
      cursorY += 8f

      // Audit Table Header
      val auditColWidths = floatArrayOf(28f, 68f, 68f, 68f, 185f, 106f)
      val auditHeaders = arrayOf(
        "S.No",
        "COL Date",
        "In Lieu Cited",
        "GH Worked",
        "Holiday / Hospital Duty Particulars",
        "Audit Status"
      )
      ensureVerticalSpace(48f)
      cursorY = drawTableHeaderRow(canvas, cursorY, auditColWidths, auditHeaders)

      targetAudit.items.forEachIndexed { index, item ->
        ensureVerticalSpace(22f)
        if (cursorY == drawContinuationCheckY()) {
          // If we just started a new page, redraw the table header row
          cursorY = drawTableHeaderRow(canvas, cursorY, auditColWidths, auditHeaders)
        }
        val statusText = when (item.status) {
          ColAuditStatus.VERIFIED_MATCH -> "VERIFIED 1:1"
          ColAuditStatus.DOUBLE_CLAIMED_ALERT -> "DUPLICATE DUTY"
          ColAuditStatus.UNPAIRED_COL -> "NEEDS GH PAIR"
          ColAuditStatus.AVAILABLE_GH_CREDIT -> "UNUSED GH CREDIT"
          ColAuditStatus.MISSING_IN_GH_COLUMN -> "IN LIEU CITED"
        }
        val statusColor = when (item.status) {
          ColAuditStatus.VERIFIED_MATCH -> COLOR_VERIFIED_GREEN
          ColAuditStatus.DOUBLE_CLAIMED_ALERT, ColAuditStatus.UNPAIRED_COL -> COLOR_ALERT_RED
          ColAuditStatus.AVAILABLE_GH_CREDIT, ColAuditStatus.MISSING_IN_GH_COLUMN -> COLOR_AMBER_WARN
        }
        val cells = arrayOf(
          (index + 1).toString(),
          item.colDateIso.ifBlank { "—" },
          item.inLieuDateIso.ifBlank { "—" },
          item.ghWorkedDateIso.ifBlank { "—" },
          item.auditExplanation.take(42),
          statusText
        )
        cursorY = drawTableDataRow(
          canvas = canvas,
          topY = cursorY,
          colWidths = auditColWidths,
          cells = cells,
          isAlternate = index % 2 == 1,
          lastColColor = statusColor
        )
      }
      cursorY += 14f

      // =========================================================================
      // SECTION III: CHRONOLOGICAL LEAVE & HOSPITAL DUTY LEDGER
      // =========================================================================
      if (config.includeChronologicalLedger) {
        val yearRecords = allRecords
          .filter { it.year == selectedYear }
          .sortedBy { it.dateIso }

        ensureVerticalSpace(80f)
        cursorY = drawSectionBanner(
          canvas = canvas,
          topY = cursorY,
          sectionCode = "SECTION III",
          title = "CHRONOLOGICAL LEAVE & DUTY LEDGER ($selectedYear • ${yearRecords.size} ENTRIES)"
        )

        val ledgerColWidths = floatArrayOf(30f, 74f, 64f, 78f, 195f, 82f)
        val ledgerHeaders = arrayOf(
          "S.No",
          "Date (ISO)",
          "Category",
          "Paired Duty",
          "Official Reason / Duty Particulars",
          "Verification"
        )
        ensureVerticalSpace(44f)
        cursorY = drawTableHeaderRow(canvas, cursorY, ledgerColWidths, ledgerHeaders)

        yearRecords.forEachIndexed { idx, rec ->
          if (cursorY + 20f > A4_HEIGHT - 64f) {
            startNextPage()
            cursorY = drawTableHeaderRow(canvas, cursorY, ledgerColWidths, ledgerHeaders)
          }
          val statusLabel = when (rec.highlightStatus) {
            "VERIFIED_GREEN" -> "VERIFIED"
            "ALERT_RED" -> "UNPAIRED"
            "PENDING_ORANGE" -> "PENDING"
            else -> "APPROVED"
          }
          val statusColor = when (rec.highlightStatus) {
            "VERIFIED_GREEN" -> COLOR_VERIFIED_GREEN
            "ALERT_RED" -> COLOR_ALERT_RED
            "PENDING_ORANGE" -> COLOR_AMBER_WARN
            else -> COLOR_SLATE_DARK
          }
          val rowCells = arrayOf(
            (idx + 1).toString(),
            rec.dateIso,
            rec.category.shortLabel,
            rec.pairedDateIso.ifBlank { "—" },
            rec.notes.ifBlank { rec.category.fullTitle }.take(44),
            statusLabel
          )
          cursorY = drawTableDataRow(
            canvas = canvas,
            topY = cursorY,
            colWidths = ledgerColWidths,
            cells = rowCells,
            isAlternate = idx % 2 == 1,
            lastColColor = statusColor
          )
        }
        cursorY += 14f
      }

      // =========================================================================
      // SECTION IV: 2026 TAMIL NADU GOVERNMENT HOLIDAYS (GH) & RESTRICTED HOLIDAYS (RH)
      // =========================================================================
      if (config.includeHolidaySummary2026) {
        ensureVerticalSpace(110f)
        cursorY = drawSectionBanner(
          canvas = canvas,
          topY = cursorY,
          sectionCode = "SECTION IV",
          title = "2026 TAMIL NADU GOVT. HOLIDAYS (GH) & RESTRICTED HOLIDAYS (RH) SUMMARY"
        )

        val logged2026ByDate = allRecords
          .filter { it.year == 2026L }
          .associateBy { it.dateIso }

        val holColWidths = floatArrayOf(72f, 68f, 62f, 215f, 106f)
        val holHeaders = arrayOf(
          "Date",
          "Day",
          "Type",
          "Festival / Government Notification Name",
          "HOD Ledger Status"
        )
        ensureVerticalSpace(44f)
        cursorY = drawTableHeaderRow(canvas, cursorY, holColWidths, holHeaders)

        val combinedHolidays = (
          InitialLeaveSeedData.tnGovtPublicHolidays2026 +
            InitialLeaveSeedData.tnRestrictedHolidays2026
          ).sortedBy { it.dateIso }

        combinedHolidays.forEachIndexed { idx, hol ->
          if (cursorY + 20f > A4_HEIGHT - 64f) {
            startNextPage()
            cursorY = drawTableHeaderRow(canvas, cursorY, holColWidths, holHeaders)
          }
          val loggedRecord = logged2026ByDate[hol.dateIso]
          val ledgerStatus = when {
            loggedRecord != null -> "LOGGED: ${loggedRecord.category.shortLabel}"
            hol.isRestrictedHoliday -> "ELIGIBLE RH (RL)"
            else -> "PUBLIC HOLIDAY"
          }
          val statusColor = when {
            loggedRecord != null -> COLOR_EMERALD_ACCENT
            hol.isRestrictedHoliday -> COLOR_AMBER_WARN
            else -> COLOR_SLATE_MUTED
          }
          val cells = arrayOf(
            hol.dateIso,
            hol.dayOfWeek,
            if (hol.isRestrictedHoliday) "TN RH (RL)" else "TN GH",
            hol.nameEnglish.take(46),
            ledgerStatus
          )
          cursorY = drawTableDataRow(
            canvas = canvas,
            topY = cursorY,
            colWidths = holColWidths,
            cells = cells,
            isAlternate = idx % 2 == 1,
            lastColColor = statusColor
          )
        }
        cursorY += 16f
      }

      // =========================================================================
      // OFFICIAL ADMINISTRATIVE SIGN-OFF & COUNTERSIGNATURE BLOCK
      // =========================================================================
      ensureVerticalSpace(115f)
      drawOfficialSignatureBlock(canvas, cursorY, config)

      // Finish last page
      drawPageFooter(canvas, currentPageNumber)
      pdfDocument.finishPage(currentPage)

      FileOutputStream(outFile).use { outStream ->
        pdfDocument.writeTo(outStream)
      }
      pdfDocument.close()

      val bitmaps = renderPdfPagesToBitmaps(outFile)
      Result.success(
        GeneratedPdfBundle(
          pdfFile = outFile,
          fileName = fileName,
          pageCount = currentPageNumber,
          previewBitmaps = bitmaps
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun drawContinuationCheckY(): Float = 94f

  private fun drawOfficialFirstPageHeader(
    canvas: Canvas,
    selectedYear: Long,
    config: PdfReportConfig
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Top Navy Banner
    paint.color = COLOR_NAVY_HEADER
    canvas.drawRoundRect(
      RectF(MARGIN_H, 28f, A4_WIDTH - MARGIN_H, 114f),
      10f,
      10f,
      paint
    )

    // Gold top accent stripe inside banner
    paint.color = COLOR_EMERALD_ACCENT
    canvas.drawRoundRect(
      RectF(MARGIN_H, 28f, A4_WIDTH - MARGIN_H, 34f),
      4f,
      4f,
      paint
    )

    // Official Crest Circle on Left
    paint.color = Color.argb(38, 52, 211, 153)
    canvas.drawCircle(MARGIN_H + 34f, 72f, 22f, paint)
    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("TN DME", MARGIN_H + 34f, 70f, paint)
    paint.textSize = 7.5f
    canvas.drawText("GOVT", MARGIN_H + 34f, 80f, paint)

    // Header Center Text
    val centerX = A4_WIDTH / 2f + 12f
    paint.color = Color.rgb(110, 231, 183)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 8f
    canvas.drawText(config.departmentGov, centerX, 49f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 11.5f
    canvas.drawText(config.institution.uppercase(), centerX, 66f, paint)

    paint.color = Color.rgb(224, 242, 254)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 10f
    canvas.drawText(
      "DEPARTMENT OF PSYCHIATRY • OFFICIAL LEAVE AUDIT & HOLIDAY STATEMENT",
      centerX,
      83f,
      paint
    )

    paint.color = Color.rgb(186, 230, 253)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 7.8f
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    val refNo = "Ref. No: GEMCH/PSYCH/HOD/AUDIT/$selectedYear • Date: $todayFormatted"
    canvas.drawText(refNo, centerX, 99f, paint)

    // 2. Officer & Statutory Metadata Box
    val metaTop = 122f
    val metaBottom = 176f
    paint.textAlign = Paint.Align.LEFT
    paint.color = COLOR_TABLE_HEADER_BG
    canvas.drawRoundRect(
      RectF(MARGIN_H, metaTop, A4_WIDTH - MARGIN_H, metaBottom),
      8f,
      8f,
      paint
    )
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = COLOR_BORDER_LIGHT
    canvas.drawRoundRect(
      RectF(MARGIN_H, metaTop, A4_WIDTH - MARGIN_H, metaBottom),
      8f,
      8f,
      paint
    )
    paint.style = Paint.Style.FILL

    // Left column metadata
    paint.color = COLOR_SLATE_MUTED
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 7.5f
    canvas.drawText("OFFICER NAME & DESIGNATION:", MARGIN_H + 12f, metaTop + 15f, paint)
    paint.color = COLOR_SLATE_DARK
    paint.textSize = 9f
    canvas.drawText("${config.officerName} — ${config.designation}", MARGIN_H + 12f, metaTop + 28f, paint)

    paint.color = COLOR_SLATE_MUTED
    paint.textSize = 7.5f
    canvas.drawText(
      "STATUTORY FRAMEWORK: Tamil Nadu Leave Rules, 1933 & Fundamental Rules (FR Annexure VII)",
      MARGIN_H + 12f,
      metaTop + 43f,
      paint
    )

    // Right column badge
    val badgeRight = A4_WIDTH - MARGIN_H - 12f
    paint.textAlign = Paint.Align.RIGHT
    paint.color = COLOR_EMERALD_ACCENT
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 8.5f
    val scopeStr = if (config.includeAllYearsAudit) "AUDIT PERIOD: 2023–2026" else "AUDIT YEAR: $selectedYear"
    canvas.drawText(scopeStr, badgeRight, metaTop + 18f, paint)

    paint.color = COLOR_SLATE_DARK
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 7.5f
    canvas.drawText("Cadre: Tamil Nadu Medical Service (TNMS)", badgeRight, metaTop + 31f, paint)
    canvas.drawText("Submitted to: Office of the Dean, GEMCH", badgeRight, metaTop + 43f, paint)

    paint.textAlign = Paint.Align.LEFT
    return metaBottom + 14f
  }

  private fun drawContinuationPageHeader(
    canvas: Canvas,
    selectedYear: Long,
    config: PdfReportConfig,
    pageNum: Int
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = COLOR_NAVY_HEADER
    canvas.drawRoundRect(
      RectF(MARGIN_H, 26f, A4_WIDTH - MARGIN_H, 62f),
      6f,
      6f,
      paint
    )

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 9.5f
    canvas.drawText(
      "GOVT. ERODE MEDICAL COLLEGE & HOSPITAL • DEPT. OF PSYCHIATRY (HOD LEAVE AUDIT)",
      MARGIN_H + 12f,
      43f,
      paint
    )

    paint.color = Color.rgb(186, 230, 253)
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 7.5f
    canvas.drawText(
      "${config.officerName} • Audit Year: $selectedYear • Continuation Sheet (Page $pageNum)",
      MARGIN_H + 12f,
      55f,
      paint
    )

    return 74f
  }

  private fun drawSectionBanner(
    canvas: Canvas,
    topY: Float,
    sectionCode: String,
    title: String
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = COLOR_NAVY_HEADER
    canvas.drawRoundRect(
      RectF(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + 22f),
      5f,
      5f,
      paint
    )

    paint.color = Color.rgb(52, 211, 153)
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = 8f
    canvas.drawText(sectionCode, MARGIN_H + 10f, topY + 14.5f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 8.5f
    canvas.drawText(title, MARGIN_H + 68f, topY + 14.5f, paint)

    return topY + 28f
  }

  private fun drawLeaveQuotaSummaryTable(
    canvas: Canvas,
    topY: Float,
    summary: YearCreditSummary,
    audit: ColGhAuditSummary
  ): Float {
    val colWidths = floatArrayOf(115f, 155f, 75f, 82f, 96f)
    val headers = arrayOf(
      "Leave Category",
      "TN Statutory Rule / Quota",
      "Availed",
      "Remaining",
      "Office Audit Note"
    )
    var y = drawTableHeaderRow(canvas, topY, colWidths, headers)

    val rows = listOf(
      arrayOf(
        "Casual Leave (CL)",
        "FR Annexure VII • 12 Days/Yr (≤10d stretch)",
        "${formatCleanDouble(summary.clTaken)} Days",
        "${formatCleanDouble(summary.clRemaining)} Days",
        if (summary.clRemaining >= 0) "WITHIN QUOTA" else "EXCEEDED"
      ),
      arrayOf(
        "Restricted Holiday (RH)",
        "TN RL List • 3 Days / Calendar Year",
        "${summary.rhTaken} Days",
        "${summary.rhRemaining} Days",
        if (summary.rhRemaining >= 0) "WITHIN QUOTA" else "EXCEEDED"
      ),
      arrayOf(
        "Compensatory Off (COL)",
        "Against Hospital GH Turn Duty (≤6m)",
        "${summary.colTaken} Days",
        "${audit.availableUnusedGhCount} Unused GH",
        if (audit.unaccountedColCount == 0 && audit.doubleClaimedCount == 0) {
          "100% PAIRED"
        } else {
          "${audit.unaccountedColCount + audit.doubleClaimedCount} NEEDS PAIR"
        }
      ),
      arrayOf(
        "GH Worked (Turn Duty)",
        "Declared TN Public Holiday Hospital Duty",
        "${summary.ghWorked} Duties",
        "${audit.availableUnusedGhCount} Unclaimed",
        "VERIFIED DUTY"
      ),
      arrayOf(
        "Earned Leave (EL)",
        "TN Leave Rule 8 • 15d / Half-Yr (Max 240d)",
        "${summary.elTaken} Days",
        "Accruing",
        "SERVICE LEDGER"
      ),
      arrayOf(
        "Medical Leave (ML / UEL)",
        "TN Leave Rule 15 • Up to 540d in Service",
        "${summary.mlTaken} Days",
        "Within Limit",
        "MC VERIFIED"
      ),
      arrayOf(
        "On Duty (OD) & Week Off",
        "DME / Univ. Exam / Post-Call Weekly Off",
        "${summary.onDutyTaken} OD / ${summary.weekOffTaken} WO",
        "N/A",
        "EXEMPT DUTY"
      )
    )

    rows.forEachIndexed { idx, row ->
      val lastColColor = if (row[4].contains("NEEDS") || row[4].contains("EXCEEDED")) {
        COLOR_ALERT_RED
      } else {
        COLOR_VERIFIED_GREEN
      }
      y = drawTableDataRow(
        canvas = canvas,
        topY = y,
        colWidths = colWidths,
        cells = row,
        isAlternate = idx % 2 == 1,
        lastColColor = lastColColor
      )
    }

    return y
  }

  private fun drawAuditKpiSummaryStrip(
    canvas: Canvas,
    topY: Float,
    audit: ColGhAuditSummary
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val boxHeight = 32f
    val gap = 8f
    val boxWidth = (CONTENT_WIDTH - (gap * 3f)) / 4f

    val kpis = listOf(
      Triple("Total COL Availed", "${audit.totalColTaken} Days", COLOR_NAVY_HEADER),
      Triple("Total GH Worked", "${audit.totalGhWorked} Duties", COLOR_VERIFIED_GREEN),
      Triple(
        "1:1 Verified Pairs",
        "${audit.fullyAccountedColCount} Matched",
        COLOR_EMERALD_ACCENT
      ),
      Triple(
        "Unused GH Credits",
        "${audit.availableUnusedGhCount} Available",
        COLOR_AMBER_WARN
      )
    )

    kpis.forEachIndexed { idx, (label, value, accent) ->
      val left = MARGIN_H + idx * (boxWidth + gap)
      val rect = RectF(left, topY, left + boxWidth, topY + boxHeight)
      paint.style = Paint.Style.FILL
      paint.color = COLOR_TABLE_HEADER_BG
      canvas.drawRoundRect(rect, 6f, 6f, paint)

      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 1f
      paint.color = COLOR_BORDER_LIGHT
      canvas.drawRoundRect(rect, 6f, 6f, paint)
      paint.style = Paint.Style.FILL

      paint.color = COLOR_SLATE_MUTED
      paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      paint.textSize = 7f
      canvas.drawText(label, left + 8f, topY + 12f, paint)

      paint.color = accent
      paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      paint.textSize = 9f
      canvas.drawText(value, left + 8f, topY + 25f, paint)
    }

    return topY + boxHeight
  }

  private fun drawTableHeaderRow(
    canvas: Canvas,
    topY: Float,
    colWidths: FloatArray,
    headers: Array<String>
  ): Float {
    val rowHeight = 20f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.style = Paint.Style.FILL
    paint.color = COLOR_TABLE_HEADER_BG
    canvas.drawRect(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + rowHeight, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    paint.color = COLOR_BORDER_LIGHT
    canvas.drawRect(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + rowHeight, paint)

    paint.style = Paint.Style.FILL
    paint.color = COLOR_NAVY_HEADER
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 7.5f

    var x = MARGIN_H
    headers.forEachIndexed { index, text ->
      canvas.drawText(text, x + 5f, topY + 13f, paint)
      x += colWidths[index]
      if (index < headers.lastIndex) {
        paint.color = COLOR_BORDER_LIGHT
        canvas.drawLine(x, topY, x, topY + rowHeight, paint)
        paint.color = COLOR_NAVY_HEADER
      }
    }

    return topY + rowHeight
  }

  private fun drawTableDataRow(
    canvas: Canvas,
    topY: Float,
    colWidths: FloatArray,
    cells: Array<String>,
    isAlternate: Boolean,
    lastColColor: Int = COLOR_SLATE_DARK
  ): Float {
    val rowHeight = 18f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (isAlternate) {
      paint.style = Paint.Style.FILL
      paint.color = COLOR_TABLE_ALT_ROW
      canvas.drawRect(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + rowHeight, paint)
    }

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.6f
    paint.color = COLOR_BORDER_LIGHT
    canvas.drawRect(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + rowHeight, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = 7.4f

    var x = MARGIN_H
    cells.forEachIndexed { index, text ->
      if (index == cells.lastIndex) {
        paint.color = lastColColor
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      } else {
        paint.color = COLOR_SLATE_DARK
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      }
      canvas.drawText(text, x + 5f, topY + 12.2f, paint)
      x += colWidths[index]
      if (index < cells.lastIndex) {
        paint.color = COLOR_BORDER_LIGHT
        canvas.drawLine(x, topY, x, topY + rowHeight, paint)
      }
    }

    return topY + rowHeight
  }

  private fun drawOfficialSignatureBlock(
    canvas: Canvas,
    topY: Float,
    config: PdfReportConfig
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val blockHeight = 86f
    val rect = RectF(MARGIN_H, topY, A4_WIDTH - MARGIN_H, topY + blockHeight)

    paint.style = Paint.Style.FILL
    paint.color = COLOR_TABLE_ALT_ROW
    canvas.drawRoundRect(rect, 8f, 8f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.9f
    paint.color = COLOR_BORDER_LIGHT
    canvas.drawRoundRect(rect, 8f, 8f, paint)
    paint.style = Paint.Style.FILL

    paint.color = COLOR_NAVY_HEADER
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textSize = 7.5f
    canvas.drawText(
      "DECLARATION & ADMINISTRATIVE VERIFICATION (FOR MEDICAL COLLEGE OFFICE USE):",
      MARGIN_H + 12f,
      topY + 14f,
      paint
    )

    paint.color = COLOR_SLATE_MUTED
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 7f
    canvas.drawText(
      "Certified that the Casual Leave (CL), Restricted Holidays (RH), and Compensatory Off Leaves (COL paired against GH Worked)",
      MARGIN_H + 12f,
      topY + 25f,
      paint
    )
    canvas.drawText(
      "detailed above are in accordance with the Tamil Nadu Leave Rules, 1933 and Fundamental Rules (Annexure VII).",
      MARGIN_H + 12f,
      topY + 34f,
      paint
    )

    val colW = CONTENT_WIDTH / 3f
    val sigY = topY + 64f
    val labels = listOf(
      Pair(config.officerName, "Professor & HOD, Dept. of Psychiatry"),
      Pair("Office Superintendent / JAO", "Establishment & Leave Section, GEMCH"),
      Pair("The Dean", "Govt. Erode Medical College & Hospital")
    )

    labels.forEachIndexed { idx, (line1, line2) ->
      val colLeft = MARGIN_H + (idx * colW) + 12f
      paint.color = COLOR_BORDER_LIGHT
      canvas.drawLine(colLeft, sigY - 12f, colLeft + colW - 28f, sigY - 12f, paint)

      paint.color = COLOR_SLATE_DARK
      paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      paint.textSize = 7.5f
      canvas.drawText(line1, colLeft, sigY, paint)

      paint.color = COLOR_SLATE_MUTED
      paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      paint.textSize = 6.8f
      canvas.drawText(line2, colLeft, sigY + 10f, paint)
    }
  }

  private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val footerY = A4_HEIGHT - 24f

    paint.color = COLOR_BORDER_LIGHT
    paint.strokeWidth = 0.7f
    canvas.drawLine(MARGIN_H, footerY - 10f, A4_WIDTH - MARGIN_H, footerY - 10f, paint)

    paint.color = COLOR_SLATE_MUTED
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 7f
    canvas.drawText(
      "TN MedLeave Pro • Official Leave & COL-GH Audit Report • Govt. Erode Medical College & Hospital",
      MARGIN_H,
      footerY,
      paint
    )

    paint.textAlign = Paint.Align.RIGHT
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    canvas.drawText("Page $pageNumber", A4_WIDTH - MARGIN_H, footerY, paint)
    paint.textAlign = Paint.Align.LEFT
  }

  private fun renderPdfPagesToBitmaps(pdfFile: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    pfd.use { descriptor ->
      val renderer = PdfRenderer(descriptor)
      renderer.use { pdf ->
        for (i in 0 until pdf.pageCount) {
          val page = pdf.openPage(i)
          // Render at 2x scale for crisp readability in preview dialog
          val width = page.width * 2
          val height = page.height * 2
          val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(bitmap)
          canvas.drawColor(Color.WHITE)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          bitmaps.add(bitmap)
        }
      }
    }
    return bitmaps
  }

  suspend fun writePdfToUri(context: Context, sourceFile: File, targetUri: Uri): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
          sourceFile.inputStream().use { input ->
            input.copyTo(out)
          }
        } ?: throw IllegalStateException("Unable to open destination URI for writing PDF.")
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  fun sharePdfReport(context: Context, pdfFile: File, selectedYear: Long) {
    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(
        Intent.EXTRA_SUBJECT,
        "Official Leave & COL-GH Worked Audit Report ($selectedYear) - HOD Psychiatry, GEMCH"
      )
      putExtra(
        Intent.EXTRA_TEXT,
        "Please find attached the official Leave & COL-GH Worked Reconciliation Audit Statement and 2026 Holiday Summary for the Department of Psychiatry, Government Erode Medical College & Hospital."
      )
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
      Intent.createChooser(shareIntent, "Submit / Share Official Audit PDF Report")
    )
  }

  private fun formatCleanDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
      value.toInt().toString()
    } else {
      String.format(java.util.Locale.US, "%.1f", value)
    }
  }
}
