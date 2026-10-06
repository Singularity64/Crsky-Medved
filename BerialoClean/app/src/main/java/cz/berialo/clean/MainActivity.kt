package cz.berialo.clean

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splash = ImageView(this).apply {
            setImageResource(R.drawable.main_loading_image)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        setContentView(splash)
        Handler(Looper.getMainLooper()).postDelayed({
            setContentView(FrameLayout(this).apply { setBackgroundColor(Color.WHITE) })
        }, 2500)
    }
}
