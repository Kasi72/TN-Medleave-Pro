package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubUpdateCheckResult(
  val isSuccess: Boolean,
  val updateAvailable: Boolean,
  val resolvedOwner: String,
  val resolvedRepo: String,
  val resolvedBranch: String,
  val currentInstalledVersion: String,
  val remoteVersionLabel: String,
  val latestCommitShaShort: String,
  val latestCommitMessage: String,
  val publishedAtFormatted: String,
  val directApkDownloadUrl: String,
  val releasePageUrl: String,
  val statusMessage: String
)

object GitHubAppUpdaterService {
  private const val PREFS_NAME = "github_ota_updater_prefs"
  private const val KEY_GITHUB_OWNER = "github_owner"
  private const val KEY_GITHUB_REPO = "github_repo"
  private const val KEY_GITHUB_BRANCH = "github_branch"
  private const val KEY_AUTO_CHECK_LAUNCH = "auto_check_on_launch"
  private const val KEY_LAST_SYNCED_SHA = "last_synced_commit_sha"
  private const val KEY_LAST_CHECK_SUMMARY = "last_check_summary"
  private const val KEY_LAST_DIRECT_APK_URL = "last_direct_apk_url"

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .build()
  }

  fun getOwner(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_GITHUB_OWNER, "Kasi72") ?: "Kasi72"

  fun getRepo(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_GITHUB_REPO, "") ?: ""

  fun getBranch(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_GITHUB_BRANCH, "main") ?: "main"

  fun isAutoCheckOnLaunchEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_AUTO_CHECK_LAUNCH, true)

  fun setAutoCheckOnLaunchEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_AUTO_CHECK_LAUNCH, enabled)
      .apply()
  }

  fun getLastSyncedSha(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_LAST_SYNCED_SHA, "") ?: ""

  fun getLastCheckSummary(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(
        KEY_LAST_CHECK_SUMMARY,
        "Installed v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Tap 'Check GitHub Update' to sync with GitHub"
      ) ?: "Installed v${BuildConfig.VERSION_NAME}"

  fun getLastDirectApkUrl(context: Context): String {
    val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_LAST_DIRECT_APK_URL, "") ?: ""
    if (saved.isNotBlank()) return saved
    val owner = getOwner(context).ifBlank { "Kasi72" }
    val repo = getRepo(context)
    val branch = getBranch(context).ifBlank { "main" }
    return if (repo.isNotBlank()) {
      "https://github.com/$owner/$repo/raw/$branch/app/build/outputs/apk/debug/app-debug.apk"
    } else {
      "https://github.com/$owner?tab=repositories"
    }
  }

  fun saveGitHubCoordinates(
    context: Context,
    owner: String,
    repo: String,
    branch: String
  ) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_GITHUB_OWNER, owner.trim().ifBlank { "Kasi72" })
      .putString(KEY_GITHUB_REPO, repo.trim())
      .putString(KEY_GITHUB_BRANCH, branch.trim().ifBlank { "main" })
      .apply()
  }

  suspend fun checkForUpdatesFromGitHub(
    context: Context,
    ownerInput: String,
    repoInput: String,
    branchInput: String
  ): GitHubUpdateCheckResult = withContext(Dispatchers.IO) {
    val cleanOwner = ownerInput.trim().ifBlank { "Kasi72" }
    var cleanRepo = repoInput.trim()
    var cleanBranch = branchInput.trim().ifBlank { "main" }

    try {
      // 1. If repository name is blank, auto-discover the most recently updated repo for this GitHub user
      if (cleanRepo.isBlank()) {
        val reposReq = Request.Builder()
          .url("https://api.github.com/users/$cleanOwner/repos?sort=updated&per_page=10")
          .header("Accept", "application/vnd.github+json")
          .header("User-Agent", "TN-MedLeave-Pro-Android-Updater")
          .build()

        httpClient.newCall(reposReq).execute().use { resp ->
          if (resp.isSuccessful) {
            val bodyStr = resp.body?.string().orEmpty()
            val arr = JSONArray(bodyStr)
            if (arr.length() > 0) {
              // Prefer a Kotlin / Android repo or the most recently pushed repo
              var chosenObj: JSONObject = arr.getJSONObject(0)
              for (i in 0 until arr.length()) {
                val candidate = arr.getJSONObject(i)
                val lang = candidate.optString("language", "")
                if (lang.equals("Kotlin", ignoreCase = true)) {
                  chosenObj = candidate
                  break
                }
              }
              cleanRepo = chosenObj.optString("name", "")
              val defaultBr = chosenObj.optString("default_branch", "")
              if (defaultBr.isNotBlank()) {
                cleanBranch = defaultBr
              }
            }
          }
        }
      }

      if (cleanRepo.isBlank()) {
        return@withContext GitHubUpdateCheckResult(
          isSuccess = false,
          updateAvailable = false,
          resolvedOwner = cleanOwner,
          resolvedRepo = "",
          resolvedBranch = cleanBranch,
          currentInstalledVersion = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})",
          remoteVersionLabel = "Repo Not Found",
          latestCommitShaShort = "—",
          latestCommitMessage = "Enter your GitHub repository name (or make sure the repo is public on github.com/$cleanOwner)",
          publishedAtFormatted = "Just now",
          directApkDownloadUrl = "https://github.com/$cleanOwner?tab=repositories",
          releasePageUrl = "https://github.com/$cleanOwner?tab=repositories",
          statusMessage = "Could not auto-detect public repo for $cleanOwner. Enter repo name below."
        )
      }

      saveGitHubCoordinates(context, cleanOwner, cleanRepo, cleanBranch)

      // 2. Check Latest GitHub Release first
      var releaseTag = ""
      var releaseName = ""
      var releasePublishedAt = ""
      var releaseAssetApkUrl = ""
      var releaseHtmlUrl = "https://github.com/$cleanOwner/$cleanRepo/releases"

      val releaseReq = Request.Builder()
        .url("https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest")
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "TN-MedLeave-Pro-Android-Updater")
        .build()

      httpClient.newCall(releaseReq).execute().use { resp ->
        if (resp.isSuccessful) {
          val json = JSONObject(resp.body?.string().orEmpty())
          releaseTag = json.optString("tag_name", "")
          releaseName = json.optString("name", releaseTag)
          releasePublishedAt = json.optString("published_at", "").take(16).replace("T", " ")
          releaseHtmlUrl = json.optString("html_url", releaseHtmlUrl)
          val assets = json.optJSONArray("assets")
          if (assets != null) {
            for (i in 0 until assets.length()) {
              val asset = assets.getJSONObject(i)
              val assetName = asset.optString("name", "")
              if (assetName.endsWith(".apk", ignoreCase = true)) {
                releaseAssetApkUrl = asset.optString("browser_download_url", "")
                break
              }
            }
          }
        }
      }

      // 3. Also check Latest Commit on the target branch
      var commitShaShort = ""
      var commitMessage = ""
      var commitDate = ""

      val commitReq = Request.Builder()
        .url("https://api.github.com/repos/$cleanOwner/$cleanRepo/commits/$cleanBranch")
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "TN-MedLeave-Pro-Android-Updater")
        .build()

      httpClient.newCall(commitReq).execute().use { resp ->
        if (resp.isSuccessful) {
          val json = JSONObject(resp.body?.string().orEmpty())
          commitShaShort = json.optString("sha", "").take(7)
          val commitObj = json.optJSONObject("commit")
          commitMessage = commitObj?.optString("message", "")?.lines()?.firstOrNull().orEmpty()
          commitDate = commitObj
            ?.optJSONObject("committer")
            ?.optString("date", "")
            ?.take(16)
            ?.replace("T", " ")
            .orEmpty()
        }
      }

      val rawBranchApkUrl =
        "https://github.com/$cleanOwner/$cleanRepo/raw/$cleanBranch/app/build/outputs/apk/debug/app-debug.apk"
      val bestDirectApkUrl = releaseAssetApkUrl.ifBlank { rawBranchApkUrl }

      val remoteId = releaseTag.ifBlank { commitShaShort }
      val previousSyncedId = getLastSyncedSha(context)
      val isNewUpdateAvailable = remoteId.isNotBlank() && remoteId != previousSyncedId

      val remoteVersionDisplay = when {
        releaseTag.isNotBlank() && commitShaShort.isNotBlank() -> "$releaseTag ($commitShaShort)"
        releaseTag.isNotBlank() -> releaseTag
        commitShaShort.isNotBlank() -> "Commit #$commitShaShort"
        else -> "Latest $cleanBranch"
      }

      val summaryLine = if (isNewUpdateAvailable) {
        "Update Available on $cleanOwner/$cleanRepo: $remoteVersionDisplay • Tap 'Download & Update APK'"
      } else {
        "Synced with $cleanOwner/$cleanRepo ($remoteVersionDisplay) • Ready to download/reinstall APK"
      }

      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_CHECK_SUMMARY, summaryLine)
        .putString(KEY_LAST_DIRECT_APK_URL, bestDirectApkUrl)
        .apply()

      GitHubUpdateCheckResult(
        isSuccess = true,
        updateAvailable = isNewUpdateAvailable,
        resolvedOwner = cleanOwner,
        resolvedRepo = cleanRepo,
        resolvedBranch = cleanBranch,
        currentInstalledVersion = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})",
        remoteVersionLabel = remoteVersionDisplay,
        latestCommitShaShort = commitShaShort.ifBlank { "latest" },
        latestCommitMessage = commitMessage.ifBlank { releaseName.ifBlank { "Latest AI Studio GitHub Push" } },
        publishedAtFormatted = releasePublishedAt.ifBlank { commitDate.ifBlank { "Live on GitHub" } },
        directApkDownloadUrl = bestDirectApkUrl,
        releasePageUrl = releaseHtmlUrl,
        statusMessage = summaryLine
      )
    } catch (e: Exception) {
      val fallbackApkUrl = if (cleanRepo.isNotBlank()) {
        "https://github.com/$cleanOwner/$cleanRepo/raw/$cleanBranch/app/build/outputs/apk/debug/app-debug.apk"
      } else {
        "https://github.com/$cleanOwner?tab=repositories"
      }
      GitHubUpdateCheckResult(
        isSuccess = false,
        updateAvailable = false,
        resolvedOwner = cleanOwner,
        resolvedRepo = cleanRepo,
        resolvedBranch = cleanBranch,
        currentInstalledVersion = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})",
        remoteVersionLabel = "Offline / Private Repo",
        latestCommitShaShort = "—",
        latestCommitMessage = e.localizedMessage ?: "Network error while contacting api.github.com",
        publishedAtFormatted = "—",
        directApkDownloadUrl = fallbackApkUrl,
        releasePageUrl = "https://github.com/$cleanOwner/$cleanRepo/releases",
        statusMessage = "Could not query GitHub API (${e.javaClass.simpleName}). You can still open the direct APK link below."
      )
    }
  }

  fun markUpdateDownloadedAndOpenUrl(
    context: Context,
    apkOrReleaseUrl: String,
    syncedVersionId: String = ""
  ) {
    if (syncedVersionId.isNotBlank()) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_SYNCED_SHA, syncedVersionId)
        .apply()
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkOrReleaseUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      context.startActivity(intent)
    }
  }
}
