package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.GeneratedPdfBundle
import com.example.domain.OfficialPdfReportExporter
import com.example.domain.PdfReportConfig
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.StatusAlertRed
import kotlinx.coroutines.launch

@Composable
fun OfficialPdfExportBannerCard(
  selectedYear: Long,
  onOpenPdfExportModal: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color.Transparent,
    modifier = modifier
      .fillMaxWidth()
      .testTag("official_pdf_export_card")
  ) {
    Box(
      modifier = Modifier
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              Color(0xFF0A2540),
              Color(0xFF1E3A5F),
              Color(0xFF065F46)
            )
          ),
          shape = RoundedCornerShape(24.dp)
        )
        .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF34D399).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "DEAN'S OFFICE SUBMISSION • GEMCH",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF34D399),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Official Leave Audit & Holiday PDF",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.14f)
          ) {
            Text(
              text = "A4 PDF",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF6EE7B7),
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Text(
          text = "Generates a formatted A4 multi-page PDF with the Government Erode Medical College & Hospital header, $selectedYear Leave Credit Summary, 1:1 COL ↔ GH Worked Reconciliation Table, 2026 Holiday Summary, and Dean/HOD signature blocks.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFE0F2FE),
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        Button(
          onClick = onOpenPdfExportModal,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981),
            contentColor = Color(0xFF052E16)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("open_pdf_export_modal_button")
        ) {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Preview & Export Official Audit PDF ($selectedYear)",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun OfficialPdfExportDialog(
  state: LeaveUiState.Success,
  onDismiss: () -> Unit,
  onExportSuccessMessage: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var includeAllYearsAudit by remember { mutableStateOf(false) }
  var includeChronologicalLedger by remember { mutableStateOf(true) }
  var includeHolidaySummary2026 by remember { mutableStateOf(true) }

  var isGenerating by remember { mutableStateOf(true) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var generatedBundle by remember { mutableStateOf<GeneratedPdfBundle?>(null) }
  var localSaveStatus by remember { mutableStateOf<String?>(null) }

  val saveDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { uri ->
    val bundle = generatedBundle
    if (uri != null && bundle != null) {
      scope.launch {
        val result = OfficialPdfReportExporter.writePdfToUri(
          context = context,
          sourceFile = bundle.pdfFile,
          targetUri = uri
        )
        result.onSuccess {
          val msg = "Saved ${bundle.fileName} (${bundle.pageCount} A4 pages) to device!"
          localSaveStatus = msg
          onExportSuccessMessage(msg)
        }.onFailure { err ->
          errorText = "Failed to save PDF: ${err.localizedMessage}"
        }
      }
    }
  }

  LaunchedEffect(
    state.selectedYear,
    state.allRecords,
    includeAllYearsAudit,
    includeChronologicalLedger,
    includeHolidaySummary2026
  ) {
    isGenerating = true
    errorText = null
    val config = PdfReportConfig(
      includeAllYearsAudit = includeAllYearsAudit,
      includeChronologicalLedger = includeChronologicalLedger,
      includeHolidaySummary2026 = includeHolidaySummary2026
    )
    val res = OfficialPdfReportExporter.generateOfficialPdfBundle(
      context = context,
      selectedYear = state.selectedYear,
      yearSummary = state.yearSummary,
      summary2026 = state.summary2026,
      colGhAuditYear = state.colGhAuditYear,
      colGhAuditAllYears = state.colGhAuditAllYears,
      allRecords = state.allRecords,
      config = config
    )
    res.onSuccess { bundle ->
      generatedBundle = bundle
      isGenerating = false
    }.onFailure { err ->
      errorText = err.localizedMessage ?: "Failed to build PDF document."
      isGenerating = false
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 20.dp)
        .testTag("official_pdf_preview_dialog")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
        Surface(
          color = Color(0xFF0A2540),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "GOVT. ERODE MEDICAL COLLEGE • OFFICIAL A4 PDF",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "Leave Audit & Holiday Report Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_pdf_preview_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close PDF Preview",
                  tint = Color.White
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section Toggle Chips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              PdfToggleChip(
                label = if (includeAllYearsAudit) "Audit: 2023–2026" else "Audit: ${state.selectedYear} Only",
                selected = includeAllYearsAudit,
                onClick = { includeAllYearsAudit = !includeAllYearsAudit },
                modifier = Modifier.weight(1f)
              )
              PdfToggleChip(
                label = "Full Ledger",
                selected = includeChronologicalLedger,
                onClick = { includeChronologicalLedger = !includeChronologicalLedger },
                modifier = Modifier.weight(1f)
              )
              PdfToggleChip(
                label = "2026 GH & RH",
                selected = includeHolidaySummary2026,
                onClick = { includeHolidaySummary2026 = !includeHolidaySummary2026 },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Save Confirmation Banner inside Modal
        localSaveStatus?.let { msg ->
          Surface(
            color = EmeraldOptimal.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldOptimal,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldOptimal
              )
            }
          }
        }

        // Body: A4 Page Preview List
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
        ) {
          when {
            isGenerating -> {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                CircularProgressIndicator(color = Color(0xFF34D399))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Formatting Official A4 PDF Report...",
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              }
            }
            errorText != null -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = errorText ?: "Error generating PDF",
                  color = StatusAlertRed,
                  style = MaterialTheme.typography.bodyLarge
                )
              }
            }
            generatedBundle != null -> {
              val bundle = generatedBundle!!
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                item {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = bundle.fileName,
                      color = Color(0xFFBAE6FD),
                      fontSize = 12.sp,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                      Text(
                        text = "${bundle.pageCount} A4 Pages Ready",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                }

                itemsIndexed(bundle.previewBitmaps) { pageIdx, bmp ->
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = "PAGE ${pageIdx + 1} OF ${bundle.pageCount}",
                      color = Color.White.copy(alpha = 0.7f),
                      fontSize = 11.sp,
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold
                    )
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Color.White,
                      shadowElevation = 6.dp,
                      modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(595f / 842f)
                    ) {
                      Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "A4 PDF Page ${pageIdx + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Bottom Action Bar: Save PDF to Device & Share / Submit to Office
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                generatedBundle?.let { bundle ->
                  saveDocumentLauncher.launch(bundle.fileName)
                }
              },
              enabled = generatedBundle != null && !isGenerating,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, EmeraldOptimal),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("save_pdf_to_device_button")
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = EmeraldOptimal,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Save PDF File",
                color = EmeraldOptimal,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }

            Button(
              onClick = {
                generatedBundle?.let { bundle ->
                  OfficialPdfReportExporter.sharePdfReport(
                    context = context,
                    pdfFile = bundle.pdfFile,
                    selectedYear = state.selectedYear
                  )
                }
              },
              enabled = generatedBundle != null && !isGenerating,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("share_pdf_report_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Share / Submit PDF",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PdfToggleChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f),
    border = BorderStroke(
      1.dp,
      if (selected) Color(0xFF34D399) else Color.White.copy(alpha = 0.25f)
    ),
    modifier = modifier
      .height(32.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        color = if (selected) Color(0xFF052E16) else Color.White,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
