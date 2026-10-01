package cz.ceskymedvidek.app

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BerialoBearView(context: android.content.Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brown = Color.rgb(176, 112, 63)
    private val dark = Color.rgb(78, 48, 31)
    private val muzzle = Color.rgb(241, 194, 142)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * .30f

        p.style = Paint.Style.FILL
        p.color = brown
        c.drawCircle(cx - r * .72f, cy - r * .70f, r * .42f, p)
        c.drawCircle(cx + r * .72f, cy - r * .70f, r * .42f, p)
        c.drawCircle(cx, cy, r * 1.15f, p)

        p.color = muzzle
        c.drawOval(RectF(cx-r*.62f, cy+r*.05f, cx+r*.62f, cy+r*.76f), p)

        p.color = dark
        c.drawCircle(cx-r*.36f, cy-r*.22f, r*.11f, p)
        c.drawCircle(cx+r*.36f, cy-r*.22f, r*.11f, p)
        c.drawOval(RectF(cx-r*.14f, cy+r*.12f, cx+r*.14f, cy+r*.32f), p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = r * .07f
        p.strokeCap = Paint.Cap.ROUND
        c.drawArc(RectF(cx-r*.30f, cy+r*.22f, cx+r*.30f, cy+r*.58f), 12f, 156f, false, p)
    }
}

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var playButton: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive()
        showWelcome()
        handler.postDelayed({ revealPlay() }, 2000L)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun immersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun showWelcome() {
        val bg = Color.rgb(255, 248, 232)
        val root = FrameLayout(this).apply { setBackgroundColor(bg) }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        content.addView(BerialoBearView(this), LinearLayout.LayoutParams(dp(230), dp(230)))

        content.addView(TextView(this).apply {
            text = "BERIALO"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(80, 50, 34))
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .08f
        }, LinearLayout.LayoutParams(-1, dp(72)))

        content.addView(TextView(this).apply {
            text = "Hraj si • poslouchej • objevuj"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(125, 92, 67))
        }, LinearLayout.LayoutParams(-1, dp(48)))

        val play = TextView(this).apply {
            text = "▶"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            alpha = 0f
            isEnabled = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(245, 145, 45))
                setStroke(dp(5), Color.WHITE)
            }
            elevation = dp(10).toFloat()
            setOnClickListener { showBlankScreen() }
        }
        playButton = play
        content.addView(play, LinearLayout.LayoutParams(dp(128), dp(128)).apply {
            topMargin = dp(24)
        })

        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun revealPlay() {
        playButton?.apply {
            isEnabled = true
            animate().alpha(1f).scaleX(1.08f).scaleY(1.08f).setDuration(280).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }.start()
        }
    }

    private fun showBlankScreen() {
        handler.removeCallbacksAndMessages(null)
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.WHITE) })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
