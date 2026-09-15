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
        title.text = "Niva Reader: Ultimate OBD-II Decoder"
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
        btnOpenLog.text = "📁 Открыть лог OpenDiag (.log / .txt)"
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

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала или откройте лог OpenDiag для глубокого анализа.")
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
                        if (text.startsWith("Time:")) {
                            currentTime = text.replace("Time:", "").trim()
                        } else if (text.startsWith("Send:") || text.startsWith("Receive:") || text.startsWith("AppVersion") || text.startsWith("ECU")) {
                            
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
                                tvData.setTextColor(Color.parseColor("#0066CC"))
                            } else if (text.startsWith("Receive:")) {
                                tvData.setTextColor(Color.parseColor("#008800"))
                            } else {
                                tvData.setTextColor(Color.BLACK)
                            }
                            cardLayout.addView(tvData)

                            // 1. Декодер текста
                            val decodedText = tryDecodeHexToAscii(text)
                            if (decodedText != null) {
                                val tvDecoded = TextView(this)
                                tvDecoded.text = "💡 Текст: $decodedText"
                                tvDecoded.textSize = 12f
                                tvDecoded.setTextColor(Color.parseColor("#990000"))
                                tvDecoded.setTypeface(null, android.graphics.Typeface.BOLD)
                                cardLayout.addView(tvDecoded)
                            }

                            // 2. Полный декодер параметров OBD-II (включая коррекции, давление, масло и т.д.)
                            val decodedPid = tryDecodeOBDPid(text)
                            if (decodedPid != null) {
                                val tvPid = TextView(this)
                                tvPid.text = "📊 $decodedPid"
                                tvPid.textSize = 13f
                                tvPid.setTextColor(Color.parseColor("#B22222"))
                                tvPid.setTypeface(null, android.graphics.Typeface.BOLD)
                                cardLayout.addView(tvPid)
                            }

                            contentContainer.addView(cardLayout)
                            totalEvents++
                        }
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }

            statusText.text = "Лог полностью разобран. Записей: $totalEvents"

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения лога: ${e.localizedMessage}"
        }
    }

    private fun tryDecodeHexToAscii(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val parts = line.replace("Receive:", "").trim().split(" ")
            if (parts.size > 3) {
                val sb = StringBuilder()
                for (i in 3 until parts.size) {
                    val hex = parts[i]
                    if (hex.length == 2) {
                        val charCode = hex.toInt(16)
                        if (charCode in 32..126) {
                            sb.append(charCode.toChar())
                        }
                    }
                }
                val result = sb.toString().trim()
                if (result.length > 2) return result
            }
        } catch (e: Exception) {}
        return null
    }

    private fun tryDecodeOBDPid(line: String): String? {
        if (!line.startsWith("Receive:")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3 && parts[0] == "41") {
                val pid = parts[1]
                when (pid) {
                    "03" -> return "Статус топливной системы: режим замкнут/разомкнут"
                    "04" -> {
                        val a = parts[2].toInt(16)
                        return "Нагрузка двигателя: ${(a * 100) / 255} %"
                    }
                    "05" -> {
                        val a = parts[2].toInt(16)
                        return "Температура антифриза: ${a - 40} °C"
                    }
                    "06" -> {
                        val a = parts[2].toInt(16)
                        val trim = String.format("%.1f", (a - 128) * 100.0 / 128.0)
                        return "Краткосрочная топливная коррекция (Банк 1): $trim %"
                    }
                    "07" -> {
                        val a = parts[2].toInt(16)
                        val trim = String.format("%.1f", (a - 128) * 100.0 / 128.0)
                        return "Долгосрочная топливная коррекция (Банк 1): $trim %"
                    }
                    "0A" -> {
                        val a = parts[2].toInt(16)
                        return "Давление топлива: ${a * 3} кПа"
                    }
                    "0B" -> {
                        val a = parts[2].toInt(16)
                        return "Давление во впускном коллекторе (ДАД): $a кПа"
                    }
                    "0C" -> {
                        if (parts.size >= 4) {
                            val a = parts[2].toInt(16)
                            val b = parts[3].toInt(16)
                            return "Обороты двигателя: ${((a * 256) + b) / 4} об/мин"
                        }
                    }
                    "0D" -> {
                        val a = parts[2].toInt(16)
                        return "Скорость автомобиля: $a км/ч"
                    }
                    "0E" -> {
                        val a = parts[2].toInt(16)
                        return "Угол опережения зажигания: ${(a - 128) / 2.0} °"
                    }
                    "0F" -> {
                        val a = parts[2].toInt(16)
                        return "Температура воздуха на впуске: ${a - 40} °C"
                    }
                    "10" -> {
                        if (parts.size >= 4) {
                            val a = parts[2].toInt(16)
                            val b = parts[3].toInt(16)
                            return "Массовый расход воздуха (ДМРВ): ${((a * 256) + b) / 100.0} г/с"
                        }
                    }
                    "11" -> {
                        val a = parts[2].toInt(16)
                        return "Положение дроссельной заслонки: ${(a * 100) / 255} %"
                    }
                    "1F" -> {
                        if (parts.size >= 4) {
                            val a = parts[2].toInt(16)
                            val b = parts[3].toInt(16)
                            return "Время с момента запуска двигателя: ${(a * 256) + b} сек"
                        }
                    }
                    "2F" -> {
                        val a = parts[2].toInt(16)
                        return "Уровень топлива в баке: ${(a * 100) / 255} %"
                    }
                    "33" -> {
                        val a = parts[2].toInt(16)
                        return "Атмосферное давление: $a кПа"
                    }
                    "42" -> {
                        if (parts.size >= 4) {
                            val a = parts[2].toInt(16)
                            val b = parts[3].toInt(16)
                            return "Напряжение бортовой сети: ${((a * 256) + b) / 1000.0} В"
                        }
                    }
                    "46" -> {
                        val a = parts[2].toInt(16)
                        return "Температура воздуха за бортом: ${a - 40} °C"
                    }
                    "5C" -> {
                        val a = parts[2].toInt(16)
                        return "Температура моторного масла: ${a - 40} °C"
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }
}

