package pro.niva.reader

import android.app.Activity
import android.os.Bundle
import android.widget.Button
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
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        // Текстовое поле, содержимое которого меняется при нажатии на кнопки
        val content = TextView(this).apply {
            text = "Выберите нужный раздел в меню выше, чтобы открыть информацию."
            textSize = 16f
        }

        // Горизонтальный блок для кнопок
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val btnEngine = Button(this).apply {
            text = "Двигатель"
            setOnClickListener {
                content.text = "Раздел: Двигатель и ЭБУ\n\n• Диагностика и параметры датчиков\n• Проверка калибровки контроллера\n• Регламент замены расходников"
            }
        }

        val btnSuspension = Button(this).apply {
            text = "Подвеска"
            setOnClickListener {
                content.text = "Раздел: Подвеска и колеса\n\n• Обслуживание ступичных узлов\n• Регулировка давления в шинах (для заснеженных покрытий рекомендуется 1.8 атм)\n• Проверка элементов подвески"
            }
        }

        val btnAudio = Button(this).apply {
            text = "Электрика"
            setOnClickListener {
                content.text = "Раздел: Электрика и аудио\n\n• Прокладка силовых линий и акустического кабеля сечением 1.5 мм²\n• Прямое подключение головного устройства к АКБ\n• Контроль предохранителей"
            }
        }

        buttonLayout.addView(btnEngine)
        buttonLayout.addView(btnSuspension)
        buttonLayout.addView(btnAudio)
        layout.addView(buttonLayout)

        val scrollView = ScrollView(this)
        scrollView.addView(content)
        layout.addView(scrollView)

        setContentView(layout)
    }
}
