package pro.niva.reader

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Niva Reader: Руководство"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)

        val scrollView = ScrollView(this)
        val content = TextView(this).apply {
            text = "Добро пожаловать в Niva Reader!\n\nЗдесь будет отображаться текст документов, мануалов и заметок.\n\nБазовая архитектура приложения успешно запущена и готова к работе."
            textSize = 16f
        }
        scrollView.addView(content)
        layout.addView(scrollView)

        setContentView(layout)
    }
}
