package dev.abbasian.agent.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import javax.inject.Inject

class PermissionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var messageView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                val perm = intent.getStringExtra(EXTRA_PERMISSION) ?: return START_NOT_STICKY
                val suspicious = intent.getBooleanExtra(EXTRA_SUSPICIOUS, false)
                showPermissionAlert(pkg, perm, suspicious)
            }
            ACTION_HIDE -> {
                hideOverlay()
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    companion object {
        const val ACTION_SHOW = "dev.abbasian.agent.service.ACTION_SHOW"
        const val ACTION_HIDE = "dev.abbasian.agent.service.ACTION_HIDE"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_PERMISSION = "extra_permission"
        const val EXTRA_SUSPICIOUS = "extra_suspicious"
    }

    private fun createOverlayView() {
        // Programmatic UI creation since we don't have layout resources in this module logic
        val cardView = CardView(this).apply {
            radius = 20f
            cardElevation = 10f
            setCardBackgroundColor(Color.parseColor("#222222"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = TextView(this).apply {
            text = "⚠️" 
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 16, 0)
        }

        messageView = TextView(this).apply {
            text = "Permission Active"
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        container.addView(iconView)
        container.addView(messageView)
        cardView.addView(container)

        overlayView = cardView
    }

    /**
     * Shows a floating overlay alert.
     * Note: Caller must ensure SYSTEM_ALERT_WINDOW permission is granted.
     */
    fun showPermissionAlert(packageName: String, permission: String, isSuspicious: Boolean) {
        if (overlayView == null) createOverlayView()
        
        val textColor = if (isSuspicious) Color.RED else Color.GREEN
        messageView?.text = "$packageName\nusing $permission"
        messageView?.setTextColor(textColor)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100

        try {
            if (overlayView?.windowToken == null) {
                windowManager?.addView(overlayView, params)
            } else {
                windowManager?.updateViewLayout(overlayView, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        try {
            if (overlayView?.windowToken != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
