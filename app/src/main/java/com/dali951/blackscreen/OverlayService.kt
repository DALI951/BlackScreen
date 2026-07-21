package com.dali951.blackscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PrefsManager

    private var floatingButton: View? = null
    private var blackOverlay: View? = null
    private var aodView: AodRenderer? = null

    private var isBlackScreenActive = false
    private var isAodVisible = false

    private var lastTapTime = 0L
    private val DOUBLE_TAP_TIMEOUT = 300L

    private val handler = Handler(Looper.getMainLooper())
    private var aodHideRunnable: Runnable? = null

    private var floatingButtonX = 0
    private var floatingButtonY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PrefsManager(this)
        startForegroundWithNotification()
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        removeBlackOverlay()
        removeAodOverlay()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startForegroundWithNotification() {
        val channelId = "blackscreen_service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BlackScreen overlay service"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun createFloatingButton() {
        val size = (56 * resources.displayMetrics.density).toInt()

        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_power)
            setBackgroundResource(R.drawable.bg_floating_button)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - (16 * resources.displayMetrics.density).toInt()
            y = (resources.displayMetrics.heightPixels * 0.4).toInt()
        }

        floatingButtonX = params.x
        floatingButtonY = params.y

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        imageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(imageView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onFloatingButtonTap(imageView)
                    }
                    true
                }
                else -> false
            }
        }

        floatingButton = imageView
        windowManager.addView(imageView, params)
    }

    private fun onFloatingButtonTap(imageView: ImageView) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            lastTapTime = 0L
            if (isBlackScreenActive && !isAodVisible) {
                showAod()
            } else if (isAodVisible) {
                hideAod()
            }
        } else {
            lastTapTime = now
            handler.postDelayed({
                if (lastTapTime != 0L && SystemClock.elapsedRealtime() - lastTapTime >= DOUBLE_TAP_TIMEOUT - 10) {
                    lastTapTime = 0L
                    toggleBlackScreen(imageView)
                }
            }, DOUBLE_TAP_TIMEOUT)
        }
    }

    private fun toggleBlackScreen(imageView: ImageView) {
        if (isBlackScreenActive) {
            hideBlackScreen()
            imageView.setBackgroundResource(R.drawable.bg_floating_button)
        } else {
            showBlackScreen()
            imageView.setBackgroundResource(R.drawable.bg_floating_button_active)
        }
    }

    private fun showBlackScreen() {
        if (blackOverlay != null) return

        val overlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )

        blackOverlay = overlay
        windowManager.addView(overlay, params)
        isBlackScreenActive = true
    }

    private fun hideBlackScreen() {
        hideAod()
        removeBlackOverlay()
        isBlackScreenActive = false
    }

    private fun removeBlackOverlay() {
        blackOverlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        blackOverlay = null
    }

    private fun showAod() {
        if (aodView != null) return

        val renderer = AodRenderer(this).apply {
            updateMediaInfo()
            updateBatteryInfo()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        aodView = renderer
        windowManager.addView(renderer, params)
        renderer.startUpdating()
        isAodVisible = true

        val timeout = prefs.aodTimeout
        if (timeout > 0) {
            aodHideRunnable = Runnable { hideAod() }
            handler.postDelayed(aodHideRunnable!!, timeout)
        }
    }

    private fun hideAod() {
        aodHideRunnable?.let { handler.removeCallbacks(it) }
        aodHideRunnable = null
        aodView?.let {
            it.stopUpdating()
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        aodView = null
        isAodVisible = false
    }

    private fun removeAodOverlay() {
        aodHideRunnable?.let { handler.removeCallbacks(it) }
        aodHideRunnable = null
        aodView?.let {
            it.stopUpdating()
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        aodView = null
    }

    private fun removeFloatingButton() {
        floatingButton?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        floatingButton = null
    }
}
