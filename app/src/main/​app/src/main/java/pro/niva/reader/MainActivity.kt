package pro.niva.reader

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.text = "Niva Reader Works!"
        text.textSize = 24f
        setContentView(text)
    }
}
