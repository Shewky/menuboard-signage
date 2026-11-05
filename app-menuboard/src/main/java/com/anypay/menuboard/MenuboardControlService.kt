package com.anypay.menuboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val dm = resources.displayMetrics
        screenW = dm.widthPixels
        screenH = dm.heightPixels
        cursorX = screenW / 2f
        cursorY = screenH / 2f
        attachGlobalCursorOverlay()
    }

    private fun attachGlobalCursorOverlay() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            cursorView = GlobalCursorView(this)
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            windowManager?.addView(cursorView, params)
            cursorView?.visibility = View.GONE
        } catch (_: Exception) {}
    }

    fun moveCursorBy(dx: Float, dy: Float) {
        mainHandler.post {
            if (cursorView == null) attachGlobalCursorOverlay()
            val dm = resources.displayMetrics
            screenW = dm.widthPixels
            screenH = dm.heightPixels
            cursorX = (cursorX + dx).coerceIn(5f, screenW - 15f)
            cursorY = (cursorY + dy).coerceIn(5f, screenH - 15f)
            cursorView?.visibility = View.VISIBLE
            cursorView?.invalidate()
            mainHandler.removeCallbacks(hideCursorRunnable)
            mainHandler.postDelayed(hideCursorRunnable, 8000L)
        }
    }

    fun clickAtCursor() {
        mainHandler.post {
            cursorView?.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply { moveTo(cursorX, cursorY) }
                val stroke = GestureDescription.StrokeDescription(path, 0, 55)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(gesture, null, null)
            }
        }
    }

    fun scrollAtCursor(scrollDy: Float) {
        mainHandler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val endY = (cursorY + scrollDy).coerceIn(60f, screenH - 60f)
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
            performGlobalAction(GLOBAL_ACTION_HOME)
            mainHandler.postDelayed({
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                        startActivity(launchIntent)
                    }
                } catch (_: Exception) {}
            }, 140L)
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
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
