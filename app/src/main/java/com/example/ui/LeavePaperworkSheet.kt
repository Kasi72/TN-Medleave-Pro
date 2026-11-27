package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ApprovalStage
import com.example.data.LeaveRecord
import com.example.data.LeaveWorkflowMeta
import com.example.domain.CalendarAndHandoverBridge
import com.example.domain.JoiningReportAndShareGenerator
import com.example.domain.SanctionSlipVault
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeInLieuLavender
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.LocalFunctionalActionColors
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange

@Composable
fun LeaveStageChip(
  workflow: LeaveWorkflowMeta?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val stage = workflow?.stageEnum ?: ApprovalStage.PLANNED
  val hasSlip = !workflow?.slipLocalFileName.isNullOrBlank()
  val accent = when (stage) {
    ApprovalStage.PLANNED -> neu.textSecondary
    ApprovalStage.SUBMITTED -> StatusPendingOrange
    ApprovalStage.SANCTIONED -> BadgeColCyan
    ApprovalStage.JOINING_FILED -> EmeraldOptimal
  }

  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 32.dp)
      .neumorphicInset(
        shape = IosSharpBadgeShape,
        depth = 1.8.dp,
        blurRadius = 3.5.dp,
        accentTint = accent,
        accentTintAlpha = 0.12f,
        borderColor = accent.copy(alpha = 0.35f),
        neu = neu
      )
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("leave_stage_chip_${stage.code.lowercase()}"),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = if (stage == ApprovalStage.JOINING_FILED || hasSlip) Icons.Default.Verified else Icons.Default.Description,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(11.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = if (hasSlip && stage == ApprovalStage.SANCTIONED) "${stage.shortLabel} • Slip" else stage.shortLabel,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
fun LeavePaperworkDialog(
  record: LeaveRecord,
  existingWorkflow: LeaveWorkflowMeta?,
  onDismiss: () -> Unit,
  onSaveWorkflow: (LeaveWorkflowMeta, String?) -> Unit,
  onAttachSlipUri: (recordId: String, uri: Uri) -> Unit,
  onRemoveSlip: (recordId: String) -> Unit,
  onShowFeedback: (String) -> Unit
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val neu = LocalNeumorphicColors.current
  val actions = LocalFunctionalActionColors.current

  val officerName = remember { HodProfileSettingsStore.getOfficerName(context) }
  val designation = remember { HodProfileSettingsStore.getDesignation(context) }
  val department = remember { HodProfileSettingsStore.getDepartment(context) }
  val institution = remember { HodProfileSettingsStore.getCollege(context) }

  var selectedStage by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.stageEnum ?: ApprovalStage.PLANNED)
  }
  var orderNo by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.sanctionOrderNo.orEmpty())
  }
  var reliever by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.relievingOfficer.orEmpty())
  }
  var relieverContact by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.relievingContact.orEmpty())
  }
  var joiningDateIso by remember(existingWorkflow) {
    mutableStateOf(
      existingWorkflow?.joiningDateIso?.takeIf { it.isNotBlank() }
        ?: JoiningReportAndShareGenerator.computeDefaultJoiningDateIso(record.dateIso)
    )
  }
  var joiningSession by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.joiningSession?.takeIf { it == "FN" || it == "AN" } ?: "FN")
  }
  var workflowNotes by remember(existingWorkflow) {
    mutableStateOf(existingWorkflow?.workflowNotes.orEmpty())
  }

  var showJoiningReportPreview by remember { mutableStateOf(false) }
  var showSlipFullscreen by remember { mutableStateOf(false) }

  val slipFileName = existingWorkflow?.slipLocalFileName.orEmpty()
  val slipBitmap = remember(slipFileName) {
    val file = SanctionSlipVault.resolveSlipFile(context, slipFileName)
    file?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
  }

  // Zero-permission Android Photo Picker for signed physical sanction slips
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      onAttachSlipUri(record.id, uri)
      if (selectedStage == ApprovalStage.PLANNED || selectedStage == ApprovalStage.SUBMITTED) {
        selectedStage = ApprovalStage.SANCTIONED
      }
    }
  }

  fun buildCurrentMeta(overrideStage: ApprovalStage = selectedStage): LeaveWorkflowMeta {
    return (existingWorkflow ?: LeaveWorkflowMeta(recordId = record.id)).copy(
      recordId = record.id,
      stage = overrideStage.code,
      sanctionOrderNo = orderNo.trim().take(80),
      relievingOfficer = reliever.trim().take(100),
      relievingContact = relieverContact.trim().take(40),
      joiningDateIso = joiningDateIso.trim(),
      joiningSession = joiningSession,
      slipLocalFileName = slipFileName,
      workflowNotes = workflowNotes.trim().take(500)
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = IosSharpCardShape,
    containerColor = neu.baseBg,
    title = {
      ExecutiveDialogHeader(
        icon = Icons.Default.Description,
        accent = neu.primaryAccent,
        title = "${record.category.shortLabel} Paperwork & Actions",
        subtitle = "${JoiningReportAndShareGenerator.formatDisplayDate(record.dateIso)} • ${record.category.fullTitle}"
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 520.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // 1. 4-Stage Approval Pipeline Selector
        Text(
          text = "APPROVAL LIFECYCLE STAGE",
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          color = neu.textSecondary
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ApprovalStage.entries.forEach { stage ->
            val chosen = selectedStage == stage
            val stageColor = when (stage) {
              ApprovalStage.PLANNED -> neu.textSecondary
              ApprovalStage.SUBMITTED -> StatusPendingOrange
              ApprovalStage.SANCTIONED -> BadgeColCyan
              ApprovalStage.JOINING_FILED -> EmeraldOptimal
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .then(
                  if (chosen) {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpBadgeShape,
                      elevation = 2.dp,
                      blurRadius = 4.dp,
                      surfaceColor = stageColor,
                      neu = neu
                    )
                  } else {
                    Modifier.neumorphicInset(
                      shape = IosSharpBadgeShape,
                      depth = 1.5.dp,
                      blurRadius = 3.dp,
                      neu = neu
                    )
                  }
                )
                .clickable { selectedStage = stage }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("workflow_stage_btn_${stage.code.lowercase()}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = stage.shortLabel,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (chosen) Color.White else neu.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // 2. Sanction Order & Relieving Officer Metadata
        NeuPaperworkField(
          value = orderNo,
          onValueChange = { orderNo = it.take(80) },
          label = "HOD / Office Diary or Sanction Order No.",
          placeholder = "e.g., Ref.No. 4182/E1/2026"
        )
        NeuPaperworkField(
          value = reliever,
          onValueChange = { reliever = it.take(100) },
          label = "Relieving Medical Officer (In-Charge)",
          placeholder = "e.g., Dr. Aravind (Asst. Prof)"
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          DatePickerOutlinedField(
            value = joiningDateIso,
            onValueChange = { joiningDateIso = it.trim() },
            label = "Return / Joining Date",
            modifier = Modifier.weight(1.45f)
          )
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("FN", "AN").forEach { sess ->
              val active = joiningSession == sess
              Box(
                modifier = Modifier
                  .weight(1f)
                  .then(
                    if (active) {
                      Modifier.neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 2.dp,
                        blurRadius = 4.dp,
                        surfaceColor = neu.primaryAccent,
                        neu = neu
                      )
                    } else {
                      Modifier.neumorphicInset(
                        shape = IosSharpBadgeShape,
                        depth = 1.5.dp,
                        blurRadius = 3.dp,
                        neu = neu
                      )
                    }
                  )
                  .clickable { joiningSession = sess }
                  .padding(vertical = 12.dp)
                  .testTag("joining_session_$sess"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = sess,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (active) Color.White else neu.textSecondary
                )
              }
            }
          }
        }

        // 3. Signed Sanction Slip Photo Vault (Android Photo Picker — Zero Storage Permission)
        Text(
          text = "SIGNED SANCTION SLIP VAULT (PRIVATE ON-DEVICE)",
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          color = neu.textSecondary
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = if (slipBitmap != null) EmeraldOptimal else BadgeColCyan,
              accentTintAlpha = 0.06f,
              neu = neu
            )
            .padding(10.dp)
        ) {
          if (slipBitmap != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Image(
                bitmap = slipBitmap.asImageBitmap(),
                contentDescription = "Signed sanction slip thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(64.dp)
                  .clip(IosSharpButtonShape)
                  .clickable { showSlipFullscreen = true }
                  .testTag("sanction_slip_thumbnail")
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Signed Slip Attached",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldOptimal
                )
                Text(
                  text = "Tap photo to zoom • Stored securely offline",
                  fontSize = 10.5.sp,
                  color = neu.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "Zoom",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BadgeColCyan,
                    modifier = Modifier.clickable { showSlipFullscreen = true }
                  )
                  Text(
                    text = "Replace",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BadgePlGold,
                    modifier = Modifier.clickable {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    }
                  )
                  Text(
                    text = "Remove",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusAlertRed,
                    modifier = Modifier.clickable { onRemoveSlip(record.id) }
                  )
                }
              }
            }
          } else {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
                .padding(vertical = 4.dp)
                .testTag("attach_sanction_slip_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = BadgeColCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Attach Signed HOD Sanction Slip Photo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BadgeColCyan
              )
            }
          }
        }

        // 4. One-Tap Clinical Action Tools: Joining Report, Google Calendar, WhatsApp Handover
        Text(
          text = "ONE-TAP OFFICIAL TOOLS & SHARING",
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          color = neu.textSecondary
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                surfaceColor = actions.exportDocs,
                neu = neu
              )
              .clickable { showJoiningReportPreview = true }
              .padding(vertical = 10.dp, horizontal = 8.dp)
              .testTag("open_joining_report_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("Joining Report", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                surfaceColor = actions.exportDocs,
                neu = neu
              )
              .clickable {
                val meta = buildCurrentMeta()
                val title = "${record.category.shortLabel} Leave • TN MedLeave Pro"
                val desc = JoiningReportAndShareGenerator.generateClinicalHandoverMessage(
                  officerName = officerName,
                  designation = designation,
                  department = department,
                  institution = institution,
                  leaveLabel = record.category.fullTitle,
                  startDateIso = record.dateIso,
                  endDateIso = record.dateIso,
                  workflow = meta
                )
                CalendarAndHandoverBridge.openDeviceCalendarInsert(
                  context = context,
                  title = title,
                  startDateIso = record.dateIso,
                  endDateIso = record.dateIso,
                  description = desc,
                  location = institution
                ).onFailure {
                  CalendarAndHandoverBridge.shareIcsCalendarFile(
                    context, title, record.dateIso, record.dateIso, desc, institution
                  )
                }
                onShowFeedback("Opened device Calendar for ${record.dateIso}.")
              }
              .padding(vertical = 10.dp, horizontal = 8.dp)
              .testTag("add_to_google_calendar_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("Calendar Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1.3f)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                surfaceColor = actions.shareHandover,
                neu = neu
              )
              .clickable {
                val msg = JoiningReportAndShareGenerator.generateClinicalHandoverMessage(
                  officerName = officerName,
                  designation = designation,
                  department = department,
                  institution = institution,
                  leaveLabel = "${record.category.fullTitle} (${record.category.shortLabel})",
                  startDateIso = record.dateIso,
                  endDateIso = record.dateIso,
                  workflow = buildCurrentMeta()
                )
                CalendarAndHandoverBridge.shareClinicalHandover(context, msg, preferWhatsApp = true)
                  .onFailure { onShowFeedback("Could not open share sheet.") }
              }
              .padding(vertical = 10.dp, horizontal = 8.dp)
              .testTag("whatsapp_handover_share_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("WhatsApp Handover", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
          }

          Box(
            modifier = Modifier
              .weight(0.7f)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                surfaceColor = actions.exportDocs,
                neu = neu
              )
              .clickable {
                val meta = buildCurrentMeta()
                val title = "${record.category.shortLabel} Leave (${record.dateIso})"
                val desc = "Ref: ${meta.sanctionOrderNo.ifBlank { "TN MedLeave Pro" }} • Reliever: ${meta.relievingOfficer.ifBlank { "Unit Duty" }}"
                CalendarAndHandoverBridge.shareIcsCalendarFile(
                  context, title, record.dateIso, record.dateIso, desc, institution
                ).onFailure { onShowFeedback("Could not export .ics file.") }
              }
              .padding(vertical = 10.dp, horizontal = 6.dp)
              .testTag("export_ics_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(".ICS File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveWorkflow(buildCurrentMeta(), null)
          onDismiss()
        },
        shape = IosSharpButtonShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = actions.createPrimary,
          contentColor = Color.White
        ),
        modifier = Modifier.testTag("save_leave_workflow_button")
      ) {
        Text("Save Paperwork", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )

  // Official Return-to-Duty Joining Report Preview Dialog
  if (showJoiningReportPreview) {
    val draftMeta = buildCurrentMeta(ApprovalStage.JOINING_FILED)
    val reportText = remember(draftMeta, record) {
      JoiningReportAndShareGenerator.generateOfficialJoiningReportText(
        officerName = officerName,
        designation = designation,
        department = department,
        institution = institution,
        record = record,
        workflow = draftMeta
      )
    }
    AlertDialog(
      onDismissRequest = { showJoiningReportPreview = false },
      shape = IosSharpCardShape,
      containerColor = neu.baseBg,
      title = {
        ExecutiveDialogHeader(
          icon = Icons.Default.CheckCircle,
          accent = EmeraldOptimal,
          title = "Official TN Joining Report",
          subtitle = "RETURN TO DUTY • ${draftMeta.joiningDateIso} (${draftMeta.joiningSession})"
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp)
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                neu = neu
              )
              .padding(12.dp)
              .verticalScroll(rememberScrollState())
          ) {
            Text(
              text = reportText,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.5.sp,
              lineHeight = 15.sp,
              color = neu.textPrimary
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                clipboard.setText(AnnotatedString(reportText))
                onShowFeedback("Copied Official Joining Report to clipboard!")
              },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = actions.shareHandover,
                contentColor = Color.White
              ),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = {
                CalendarAndHandoverBridge.shareClinicalHandover(context, reportText, preferWhatsApp = false)
              },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = actions.shareHandover,
                contentColor = Color.White
              ),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = {
                CalendarAndHandoverBridge.exportAndShareJoiningReportPdf(
                  context = context,
                  fileSlug = "${record.leaveType}_${record.dateIso}",
                  reportText = reportText
                ).onFailure { onShowFeedback("PDF export failed: ${it.localizedMessage}") }
              },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = actions.exportDocs,
                contentColor = Color.White
              ),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            selectedStage = ApprovalStage.JOINING_FILED
            onSaveWorkflow(
              draftMeta,
              "Joining Report ready & ${record.category.shortLabel} (${record.dateIso}) marked Joining Filed!"
            )
            showJoiningReportPreview = false
            onDismiss()
          },
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = actions.verifyApprove,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("mark_joining_filed_button")
        ) {
          Text("Mark Joining Filed", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showJoiningReportPreview = false }) {
          Text("Back")
        }
      }
    )
  }

  // Full-Screen Sanction Slip Viewer
  if (showSlipFullscreen && slipBitmap != null) {
    Dialog(
      onDismissRequest = { showSlipFullscreen = false },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.92f))
          .padding(16.dp)
      ) {
        Image(
          bitmap = slipBitmap.asImageBitmap(),
          contentDescription = "Full-size signed sanction slip",
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
        )
        IconButton(
          onClick = { showSlipFullscreen = false },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 24.dp)
            .background(Color.Black.copy(alpha = 0.6f), shape = IosSharpButtonShape)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close slip viewer", tint = Color.White)
        }
      }
    }
  }
}

@Composable
private fun NeuPaperworkField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicInset(
        shape = IosSharpWellShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        neu = neu
      )
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label, fontSize = 11.5.sp) },
      placeholder = { Text(placeholder, fontSize = 11.5.sp) },
      singleLine = true,
      shape = IosSharpWellShape,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = neu.primaryAccent.copy(alpha = 0.55f),
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}
