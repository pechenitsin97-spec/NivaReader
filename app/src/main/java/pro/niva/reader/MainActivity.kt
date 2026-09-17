package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var btnSaveCsv: Button
    
    private val PICK_FILE_REQUEST = 101
    private val CREATE_CSV_REQUEST = 102
    
    private val ecuParamsMap: MutableMap<String, List<Int>> = mutableMapOf()
    private val csvLines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(16, 16, 16, 16)
        mainLayout.setBackgroundColor(Color.parseColor("#F0F2F5")) // Светло-серый фон приложения

        val title = TextView(this)
        title.text = "Niva Reader: Pro Diagnostics"
        title.textSize = 20f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#1A1A1D"))
        title.setPadding(0, 0, 0, 12)
        mainLayout.addView(title)

        val menuLayout = LinearLayout(this)
        menuLayout.orientation = LinearLayout.VERTICAL
        menuLayout.setPadding(0, 0, 0, 12)

        val btnOpenLog = Button(this)
        btnOpenLog.text = "📁 ОТКРЫТЬ ЛОГ OPEN DIAG"
        btnOpenLog.setBackgroundColor(Color.parseColor("#3B82F6"))
        btnOpenLog.setTextColor(Color.WHITE)
        btnOpenLog.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnOpenLog.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }
        menuLayout.addView(btnOpenLog)
        
        btnSaveCsv = Button(this)
        btnSaveCsv.text = "💾 СОХРАНИТЬ .CSV ДЛЯ EXCEL"
        btnSaveCsv.setBackgroundColor(Color.parseColor("#10B981"))
        btnSaveCsv.setTextColor(Color.WHITE)
        
        val saveParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        saveParams.setMargins(0, 12, 0, 0)
        btnSaveCsv.layoutParams = saveParams
        btnSaveCsv.isEnabled = false 
        btnSaveCsv.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "Niva_Telemetry_Pro.csv")
            }
            startActivityForResult(intent, CREATE_CSV_REQUEST)
        }
        menuLayout.addView(btnSaveCsv)

        mainLayout.addView(menuLayout)

        statusText = TextView(this)
        statusText.textSize = 13f
        statusText.setTextColor(Color.parseColor("#4B5563"))
        statusText.setPadding(0, 4, 0, 12)
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

        try {
            val loaded = loadEcuParamsSafe(assets)
            ecuParamsMap.putAll(loaded)
            statusText.text = "Готово к работе. Ожидание лог-файла..."
        } catch (e: Exception) {
            statusText.text = "Готово. Откройте лог."
        }
    }

    private fun loadEcuParamsSafe(assetManager: AssetManager): Map<String, List<Int>> {
        val map = mutableMapOf<String, List<Int>>()
        try {
            val inputStream = assetManager.open("ecuParams.xml")
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(inputStream))
            var eventType = parser.eventType
            var currentParamName = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if (parser.name == "params") currentParamName = parser.getAttributeValue(null, "name") ?: ""
                    XmlPullParser.TEXT -> {
                        if (currentParamName.isNotEmpty()) {
                            val text = parser.text?.trim()
                            if (!text.isNullOrEmpty() && text.contains(",")) {
                                val intList = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                                if (intList.isNotEmpty()) map[currentParamName] = intList
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "params") currentParamName = ""
                }
                eventType = parser.next()
            }
            inputStream.close()
        } catch (e: Exception) {}
        return map
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) readAndParseLogFile(data.data!!)
        } else if (requestCode == CREATE_CSV_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) saveCsvToFile(data.data!!)
        }
    }

    private fun readAndParseLogFile(uri: Uri) {
        contentContainer.removeAllViews()
        csvLines.clear()
        
        var totalEvents = 0
        var telemetryCount = 0
        var headerText = StringBuilder()
        var isHeaderParsed = false
        var isHeaderAdded = false

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                while (line != null) {
                    val text = line.trim()
                    if (text.isEmpty()) {
                        line = reader.readLine()
                        continue
                    }

                    if (!isHeaderParsed) {
                        if (text.startsWith("Time:") || text.startsWith("Send:") || text.startsWith("Receive:")) {
                            isHeaderParsed = true
                        } else {
                            headerText.append(text).append("\n")
                            line = reader.readLine()
                            continue
                        }
                    }

                    if (isHeaderParsed && !isHeaderAdded) {
                        val finalHeader = headerText.toString().trim()
                        
                        // Пишем заголовок в CSV
                        csvLines.add("--- ИНФОРМАЦИЯ О ЛОГЕ ---")
                        finalHeader.split("\n").forEach { csvLines.add(it.replace(";", ",")) }
                        csvLines.add("-------------------------")
                        csvLines.add("Обороты;Антифриз_C;Скорость_кмч;Педаль_%;УОЗ_град;Воздух_ДМРВ;Впрыск_мс;Коррекция_%;АКБ_В;Баланс_Ц1;Баланс_Ц2;Баланс_Ц3;Баланс_Ц4;Пропуски_Ц1;Пропуски_Ц2;Пропуски_Ц3;Пропуски_Ц4")
                        isHeaderAdded = true
                    }
                    
                    if (text.startsWith("Receive: 62") || text.startsWith("Receive: 61") || text.startsWith("Receive: 49")) {
                        val uiCard = tryDecodeBoschPacket(text)
                        
                        if (uiCard != null) {
                            val cardLayout = LinearLayout(this)
                            cardLayout.orientation = LinearLayout.VERTICAL
                            cardLayout.setPadding(24, 20, 24, 20)
                            
                            // Раскраска карточек
                            if (uiCard.contains("ТЕЛЕМЕТРИЯ")) {
                                cardLayout.setBackgroundColor(Color.parseColor("#EBF5FF")) // Нежно-голубой для датчиков
                            } else if (uiCard.contains("ВНИМАНИЕ")) {
                                cardLayout.setBackgroundColor(Color.parseColor("#FEE2E2")) // Красный для пропусков
                            } else if (uiCard.contains("ЧИСТО")) {
                                cardLayout.setBackgroundColor(Color.parseColor("#ECFCCB")) // Светло-зеленый
                            } else {
                                cardLayout.setBackgroundColor(Color.WHITE)
                            }
                            
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.setMargins(0, 8, 0, 8)
                            cardLayout.layoutParams = lp

                            val tvPid = TextView(this)
                            tvPid.text = uiCard
                            tvPid.textSize = 14f
                            tvPid.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                            tvPid.setTextColor(Color.parseColor("#111827"))
                            tvPid.setLineSpacing(0f, 1.3f)
                            cardLayout.addView(tvPid)

                            contentContainer.addView(cardLayout)
                            telemetryCount++
                        }
                    }
                    totalEvents++
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }
            if (telemetryCount > 0) btnSaveCsv.isEnabled = true
            statusText.text = "✅ Лог разобран. Строк: $totalEvents (Отрисовано пакетов: $telemetryCount)"
        } catch (e: Exception) {
            statusText.text = "❌ Ошибка чтения лога"
        }
    }

    private fun tryDecodeBoschPacket(line: String): String? {
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split("\\s+".toRegex()) 
            
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // --- ПАКЕТ 0001: ТОЛЬКО ДАТЧИКИ (Super Precision) ---
                if (did == "0001" && parts.size > 50) {
                    val tempRaw = parts[4].toIntOrNull(16) ?: 40
                    val coolant = tempRaw - 40
                    
                    val rpmH = parts[7].toIntOrNull(16) ?: 0
                    val rpmL = parts[8].toIntOrNull(16) ?: 0
                    val rpm = ((rpmH * 256.0) + rpmL) / 4.0
                    
                    val speedRaw = parts[9].toIntOrNull(16) ?: 0
                    val speed = speedRaw * 1.32
                    
                    val uozRaw = parts[10].toIntOrNull(16) ?: 0
                    val uoz = if (uozRaw > 127) uozRaw - 256 else uozRaw
                    
                    val inj = (((parts[11].toIntOrNull(16) ?: 0) * 256) + (parts[12].toIntOrNull(16) ?: 0)) / 200.0
                    val maf = (((parts[13].toIntOrNull(16) ?: 0) * 256) + (parts[14].toIntOrNull(16) ?: 0)) / 10.0
                    val voltage = (parts[21].toIntOrNull(16) ?: 0) / 10.0
                    val pedal = ((parts[22].toIntOrNull(16) ?: 0) * 100.0) / 255.0
                    
                    val stftRaw = parts[25].toIntOrNull(16) ?: 128
                    val stft = (stftRaw - 128) * 100.0 / 128.0

                    val bal1Raw = parts[47].toIntOrNull(16) ?: 0
                    val bal2Raw = parts[48].toIntOrNull(16) ?: 0
                    val bal3Raw = parts[49].toIntOrNull(16) ?: 0
                    val bal4Raw = parts[50].toIntOrNull(16) ?: 0
                    
                    val balance1 = if (bal1Raw > 127) bal1Raw - 256 else bal1Raw
                    val balance2 = if (bal2Raw > 127) bal2Raw - 256 else bal2Raw
                    val balance3 = if (bal3Raw > 127) bal3Raw - 256 else bal3Raw
                    val balance4 = if (bal4Raw > 127) bal4Raw - 256 else bal4Raw

                    // CSV (Локаль US гарантирует точку в десятичных дробях)
                    val csvLine = String.format(Locale.US, "%.0f;%d;%.1f;%.1f;%d;%.1f;%.2f;%+.1f;%.1f;%d;%d;%d;%d;;;;",
                        rpm, coolant, speed, pedal, uoz, maf, inj, stft, voltage, balance1, balance2, balance3, balance4)
                    csvLines.add(csvLine)

                    // Красивая UI карточка
                    return "🔵 ТЕЛЕМЕТРИЯ [0001]\n" +
                           "🔥 Обороты: ${String.format(Locale.US, "%.0f", rpm)} об/мин | 🌡 Темп: $coolant °C\n" +
                           "🚗 Скорость: ${String.format(Locale.US, "%.1f", speed)} км/ч  | ⚡ Педаль: ${String.format(Locale.US, "%.1f", pedal)}%\n" +
                           "💨 Воздух: ${String.format(Locale.US, "%.1f", maf)} кг/ч    | 💉 Впрыск: ${String.format(Locale.US, "%.2f", inj)} мс\n" +
                           "💧 Коррекция: ${String.format(Locale.US, "%+.1f", stft)}%  | ⏱ УОЗ: $uoz°\n" +
                           "🔋 АКБ: ${String.format(Locale.US, "%.1f", voltage)} В\n" +
                           "⚖️ Баланс: [$balance1] [$balance2] [$balance3] [$balance4]"
                }

                // --- ПАКЕТ 0002: ТОЛЬКО ПРОПУСКИ ---
                if (did == "0002" && parts.size > 42) {
                    val tempRaw = parts[4].toIntOrNull(16) ?: 40
                    val coolant = tempRaw - 40
                    val rpmH = parts[7].toIntOrNull(16) ?: 0
                    val rpmL = parts[8].toIntOrNull(16) ?: 0
                    val rpm = ((rpmH * 256.0) + rpmL) / 4.0

                    val misfire1 = (parts[35].toIntOrNull(16) ?: 0) * 256 + (parts[36].toIntOrNull(16) ?: 0)
                    val misfire2 = (parts[37].toIntOrNull(16) ?: 0) * 256 + (parts[38].toIntOrNull(16) ?: 0)
                    val misfire3 = (parts[39].toIntOrNull(16) ?: 0) * 256 + (parts[40].toIntOrNull(16) ?: 0)
                    val misfire4 = (parts[41].toIntOrNull(16) ?: 0) * 256 + (parts[42].toIntOrNull(16) ?: 0)

                    // Пишем в CSV пустые ячейки для датчиков, чтобы не ломать графики
                    val csvLine = String.format(Locale.US, "%.0f;%d;;;;;;;;;;;;%d;%d;%d;%d",
                        rpm, coolant, misfire1, misfire2, misfire3, misfire4)
                    csvLines.add(csvLine)

                    if (misfire1 > 0 || misfire2 > 0 || misfire3 > 0 || misfire4 > 0) {
                        return "🔴 ВНИМАНИЕ: ПРОПУСКИ ЗАЖИГАНИЯ!\n" +
                               "🔥 Обороты: ${String.format(Locale.US, "%.0f", rpm)} об/мин | 🌡 Темп: $coolant °C\n" +
                               "❌ Ц1: $misfire1 | Ц2: $misfire2 | Ц3: $misfire3 | Ц4: $misfire4"
                    } else {
                        return "🟢 ДИАГНОСТИКА ПРОПУСКОВ (ЧИСТО)\n" +
                               "🔥 Обороты: ${String.format(Locale.US, "%.0f", rpm)} об/мин | 🌡 Темп: $coolant °C\n" +
                               "✅ Пропуски отсутствуют"
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun saveCsvToFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write("\uFEFF") // BOM для нормального отображения в Excel
                    for (line in csvLines) {
                        writer.write(line + "\n")
                    }
                }
            }
            statusText.text = "✅ Успешно сохранено! (Строк: ${csvLines.size})"
        } catch (e: Exception) {
            statusText.text = "❌ Ошибка при сохранении CSV файла"
        }
    }
}

