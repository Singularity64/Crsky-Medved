package cz.ceskymedvidek.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val image = ImageView(this).apply {
            setImageResource(R.drawable.main_loading_image)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.WHITE)
        }
        setContentView(image, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        handler.postDelayed({
            setContentView(FrameLayout(this).apply {
                setBackgroundColor(Color.WHITE)
            })
        }, 2500L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
