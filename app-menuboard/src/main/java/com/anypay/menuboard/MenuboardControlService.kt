package com.anypay.menuboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MenuboardControlService : AccessibilityService() {

    companion object {
        var instance: MenuboardControlService? = null
    }

    private var windowManager: WindowManager? = null
    private var cursorView: GlobalCursorView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var cursorX = 540f
    var cursorY = 960f
    private var screenW = 1080
    private var screenH = 1920
    private val hideCursorRunnable = Runnable { cursorView?.visibility = View.GONE }

    inner class GlobalCursorView(ctx: Context) : View(ctx) {
        private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.FILL
        }
        private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        private val cursorPath = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            cursorPath.reset()
            cursorPath.moveTo(cursorX, cursorY)
            cursorPath.lineTo(cursorX, cursorY + 38f)
            cursorPath.lineTo(cursorX + 11f, cursorY + 29f)
            cursorPath.lineTo(cursorX + 19f, cursorY + 44f)
            cursorPath.lineTo(cursorX + 26f, cursorY + 40f)
            cursorPath.lineTo(cursorX + 17f, cursorY + 25f)
            cursorPath.lineTo(cursorX + 30f, cursorY + 25f)
            cursorPath.close()

            canvas.drawPath(cursorPath, paintFill)
            canvas.drawPath(cursorPath, paintStroke)
        }
    }

    private fun updateRealScreenSize() {
        val wm = windowManager ?: (getSystemService(WINDOW_SERVICE) as? WindowManager)
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.maximumWindowMetrics.bounds
                screenW = bounds.width()
                screenH = bounds.height()
            } else {
                val dm = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(dm)
                screenW = dm.widthPixels
                screenH = dm.heightPixels
            }
        } else {
            val dm = resources.displayMetrics
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        updateRealScreenSize()
        cursorX = screenW / 2f
        cursorY = screenH / 2f
        attachGlobalCursorOverlay()
    }

    private fun attachGlobalCursorOverlay() {
        if (cursorView != null) return
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val view = GlobalCursorView(this)

            val primaryType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            fun buildParams(type: Int): WindowManager.LayoutParams {
                return WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        fitInsetsTypes = 0
                    }
                }
            }

            try {
                windowManager?.addView(view, buildParams(primaryType))
                cursorView = view
            } catch (_: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Settings.canDrawOverlays(this)) {
                    windowManager?.addView(view, buildParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))
                    cursorView = view
                }
            }
            cursorView?.visibility = View.GONE
        } catch (_: Exception) {}
    }

    fun moveCursorBy(dx: Float, dy: Float) {
        mainHandler.post {
            if (cursorView == null) attachGlobalCursorOverlay()
            updateRealScreenSize()
            cursorX = (cursorX + dx).coerceIn(0f, (screenW - 2).toFloat())
            cursorY = (cursorY + dy).coerceIn(0f, (screenH - 2).toFloat())
            cursorView?.visibility = View.VISIBLE
            cursorView?.bringToFront()
            cursorView?.invalidate()
            mainHandler.removeCallbacks(hideCursorRunnable)
            mainHandler.postDelayed(hideCursorRunnable, 8000L)
        }
    }

    // Sistem Pencerelerinde (Yalnızca bir defa / Her zaman / İzin Onayları) Tapjacking Korumasını Aşan Akıllı Tıklama
    fun clickAtCursor() {
        mainHandler.post {
            val x = cursorX.toInt()
            val y = cursorY.toInt()

            val clickedViaNode = tryClickAccessibilityNodeAt(x, y)
            cursorView?.visibility = View.GONE

            mainHandler.postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !clickedViaNode) {
                    val path = Path().apply { moveTo(cursorX, cursorY) }
                    val stroke = GestureDescription.StrokeDescription(path, 0, 50)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()
                    dispatchGesture(gesture, null, null)
                }
                mainHandler.postDelayed({
                    cursorView?.visibility = View.VISIBLE
                    mainHandler.removeCallbacks(hideCursorRunnable)
                    mainHandler.postDelayed(hideCursorRunnable, 8000L)
                }, 80L)
            }, 40L)
        }
    }

    private fun tryClickAccessibilityNodeAt(x: Int, y: Int): Boolean {
        try {
            val root = rootInActiveWindow ?: return false
            val targetNode = findSmallestClickableNodeAt(root, x, y)
            if (targetNode != null) {
                if (targetNode.isCheckable && !targetNode.isChecked) {
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                if (targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun findSmallestClickableNodeAt(node: AccessibilityNodeInfo?, x: Int, y: Int): AccessibilityNodeInfo? {
        if (node == null || !node.isVisibleToUser) return null
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        for (i in 0 until node.childCount) {
            val childMatch = findSmallestClickableNodeAt(node.getChild(i), x, y)
            if (childMatch != null) return childMatch
        }

        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable || current.isCheckable) return current
            current = current.parent
        }
        return null
    }

    fun scrollAtCursor(scrollDy: Float) {
        mainHandler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                updateRealScreenSize()
                val endY = (cursorY + scrollDy).coerceIn(10f, (screenH - 10).toFloat())
                val path = Path().apply {
                    moveTo(cursorX, cursorY)
                    lineTo(cursorX, endY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, 220)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(gesture, null, null)
            }
        }
    }

    fun performGlobalNav(act: String) {
        mainHandler.post {
            when (act) {
                "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
        }
    }

    fun bringPackageToFront(pkg: String) {
        mainHandler.post {
            try {
                if (cursorView == null) attachGlobalCursorOverlay()
                cursorView?.visibility = View.VISIBLE
                cursorView?.invalidate()

                if (pkg == packageName) {
                    val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    val myTaskId = MainActivity.currentTaskId
                    if (myTaskId != -1) {
                        am?.moveTaskToFront(myTaskId, ActivityManager.MOVE_TASK_WITH_HOME)
                    }
                }

                val launchIntent = if (pkg == packageName) {
                    Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                } else {
                    packageManager.getLaunchIntentForPackage(pkg)
                }

                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )

                    val optionsBundle: Bundle? = if (Build.VERSION.SDK_INT >= 34) {
                        try {
                            val opts = ActivityOptions.makeBasic()
                            val method = ActivityOptions::class.java.getMethod(
                                "setPendingIntentBackgroundActivityStartMode",
                                Int::class.javaPrimitiveType
                            )
                            method.invoke(opts, 1)
                            opts.toBundle()
                        } catch (_: Exception) { null }
                    } else null

                    val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pi = PendingIntent.getActivity(this, (System.currentTimeMillis() % 10000).toInt(), launchIntent, piFlags)
                    try {
                        if (optionsBundle != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            pi.send(this, 0, null, null, null, null, optionsBundle)
                        } else {
                            pi.send()
                        }
                    } catch (_: Exception) {}

                    startActivity(launchIntent)
                }

                mainHandler.removeCallbacks(hideCursorRunnable)
                mainHandler.postDelayed(hideCursorRunnable, 3000L)
            } catch (_: Exception) {}
        }
    }

    fun inputKeyboardText(text: String, isBackspace: Boolean = false) {
        mainHandler.post {
            val root = rootInActiveWindow ?: return@post
            val focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditableNode(root)
            if (focusNode != null) {
                val currentText = focusNode.text?.toString() ?: ""
                val newText = if (isBackspace) {
                    if (currentText.isNotEmpty()) currentText.dropLast(1) else ""
                } else {
                    currentText + text
                }
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
    }

    private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        try {
            if (cursorView != null) windowManager?.removeView(cursorView)
            cursorView = null
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
