package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var statusText: TextView
    private val PICK_FILE_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(16, 16, 16, 16)

        val title = TextView(this)
        title.text = "Niva Reader: OpenDiag Log Monitor"
        title.textSize = 18f
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 8)
        mainLayout.addView(title)

        val menuLayout = LinearLayout(this)
        menuLayout.orientation = LinearLayout.VERTICAL
        menuLayout.setPadding(0, 0, 0, 8)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.setPadding(0, 0, 0, 6)

        val btnEngine = Button(this)
        btnEngine.text = "Двигатель"
        btnEngine.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        btnEngine.setOnClickListener {
            showManualText(
                "Раздел: Двигатель и ЭБУ",
                "• Диагностика и параметры датчиков\n• Проверка калибровки контроллера\n• Регламент замены расходников"
            )
        }

        val btnSuspension = Button(this)
        btnSuspension.text = "Подвеска"
        btnSuspension.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        btnSuspension.setOnClickListener {
            showManualText(
                "Раздел: Подвеска и колеса",
                "• Обслуживание ступичных узлов\n• Регулировка давления в шинах (для зимы рекомендуется 1.8 атм)\n• Проверка элементов подвески"
            )
        }

        val btnAudio = Button(this)
        btnAudio.text = "Электрика"
        btnAudio.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        btnAudio.setOnClickListener {
            showManualText(
                "Раздел: Электрика и аудио",
                "• Прокладка силовых линий и кабеля 1.5 мм²\n• Прямое подключение головного устройства к АКБ\n• Контроль предохранителей"
            )
        }

        row1.addView(btnEngine)
        row1.addView(btnSuspension)
        row1.addView(btnAudio)
        menuLayout.addView(row1)

        val btnOpenLog = Button(this)
        btnOpenLog.text = "📁 Открыть сырой лог OpenDiag (.log / .txt)"
        btnOpenLog.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        btnOpenLog.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }
        menuLayout.addView(btnOpenLog)
        mainLayout.addView(menuLayout)

        statusText = TextView(this)
        statusText.text = "Выберите раздел мануала или откройте лог-файл."
        statusText.textSize = 13f
        statusText.setTextColor(Color.DKGRAY)
        statusText.setPadding(0, 4, 0, 8)
        mainLayout.addView(statusText)

        contentContainer = LinearLayout(this)
        contentContainer.orientation = LinearLayout.VERTICAL
        contentContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val verticalScroll = ScrollView(this)
        verticalScroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        verticalScroll.addView(contentContainer)
        mainLayout.addView(verticalScroll)

        setContentView(mainLayout)

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала или откройте лог OpenDiag для анализа обмена с ЭБУ.")
    }

    private fun showManualText(heading: String, body: String) {
        contentContainer.removeAllViews()
        statusText.text = heading

        val tvHeading = TextView(this)
        tvHeading.text = heading
        tvHeading.textSize = 16f
        tvHeading.setTextColor(Color.BLACK)
        tvHeading.setTypeface(null, android.graphics.Typeface.BOLD)
        tvHeading.setPadding(0, 0, 0, 8)

        val tvBody = TextView(this)
        tvBody.text = body
        tvBody.textSize = 14f
        tvBody.setTextColor(Color.DKGRAY)

        contentContainer.addView(tvHeading)
        contentContainer.addView(tvBody)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                readAndParseLogFile(data.data!!)
            }
        }
    }

    private fun readAndParseLogFile(uri: Uri) {
        contentContainer.removeAllViews()
        var totalEvents = 0

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                var currentTime = ""

                while (line != null) {
                    val text = line.trim()
                    if (text.isNotEmpty()) {
                        // Распознаем блоки лога OpenDiag
                        if (text.startsWith("Time:")) {
                            currentTime = text.replace("Time:", "").trim()
                        } else if (text.startsWith("Send:") || text.startsWith("Receive:") || text.startsWith("AppVersion") || text.startsWith("ECU")) {
                            
                            // Создаем красивую карточку для каждой записи обмена
                            val cardLayout = LinearLayout(this)
                            cardLayout.orientation = LinearLayout.VERTICAL
                            cardLayout.setPadding(12, 8, 12, 8)
                            cardLayout.setBackgroundColor(Color.parseColor("#F0F0F0"))
                            
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.setMargins(0, 4, 0, 4)
                            cardLayout.layoutParams = lp

                            if (currentTime.isNotEmpty() && text.startsWith("Send")) {
                                val tvTime = TextView(this)
                                tvTime.text = "⏱ Время: $currentTime"
                                tvTime.textSize = 11f
                                tvTime.setTextColor(Color.GRAY)
                                cardLayout.addView(tvTime)
                            }

                            val tvData = TextView(this)
                            tvData.text = text
                            tvData.textSize = 13f
                            
                            if (text.startsWith("Send:")) {
                                tvData.setTextColor(Color.parseColor("#0066CC")) // Синий для запросов
                            } else if (text.startsWith("Receive:")) {
                                tvData.setTextColor(Color.parseColor("#008800")) // Зеленый для ответов ЭБУ
                            } else {
                                tvData.setTextColor(Color.BLACK)
                            }
                            
                            cardLayout.addView(tvData)
                            contentContainer.addView(cardLayout)
                            totalEvents++
                        }
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }

            statusText.text = "Лог успешно разобран. Записей: $totalEvents"

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения лога: ${e.localizedMessage}"
        }
    }
}
