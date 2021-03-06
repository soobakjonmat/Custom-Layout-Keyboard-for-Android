package com.soobakjonmat.customlayoutkeyboard.layout

import android.annotation.SuppressLint
import android.content.res.Resources
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.core.view.size
import com.soobakjonmat.customlayoutkeyboard.MainKeyboardService
import com.soobakjonmat.customlayoutkeyboard.R
import java.util.*
import kotlin.concurrent.timerTask

class EnglishLayout(private val mainKeyboardService: MainKeyboardService) {
    private val mainKeyboardView = mainKeyboardService.mainKeyboardView
    private val resources: Resources = mainKeyboardService.baseContext.resources
    private val gestureMinDist = mainKeyboardService.gestureMinDist

    private val capsLockMode0Image = mainKeyboardService.capsLockMode0Image
    private val capsLockMode1Image = mainKeyboardService.capsLockMode1Image
    private val capsLockMode2Image = mainKeyboardService.capsLockMode2Image

    private val row1Letters = listOf("q", "w", "f", "p", "g", "j", "l", "u", "y")
    private val row2Letters = listOf("a", "s", "d", "t", "r", "h", "e", "k", "i", "o")
    private val row3Letters = listOf("z", "x", "c", "v", "b", "n", "m")
    private val letterList = listOf(row1Letters, row2Letters, row3Letters)

    private val combinedLetterList = List(letterList.size) { mutableListOf<SpannableString>() }

    private val btnList = mutableListOf<List<Button>>()

    private val rowList = List(letterList.size) { LinearLayout(mainKeyboardView.context) }

    private val capsLockBtn = ImageButton(ContextThemeWrapper(mainKeyboardService, R.style.Theme_ControlBtn))
    private val backspaceBtn = ImageButton(ContextThemeWrapper(mainKeyboardService, R.style.Theme_ControlBtn))

    private var capsLockMode = 0

    @SuppressLint("ClickableViewAccessibility")
    fun init() {
        for (i in letterList.indices) {
            // add buttons to btnList
            btnList.add(List(letterList[i].size) { Button(ContextThemeWrapper(mainKeyboardService, R.style.Theme_LetterBtn)) })
            // set linear layout attributes
            rowList[i].layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            rowList[i].orientation = LinearLayout.HORIZONTAL
            // create letter buttons and set properties
            for (j in letterList[i].indices) {
                val text = SpannableString(mainKeyboardService.subTextLetterList[i][j] + "\n" + letterList[i][j])
                if (mainKeyboardService.subTextLetterList[i][j] != "") {
                    text.setSpan(
                        ForegroundColorSpan(mainKeyboardService.subtextColor),
                        0,
                        1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                text.setSpan(
                    RelativeSizeSpan(resources.getFloat(R.dimen.text_scale)),
                    text.length - 1,
                    text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                combinedLetterList[i].add(text)
                btnList[i][j].text = text
                btnList[i][j].layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                btnList[i][j].isAllCaps = false
                btnList[i][j].setPadding(0)

                val gestureDetector = GestureDetector(mainKeyboardService, GestureListener(i, j))
                btnList[i][j].setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                }
                // add buttons to linear layouts
                rowList[i].addView(btnList[i][j])
            }
        }
        // set capsLockBtn attributes
        capsLockBtn.setImageDrawable(capsLockMode0Image)
        capsLockBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getFloat(R.dimen.caps_lock_weight)
        )
        capsLockBtn.setOnClickListener {
            mainKeyboardService.vibrate()
            when (capsLockMode) {
                0 -> {
                    setToUppercase()
                    capsLockMode = 1
                    capsLockBtn.setImageDrawable(capsLockMode1Image)
                }
                1 -> {
                    capsLockMode = 2
                    capsLockBtn.setImageDrawable(capsLockMode2Image)
                }
                2 -> {
                    setToLowercase()
                    capsLockMode = 0
                    capsLockBtn.setImageDrawable(capsLockMode0Image)
                }
            }
        }
        rowList[rowList.size-1].addView(capsLockBtn, 0)

        // set backspaceBtn attributes
        backspaceBtn.setImageDrawable(mainKeyboardService.backspaceImage)
        backspaceBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getFloat(R.dimen.backspace_weight)
        )
        backspaceBtn.setOnClickListener {
            mainKeyboardService.vibrate()
            if (mainKeyboardService.currentInputConnection.getSelectedText(0).isNullOrEmpty()) {
                // no selection, so delete previous character
                mainKeyboardService.currentInputConnection.deleteSurroundingText(1, 0)
            } else {
                // delete the selection
                mainKeyboardService.currentInputConnection.commitText("", 1)
            }
        }
        backspaceBtn.setOnLongClickListener {
            Timer().schedule(timerTask {
                if (!backspaceBtn.isPressed || !mainKeyboardService.deleteByWord(-1)) {
                    this.cancel()
                }
            }, 0, mainKeyboardService.rapidTextDeleteInterval)
            return@setOnLongClickListener true
        }

        rowList[rowList.size-1].addView(backspaceBtn, rowList[rowList.size-1].size)
    }

    fun updateSubtextColor() {
        for (i in letterList.indices) {
            for (j in letterList[i].indices) {
                if (mainKeyboardService.subTextLetterList[i][j] != "") {
                    combinedLetterList[i][j].setSpan(
                        ForegroundColorSpan(mainKeyboardService.subtextColor),
                        0,
                        1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    fun insertLetterBtns() {
        for (i in rowList.size - 1 downTo 0) {
            mainKeyboardView.addView(rowList[i], 1)
        }
    }

    private fun setToUppercase() {
        for (i in letterList.indices) {
            for (j in letterList[i].indices) {
                btnList[i][j].isAllCaps = true
            }
        }
    }

    fun setToLowercase() {
        for (i in letterList.indices) {
            for (j in letterList[i].indices) {
                btnList[i][j].isAllCaps = false
            }
        }
    }

    private inner class GestureListener(
        private val i: Int,
        private val j: Int
        ) : GestureDetector.OnGestureListener {

        override fun onDown(event: MotionEvent): Boolean {
            btnList[i][j].isPressed = true
            mainKeyboardService.vibrate()
            return true
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            btnList[i][j].isPressed = false
            if (capsLockMode == 0) {
                mainKeyboardService.currentInputConnection.commitText(letterList[i][j], 1)
            } else {
                if (capsLockMode == 1) {
                    setToLowercase()
                    capsLockBtn.setImageDrawable(capsLockMode0Image)
                    capsLockMode = 0
                }
                mainKeyboardService.currentInputConnection.commitText(letterList[i][j].uppercase(), 1)
            }
            return true
        }

        override fun onLongPress(event: MotionEvent) {
            mainKeyboardService.vibrate()
            mainKeyboardService.resetAndFinishComposing()
            mainKeyboardService.currentInputConnection.commitText(mainKeyboardService.subTextLetterList[i][j], 1)
        }

        override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
            if (p0.rawX - p1.rawX > gestureMinDist) {
                mainKeyboardService.deleteByWord(-1)
                return true
            }
            else if (p1.rawX - p0.rawX > gestureMinDist) {
                mainKeyboardService.deleteByWord(1)
                return true
            }
            return false
        }

        override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
            return true
        }

        override fun onShowPress(p0: MotionEvent?) {

        }
    }
}
