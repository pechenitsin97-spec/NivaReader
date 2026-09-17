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

class MainActivity : Activity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var btnSaveCsv: Button
    
    private val PICK_FILE_REQUEST = 101
    private val CREATE_CSV_REQUEST = 102
    
    private val ecuParamsMap: MutableMap<String, List<Int>> = mutableMapOf()
    private val csvLines = mutableListOf<String>()

    // === ПАМЯТЬ ПРИЛОЖЕНИЯ (Для склейки пакетов от OpenDiag) ===
    private var lastRpm = 0
    private var lastCoolant = 0
    private var lastSpeed = 0
    private var lastPedal = 0
    private var lastUoz = 0
    private var lastMaf = 0.0
    private var lastInj = 0.0
    private var lastStft = 0.0
    private var lastVolt = 0.0
    private var lastBal1 = 0
    private var lastBal2 = 0
    private var lastBal3 = 0
    private var lastBal4 = 0
    private var lastMis1 = 0
    private var lastMis2 = 0
    private var lastMis3 = 0
    private var lastMis4 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(16, 16, 16, 16)

        val title = TextView(this)
        title.text = "Niva Reader: Smart ECU Decoder"
        title.textSize = 18f
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 8)
        mainLayout.addView(title)

        val menuLayout = LinearLayout(this)
        menuLayout.orientation = LinearLayout.VERTICAL
        menuLayout.setPadding(0, 0, 0, 8)

        val btnOpenLog = Button(this)
        btnOpenLog.text = "📁 Открыть лог OpenDiag (.log / .txt)"
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
        btnSaveCsv.text = "💾 Сохранить в .csv (Excel)"
        
        val saveParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        saveParams.setMargins(0, 16, 0, 0)
        btnSaveCsv.layoutParams = saveParams
        
        btnSaveCsv.isEnabled = false 
        btnSaveCsv.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "Niva_Telemetry.csv")
            }
            startActivityForResult(intent, CREATE_CSV_REQUEST)
        }
        menuLayout.addView(btnSaveCsv)

        mainLayout.addView(menuLayout)

        statusText = TextView(this)
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

        try {
            val loaded = loadEcuParamsSafe(assets)
            ecuParamsMap.putAll(loaded)
            statusText.text = "Готово к работе. Откройте лог."
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
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "params") {
                            currentParamName = parser.getAttributeValue(null, "name") ?: ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (currentParamName.isNotEmpty()) {
                            val text = parser.text?.trim()
                            if (!text.isNullOrEmpty() && text.contains(",")) {
                                val intList = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                                if (intList.isNotEmpty()) {
                                    map[currentParamName] = intList
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "params") {
                            currentParamName = ""
                        }
                    }
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
            if (data != null && data.data != null) {
                readAndParseLogFile(data.data!!)
            }
        } else if (requestCode == CREATE_CSV_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                saveCsvToFile(data.data!!)
            }
        }
    }

    private fun addHeaderCardToUI(info: String) {
        val cardLayout = LinearLayout(this)
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(24, 24, 24, 24)
        cardLayout.setBackgroundColor(Color.parseColor("#181A1B"))
        
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, 16)
        cardLayout.layoutParams = lp

        val tvTitle = TextView(this)
        tvTitle.text = "⚡ ДАННЫЕ АВТОМОБИЛЯ И ЭБУ"
        tvTitle.textSize = 14f
        tvTitle.setTypeface(null, Typeface.BOLD)
        tvTitle.setTextColor(Color.parseColor("#00FFCC"))
        tvTitle.setPadding(0, 0, 0, 12)
        cardLayout.addView(tvTitle)

        val tvInfo = TextView(this)
        tvInfo.text = info
        tvInfo.textSize = 14f
        tvInfo.setTextColor(Color.parseColor("#E9ECEF"))
        tvInfo.setLineSpacing(0f, 1.3f)
        cardLayout.addView(tvInfo)

        contentContainer.addView(cardLayout)
    }

    private fun readAndParseLogFile(uri: Uri) {
        contentContainer.removeAllViews()
        csvLines.clear()
        
        // Сбрасываем память при открытии нового лога
        lastRpm = 0
        lastCoolant = 0
        lastSpeed = 0
        lastPedal = 0
        lastUoz = 0
        lastMaf = 0.0
        lastInj = 0.0
        lastStft = 0.0
        lastVolt = 0.0
        lastBal1 = 0
        lastBal2 = 0
        lastBal3 = 0
        lastBal4 = 0
        lastMis1 = 0
        lastMis2 = 0
        lastMis3 = 0
        lastMis4 = 0
        
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
                        if (finalHeader.isNotEmpty()) {
                            addHeaderCardToUI(finalHeader)
                        }
                        
                        csvLines.add("--- ИНФОРМАЦИЯ О ЛОГЕ ---")
                        finalHeader.split("\n").forEach { 
                            csvLines.add(it.replace(";", ","))
                        }
                        csvLines.add("-------------------------")
                        csvLines.add("Обороты;Антифриз_C;Скорость_кмч;Педаль_%;УОЗ_град;Воздух_ДМРВ;Впрыск_мс;Коррекция_%;АКБ_В;Баланс_Ц1;Баланс_Ц2;Баланс_Ц3;Баланс_Ц4;Пропуски_Ц1;Пропуски_Ц2;Пропуски_Ц3;Пропуски_Ц4")
                        
                        isHeaderAdded = true
                    }
                    
                    if (text.startsWith("Receive: 62") || text.startsWith("Receive: 61") || text.startsWith("Receive: 49")) {
                        val decodedPid = tryDecodeBoschPacket(text)
                        
                        if (decodedPid != null) {
                            val cardLayout = LinearLayout(this)
                            cardLayout.orientation = LinearLayout.VERTICAL
                            cardLayout.setPadding(16, 12, 16, 12)
                            
                            // Новая логика раскраски: если в тексте карточки есть "❌", красим всю карточку
                            if (decodedPid.startsWith("📝")) {
                                cardLayout.setBackgroundColor(Color.parseColor("#E6FFFA"))
                            } else if (decodedPid.contains("❌")) {
                                cardLayout.setBackgroundColor(Color.parseColor("#FFF0F0"))
                            } else {
                                if (telemetryCount % 2 == 0) {
                                    cardLayout.setBackgroundColor(Color.parseColor("#F8F9FA"))
                                } else {
                                    cardLayout.setBackgroundColor(Color.parseColor("#E9ECEF"))
                                }
                            }
                            telemetryCount++
                            
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.setMargins(0, 4, 0, 4)
                            cardLayout.layoutParams = lp

                            val tvPid = TextView(this)
                            tvPid.text = decodedPid
                            tvPid.textSize = 14f
                            tvPid.setTextColor(Color.parseColor("#212529"))
                            tvPid.setLineSpacing(0f, 1.2f)
                            cardLayout.addView(tvPid)

                            contentContainer.addView(cardLayout)
                        }
                    }
                    totalEvents++
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }
            
            if (telemetryCount > 0) {
                btnSaveCsv.isEnabled = true
            }
            
            statusText.text = "Лог разобран. Строк: $totalEvents (Отрисовано пакетов: $telemetryCount)"
        } catch (e: Exception) {
            statusText.text = "Ошибка чтения лога"
        }
    }

    private fun tryDecodeBoschPacket(line: String): String? {
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                if (did.startsWith("F1") || did.startsWith("90") || did.startsWith("009") || did.startsWith("00A") || did.startsWith("02")) {
                    val sb = StringBuilder()
                    for (i in 3 until parts.size) {
                        val hex = parts[i]
                        if (hex.length == 2 && hex != "AA" && hex != "00") {
                            val charCode = hex.toIntOrNull(16) ?: continue
                            if (charCode in 32..126 || charCode in 1040..1103) {
                                sb.append(charCode.toChar())
                            }
                        }
                    }
                    val textResult = sb.toString().trim()
                    if (textResult.length >= 4 && textResult.matches(Regex(".*[A-Za-z0-9]{4,}.*"))) {
                        return "📝 Паспорт ЭБУ: $textResult"
                    }
                }

                if (line.startsWith("Receive: 62")) {
                    var isUpdated = false
                    
                    // --- ПАКЕТ 01: ОБНОВЛЯЕМ БАЗОВЫЕ ПАРАМЕТРЫ ---
                    if (did == "0001" && parts.size > 50) {
                        try {
                            val tempRaw = parts[4].toIntOrNull(16) ?: 40
                            lastCoolant = tempRaw - 40

                            val rpmH = parts[7].toIntOrNull(16) ?: 0
                            val rpmL = parts[8].toIntOrNull(16) ?: 0
                            lastRpm = ((rpmH * 256) + rpmL) / 4

                            val speedRaw = parts[9].toIntOrNull(16) ?: 0
                            lastSpeed = Math.round(speedRaw * 1.32).toInt()

                            val uozRaw = parts[10].toIntOrNull(16) ?: 0
                            lastUoz = if (uozRaw > 127) uozRaw - 256 else uozRaw

                            val injH = parts[11].toIntOrNull(16) ?: 0
                            val injL = parts[12].toIntOrNull(16) ?: 0
                            val inj = ((injH * 256) + injL) / 200.0
                            lastInj = Math.round(inj * 100) / 100.0

                            val mafH = parts[13].toIntOrNull(16) ?: 0
                            val mafL = parts[14].toIntOrNull(16) ?: 0
                            lastMaf = ((mafH * 256) + mafL) / 10.0

                            val voltRaw = parts[21].toIntOrNull(16) ?: 0
                            lastVolt = voltRaw / 10.0

                            val pedalRaw = parts[22].toIntOrNull(16) ?: 0
                            lastPedal = (pedalRaw * 100) / 255

                            val stftRaw = parts[25].toIntOrNull(16) ?: 128
                            val stftPercent = (stftRaw - 128) * 100.0 / 128.0
                            lastStft = Math.round(stftPercent * 10) / 10.0 

                            val bal1Raw = parts[47].toIntOrNull(16) ?: 0
                            val bal2Raw = parts[48].toIntOrNull(16) ?: 0
                            val bal3Raw = parts[49].toIntOrNull(16) ?: 0
                            val bal4Raw = parts[50].toIntOrNull(16) ?: 0
                            
                            lastBal1 = if (bal1Raw > 127) bal1Raw - 256 else bal1Raw
                            lastBal2 = if (bal2Raw > 127) bal2Raw - 256 else bal2Raw
                            lastBal3 = if (bal3Raw > 127) bal3Raw - 256 else bal3Raw
                            lastBal4 = if (bal4Raw > 127) bal4Raw - 256 else bal4Raw
                            
                            isUpdated = true
                        } catch (e: Exception) {
                            return null
                        }
                    }
                    
                    // --- ПАКЕТ 02: ОБНОВЛЯЕМ ТОЛЬКО ПРОПУСКИ ---
                    else if (did == "0002" && parts.size > 42) {
                        try {
                            lastMis1 = (parts[35].toIntOrNull(16) ?: 0) * 256 + (parts[36].toIntOrNull(16) ?: 0)
                            lastMis2 = (parts[37].toIntOrNull(16) ?: 0) * 256 + (parts[38].toIntOrNull(16) ?: 0)
                            lastMis3 = (parts[39].toIntOrNull(16) ?: 0) * 256 + (parts[40].toIntOrNull(16) ?: 0)
                            lastMis4 = (parts[41].toIntOrNull(16) ?: 0) * 256 + (parts[42].toIntOrNull(16) ?: 0)
                            
                            isUpdated = true
                        } catch (e: Exception) {
                            return null
                        }
                    }

                    // --- СКЛЕЙКА ПАМЯТИ: Печатаем, если обновился ЛЮБОЙ из пакетов ---
                    if (isUpdated) {
                        val sign = if (lastStft > 0) "+" else ""
                        val csvLine = "$lastRpm;$lastCoolant;$lastSpeed;$lastPedal;$lastUoz;$lastMaf;$lastInj;$lastStft;$lastVolt;$lastBal1;$lastBal2;$lastBal3;$lastBal4;$lastMis1;$lastMis2;$lastMis3;$lastMis4"
                        csvLines.add(csvLine)

                        val misfireText = if (lastMis1 > 0 || lastMis2 > 0 || lastMis3 > 0 || lastMis4 > 0) {
                            "❌ Пропуски: Ц1=$lastMis1 | Ц2=$lastMis2 | Ц3=$lastMis3 | Ц4=$lastMis4"
                        } else {
                            "✅ Пропуски: Отсутствуют"
                        }

                        return "🔥 Обороты: $lastRpm об/мин | 🌡 Антифриз: $lastCoolant °C\n" +
                               "🚗 Скорость: $lastSpeed км/ч | ⚡ Педаль: $lastPedal% | ⏱ УОЗ: $lastUoz°\n" +
                               "💨 Воздух: $lastMaf кг/ч | 💉 Впрыск: $lastInj мс | 💧 Корр: $sign$lastStft%\n" +
                               "🔋 АКБ: $lastVolt В | ⚖️ Баланс: [$lastBal1] [$lastBal2] [$lastBal3] [$lastBal4]\n" +
                               misfireText
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
                    writer.write("\uFEFF") 
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

