package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Stores picked signed sanction-slip photos securely inside app-private internal storage
 * (`filesDir/sanction_slips/`) via Android's zero-permission Photo Picker so slips survive reboots
 * without requesting broad external storage permissions.
 */
object SanctionSlipVault {

  private const val VAULT_DIR = "sanction_slips"

  fun savePickedImageToPrivateVault(
    context: Context,
    recordId: String,
    sourceUri: Uri
  ): Result<String> {
    return runCatching {
      val dir = File(context.filesDir, VAULT_DIR).apply { if (!exists()) mkdirs() }
      val safeId = recordId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(48)
      val fileName = "slip_${safeId}_${System.currentTimeMillis()}.jpg"
      val outFile = File(dir, fileName)

      val resolver = context.contentResolver
      // Decode bounds first to keep memory footprint low on high-MP phone cameras
      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
      val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
      val sampleSize = when {
        maxDim > 3200 -> 4
        maxDim > 1800 -> 2
        else -> 1
      }

      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val bitmap = resolver.openInputStream(sourceUri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOpts)
      } ?: throw IllegalStateException("Unable to decode selected sanction slip image")

      FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, fos)
      }
      bitmap.recycle()
      fileName
    }
  }

  fun resolveSlipFile(context: Context, fileName: String): File? {
    if (fileName.isBlank()) return null
    val safeName = File(fileName).name
    val file = File(File(context.filesDir, VAULT_DIR), safeName)
    return if (file.exists() && file.length() > 0L) file else null
  }

  fun deleteSlipFile(context: Context, fileName: String) {
    resolveSlipFile(context, fileName)?.delete()
  }
}

/**
 * Zero-permission native Android bridges for:
 *  - 1-Tap Google / Device Calendar event creation (`CalendarContract.Events.CONTENT_URI`)
 *  - Shareable `.ics` calendar file export via `FileProvider`
 *  - 1-Tap WhatsApp / System Share Sheet clinical handover dispatch
 *  - Printable Official TN Govt Joining Report PDF export
 */
object CalendarAndHandoverBridge {

  private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun openDeviceCalendarInsert(
    context: Context,
    title: String,
    startDateIso: String,
    endDateIso: String,
    description: String,
    location: String
  ): Result<Unit> {
    return runCatching {
      val zone = ZoneId.systemDefault()
      val start = runCatching { LocalDate.parse(startDateIso, isoFmt) }.getOrElse { LocalDate.now() }
      val end = runCatching { LocalDate.parse(endDateIso, isoFmt) }.getOrElse { start }
      val beginMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
      val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

      val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  fun shareIcsCalendarFile(
    context: Context,
    title: String,
    startDateIso: String,
    endDateIso: String,
    description: String,
    location: String
  ): Result<File> {
    return runCatching {
      val icsText = JoiningReportAndShareGenerator.buildIcsCalendarContent(
        title = title,
        startDateIso = startDateIso,
        endDateIso = endDateIso,
        description = description,
        location = location
      )
      val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
      val file = File(exportDir, "TN_MedLeave_${startDateIso}.ics")
      file.writeText(icsText, Charsets.UTF_8)

      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(shareIntent, "Add Leave to Calendar (.ics)").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      file
    }
  }

  fun shareClinicalHandover(
    context: Context,
    message: String,
    preferWhatsApp: Boolean = true
  ): Result<Unit> {
    return runCatching {
      if (preferWhatsApp) {
        val waIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          `package` = "com.whatsapp"
          putExtra(Intent.EXTRA_TEXT, message)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val canLaunchWa = waIntent.resolveActivity(context.packageManager) != null
        if (canLaunchWa) {
          context.startActivity(waIntent)
          return@runCatching
        }
      }
      val fallback = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Clinical Charge Handover — TN MedLeave Pro")
        putExtra(Intent.EXTRA_TEXT, message)
      }
      val chooser = Intent.createChooser(fallback, "Share Clinical Handover").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    }
  }

  fun exportAndShareJoiningReportPdf(
    context: Context,
    fileSlug: String,
    reportText: String
  ): Result<File> {
    return runCatching {
      val pdf = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
      val page = pdf.startPage(pageInfo)
      val canvas = page.canvas

      val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 37, 64)
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 36, 48)
        textSize = 11f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
      }
      val linePaint = Paint().apply {
        color = Color.rgb(14, 116, 144)
        strokeWidth = 1.5f
      }

      val leftMargin = 48f
      val maxWidth = 595f - 96f
      var y = 56f

      val rawLines = reportText.lines()
      rawLines.forEachIndexed { idx, line ->
        if (idx <= 1) {
          canvas.drawText(line, leftMargin, y, titlePaint)
          y += 20f
        } else if (line.startsWith("═══")) {
          canvas.drawLine(leftMargin, y - 6f, 595f - leftMargin, y - 6f, linePaint)
          y += 14f
        } else if (line.isBlank()) {
          y += 12f
        } else {
          // Word-wrap long paragraphs cleanly inside A4 margins
          val words = line.split(" ")
          var current = ""
          for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (bodyPaint.measureText(candidate) <= maxWidth) {
              current = candidate
            } else {
              canvas.drawText(current, leftMargin, y, bodyPaint)
              y += 16f
              current = word
            }
          }
          if (current.isNotEmpty()) {
            canvas.drawText(current, leftMargin, y, bodyPaint)
            y += 16f
          }
        }
      }

      pdf.finishPage(page)

      val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
      val safeSlug = fileSlug.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
      val outFile = File(exportDir, "TN_Joining_Report_$safeSlug.pdf")
      FileOutputStream(outFile).use { pdf.writeTo(it) }
      pdf.close()

      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Official TN Govt Joining Report")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(shareIntent, "Share / Print Joining Report PDF").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      outFile
    }
  }
}
