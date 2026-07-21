package com.dali951.blackscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AodRenderer(context: Context) : View(context) {

    private val prefs = PrefsManager(context)

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 96f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val mediaTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val mediaArtistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val batteryBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
        style = Paint.Style.FILL
    }

    private val batteryFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val batteryStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    private val handler = Handler(Looper.getMainLooper())
    private var batteryLevel = 0

    private var currentTitle: String? = null
    private var currentArtist: String? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMediaInfo()
            updateBatteryInfo()
            invalidate()
            handler.postDelayed(this, 60_000L)
        }
    }

    fun startUpdating() {
        handler.post(updateRunnable)
    }

    fun stopUpdating() {
        handler.removeCallbacks(updateRunnable)
    }

    fun updateMediaInfo() {
        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
                    as? MediaSessionManager ?: return
            val sessions = mediaSessionManager.getActiveSessions(null)
            if (sessions.isNotEmpty()) {
                val metadata = sessions[0].metadata
                currentTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                currentArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            } else {
                currentTitle = null
                currentArtist = null
            }
        } catch (_: SecurityException) {
            currentTitle = null
            currentArtist = null
        }
    }

    fun updateBatteryInfo() {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) {
            batteryLevel = 0
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val centerX = width / 2f
        var yPos = height * 0.35f

        if (prefs.showClock) {
            val timeText = timeFormat.format(Date())
            canvas.drawText(timeText, centerX, yPos, clockPaint)
            yPos += 20f
        }

        if (prefs.showDate) {
            val dateText = dateFormat.format(Date())
            yPos += 60f
            canvas.drawText(dateText, centerX, yPos, datePaint)
        }

        if (prefs.showBattery) {
            yPos += 70f
            drawBattery(canvas, centerX, yPos)
        }

        if (prefs.showMedia && !currentTitle.isNullOrBlank()) {
            yPos += 80f
            canvas.drawText(currentTitle ?: "", centerX, yPos, mediaTitlePaint)

            if (!currentArtist.isNullOrBlank()) {
                yPos += 40f
                canvas.drawText(currentArtist ?: "", centerX, yPos, mediaArtistPaint)
            }
        }
    }

    private fun drawBattery(canvas: Canvas, cx: Float, cy: Float) {
        val batteryWidth = 120f
        val batteryHeight = 28f
        val batteryNubWidth = 8f
        val batteryNubHeight = 14f

        val left = cx - batteryWidth / 2f
        val top = cy - batteryHeight / 2f
        val right = left + batteryWidth
        val bottom = top + batteryHeight

        canvas.drawRoundRect(RectF(left, top, right, bottom), 4f, 4f, batteryBgPaint)

        val fillWidth = (batteryWidth - 4f) * (batteryLevel / 100f)
        if (fillWidth > 0) {
            canvas.drawRoundRect(
                RectF(left + 2f, top + 2f, left + 2f + fillWidth, bottom - 2f),
                3f, 3f, batteryFgPaint
            )
        }

        canvas.drawRoundRect(RectF(left, top, right, bottom), 4f, 4f, batteryStrokePaint)

        val nubLeft = right
        val nubTop = cy - batteryNubHeight / 2f
        canvas.drawRect(
            RectF(nubLeft, nubTop, nubLeft + batteryNubWidth, nubTop + batteryNubHeight),
            batteryStrokePaint
        )

        val batteryText = "$batteryLevel%"
        canvas.drawText(batteryText, cx, bottom + 36f, batteryPaint)
    }
}
