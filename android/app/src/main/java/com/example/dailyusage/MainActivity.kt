package com.example.dailyusage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : Activity() {
    private val ink = 0xFF202B3B.toInt()
    private val muted = 0xFF697586.toInt()
    private val blue = 0xFF526EE5.toInt()
    private val page = 0xFFF5F6F3.toInt()
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = 0xFFFFFFFF.toInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(page)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(25), dp(22), dp(30))
        }
        scroll.addView(content)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            val top = insets.systemWindowInsetTop
            @Suppress("DEPRECATION")
            val bottom = insets.systemWindowInsetBottom
            view.setPadding(0, top, 0, bottom)
            insets
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) renderScreen()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun renderScreen() {
        content.removeAllViews()
        content.addView(text("나의 디지털 하루", 14, 0xFF547B69.toInt(), true), margin(bottom = 14))
        content.addView(text("오늘 가장 오래\n사용한 앱은?", 34, ink, true, lineSpacing = 1.02f), margin(bottom = 12))
        content.addView(
            text("사용 정보 접근을 허용하면 오늘 사용한 앱과 시간을 확인할 수 있어요.", 16, muted, false, lineSpacing = 1.15f),
            margin(bottom = 22)
        )

        val granted = hasUsageAccess()
        val permissionCard = row(0xFFFFFFFF.toInt(), 18).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
        }
        val permissionSymbol = text("◉", 22, blue, true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFFEEF1FF.toInt(), 14)
        }
        permissionCard.addView(permissionSymbol, size(42, 42).also { it.marginEnd = dp(12) })
        val permissionText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        permissionText.addView(text("사용 정보 접근", 15, ink, true))
        permissionText.addView(text(if (granted) "사용시간을 확인할 수 있어요" else "Android 설정에서 직접 허용해요", 12, muted).apply {
            setPadding(0, dp(4), 0, 0)
        })
        permissionCard.addView(permissionText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        permissionCard.addView(text(if (granted) "허용됨" else "설정 필요", 11, if (granted) 0xFF347858.toInt() else 0xFF956423.toInt(), true).apply {
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = rounded(if (granted) 0xFFE5F3E9.toInt() else 0xFFFFF2DF.toInt(), 50)
        })
        content.addView(permissionCard, margin(bottom = 12))

        val openSettings = Button(this).apply {
            text = if (granted) "사용 정보 접근 설정 열기" else "사용 정보 접근 허용"
            isAllCaps = false
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(blue, 15)
            minHeight = dp(54)
            setPadding(dp(14), 0, dp(14), 0)
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "이 기기에서 사용 정보 설정을 열 수 없어요.", Toast.LENGTH_LONG).show()
                }
            }
        }
        content.addView(openSettings, margin(bottom = 24))

        if (granted) {
            showUsageList()
        } else {
            val privacy = row(0xFFECEFEC.toInt(), 16).apply {
                setPadding(dp(15), dp(13), dp(15), dp(13))
                gravity = Gravity.CENTER_VERTICAL
            }
            privacy.addView(text("◇", 20, 0xFF547B69.toInt(), true), size(30, 30).also { it.marginEnd = dp(9) })
            privacy.addView(text("사용시간은 이 기기에서만 읽으며, 허용 전에는 앱 목록을 볼 수 없어요.", 13, muted, lineSpacing = 1.1f))
            content.addView(privacy)
        }
    }

    private fun showUsageList() {
        val header = row(0x00000000, 10).apply { gravity = Gravity.CENTER_VERTICAL }
        val heading = text("오늘 사용시간 TOP 5", 21, ink, true)
        header.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val date = SimpleDateFormat("M월 d일", Locale.KOREAN).format(Date())
        header.addView(text(date, 12, muted, true))
        content.addView(header, margin(bottom = 8))
        content.addView(text("앱별 사용시간 · 분 단위", 13, muted), margin(bottom = 16))

        val usage = getTodayUsage()
        if (usage.isEmpty()) {
            content.addView(text("오늘 기록된 앱 사용시간이 아직 없어요.", 15, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(36), dp(10), dp(36))
                background = rounded(0xFFFFFFFF.toInt(), 18)
            })
            return
        }

        val maxTime = usage.maxOf { it.totalTimeInForeground }.coerceAtLeast(1L)
        usage.forEachIndexed { index, stat ->
            val info = try {
                packageManager.getApplicationInfo(stat.packageName, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
            val appName = info?.let { packageManager.getApplicationLabel(it).toString() } ?: stat.packageName
            val icon = info?.let {
                try { packageManager.getApplicationIcon(it) } catch (_: Exception) { null }
            }
            content.addView(appUsageCard(index + 1, appName, stat.totalTimeInForeground, maxTime, icon), margin(bottom = 10))
        }
    }

    private fun getTodayUsage(): List<UsageEntry> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()
        val manager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val results = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            ?: return emptyList()
        return results.asSequence()
            .filter { it.totalTimeInForeground > 0L }
            .groupBy { it.packageName }
            .map { (pkg, entries) ->
                val total = entries.sumOf { it.totalTimeInForeground }
                UsageEntry(pkg, total)
            }
            .sortedByDescending { it.total }
            .take(5)
    }

    private fun appUsageCard(rank: Int, name: String, millis: Long, maxMillis: Long, icon: android.graphics.drawable.Drawable?): View {
        val card = row(0xFFFFFFFF.toInt(), 17).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        val rankView = text(rank.toString(), 13, 0xFF738096.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFFF0F2F6.toInt(), 11)
        }
        card.addView(rankView, size(32, 32).also { it.marginEnd = dp(11) })
        val appIcon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(icon ?: getDrawable(android.R.drawable.sym_def_app_icon))
        }
        card.addView(appIcon, size(39, 39).also { it.marginEnd = dp(11) })

        val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val line = row(0x00000000, 0).apply { gravity = Gravity.CENTER_VERTICAL }
        line.addView(text(name, 14, ink, true, maxLines = 1), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val minutes = max(1L, (millis + 30_000L) / 60_000L)
        line.addView(text("${minutes}분", 13, ink, true))
        detail.addView(line)
        val track = FrameLayout(this).apply {
            background = rounded(0xFFEEF0ED.toInt(), 10)
            clipToOutline = true
        }
        val progress = View(this).apply {
            background = rounded(if (rank == 1) 0xFF7589E4.toInt() else 0xFFA0ACE8.toInt(), 10)
        }
        val ratio = (millis.toFloat() / maxMillis).coerceIn(0f, 1f)
        track.addView(progress, FrameLayout.LayoutParams(0, dp(5)).apply { gravity = Gravity.START })
        track.post {
            progress.layoutParams = FrameLayout.LayoutParams((track.width * ratio).toInt().coerceAtLeast(dp(5)), dp(5)).apply { gravity = Gravity.START }
        }
        detail.addView(track, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)).apply { topMargin = dp(9) })
        card.addView(detail, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return card
    }

    private fun text(
        value: String,
        sizeSp: Int,
        color: Int,
        bold: Boolean = false,
        lineSpacing: Float = 1f,
        maxLines: Int = Int.MAX_VALUE
    ) = TextView(this).apply {
        text = value
        textSize = sizeSp.toFloat()
        setTextColor(color)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.maxLines = maxLines
        if (maxLines == 1) ellipsize = android.text.TextUtils.TruncateAt.END
        setLineSpacing(dp(2).toFloat(), lineSpacing)
    }

    private fun row(color: Int, radius: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (color != 0) background = rounded(color, radius)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun size(width: Int, height: Int) = LinearLayout.LayoutParams(dp(width), dp(height))

    private fun margin(bottom: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(bottom) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class UsageEntry(val packageName: String, val total: Long)
}
