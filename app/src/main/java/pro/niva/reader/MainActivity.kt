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
        btnSaveCsv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
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
        cardLayout.setBackgroundColor(Color.parseColor("#181A1B")) // Графитовый фон как на иконке
        
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
        tvTitle.setTextColor(Color.parseColor("#00FFCC")) // Неоновый зеленый как на молнии
        tvTitle.setPadding(0, 0, 0, 12)
        cardLayout.addView(tvTitle)

        val tvInfo = TextView(this)
        tvInfo.text = info
        tvInfo.textSize = 14f
        tvInfo.setTextColor(Color.parseColor("#E9ECEF")) // Светло-серый текст
        tvInfo.setLineSpacing(0f, 1.3f)
        cardLayout.addView(tvInfo)

        contentContainer.addView(cardLayout)
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

                    // 1. Собираем шапку лога (всё, что до команд Time/Send/Receive)
                    if (!isHeaderParsed) {
                        if (text.startsWith("Time:") || text.startsWith("Send:") || text.startsWith("Receive:")) {
                            isHeaderParsed = true
                        } else {
                            headerText.append(text).append("\n")
                            line = reader.readLine()
                            continue
                        }
                    }

                    // 2. Инициализируем UI и CSV, как только шапка собрана
                    if (isHeaderParsed && !isHeaderAdded) {
                        val finalHeader = headerText.toString().trim()
                        if (finalHeader.isNotEmpty()) {
                            addHeaderCardToUI(finalHeader) // Выводим красивую карточку на экран
                        }
                        
                        // Добавляем шапку в Excel файл
                        csvLines.add("--- ИНФОРМАЦИЯ О ЛОГЕ ---")
                        finalHeader.split("\n").forEach { 
                            csvLines.add(it.replace(";", ",")) // Защита от случайных разделителей
                        }
                        csvLines.add("-------------------------")
                        csvLines.add("Обороты;Антифриз_C;Скорость_кмч;Педаль_%;УОЗ_град;Воздух_ДМРВ;Впрыск_мс;Коррекция_%;АКБ_В;Шум_Ц1;Шум_Ц2;Шум_Ц3;Шум_Ц4")
                        
                        isHeaderAdded = true
                    }
                    
                    // 3. Разбираем телеметрию
                    if (text.startsWith("Receive: 62")) {
                        val decodedPid = tryDecodeBoschPacket(text)
                        
                        if (decodedPid != null) {
                            val cardLayout = LinearLayout(this)
                            cardLayout.orientation = LinearLayout.VERTICAL
                            cardLayout.setPadding(16, 12, 16, 12)
                            
                            if (telemetryCount % 2 == 0) {
                                cardLayout.setBackgroundColor(Color.parseColor("#F8F9FA"))
                            } else {
                                cardLayout.setBackgroundColor(Color.parseColor("#E9ECEF"))
                            }
                            
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
                            telemetryCount++
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
            
            statusText.text = "Лог разобран. Строк: $totalEvents (Найдено пакетов: $telemetryCount)"
        } catch (e: Exception) {
            statusText.text = "Ошибка чтения лога"
        }
    }

    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                if (did >= "0090" && did <= "00A3") {
                    val sb = StringBuilder()
                    for (i in 3 until parts.size) {
                        val hex = parts[i]
                        if (hex.length == 2 && hex != "AA") {
                            val charCode = hex.toIntOrNull(16) ?: continue
                            if (charCode in 32..126) {
                                sb.append(charCode.toChar())
                            }
                        }
                    }
                    val textResult = sb.toString().trim()
                    if (textResult.isNotEmpty()) {
                        return "📝 Паспорт [$did]: $textResult"
                    }
                }

                if (did == "0001" && parts.size > 50) {
                    try {
                        val tempRaw = parts[4].toIntOrNull(16) ?: 40
                        val coolant = tempRaw - 40

                        val rpmH = parts[7].toIntOrNull(16) ?: 0
                        val rpmL = parts[8].toIntOrNull(16) ?: 0
                        val rpm = ((rpmH * 256) + rpmL) / 4

                        val speed = parts[9].toIntOrNull(16) ?: 0

                        val uozRaw = parts[10].toIntOrNull(16) ?: 0
                        val uoz = if (uozRaw > 127) uozRaw - 256 else uozRaw

                        val injH = parts[11].toIntOrNull(16) ?: 0
                        val injL = parts[12].toIntOrNull(16) ?: 0
                        val inj = ((injH * 256) + injL) / 200.0
                        val injRounded = Math.round(inj * 100) / 100.0

                        val mafH = parts[13].toIntOrNull(16) ?: 0
                        val mafL = parts[14].toIntOrNull(16) ?: 0
                        val maf = ((mafH * 256) + mafL) / 10.0

                        val voltRaw = parts[21].toIntOrNull(16) ?: 0
                        val voltage = voltRaw / 10.0

                        val pedalRaw = parts[22].toIntOrNull(16) ?: 0
                        val pedal = (pedalRaw * 100) / 255

                        val stftRaw = parts[25].toIntOrNull(16) ?: 128
                        val stftPercent = (stftRaw - 128) * 100.0 / 128.0
                        val stftRounded = Math.round(stftPercent * 10) / 10.0 
                        val sign = if (stftRounded > 0) "+" else ""

                        val noise1 = parts[47].toIntOrNull(16) ?: 0
                        val noise2 = parts[48].toIntOrNull(16) ?: 0
                        val noise3 = parts[49].toIntOrNull(16) ?: 0
                        val noise4 = parts[50].toIntOrNull(16) ?: 0
                        
                        val csvLine = "$rpm;$coolant;$speed;$pedal;$uoz;$maf;$injRounded;$stftRounded;$voltage;$noise1;$noise2;$noise3;$noise4"
                        csvLines.add(csvLine)

                        return "🔥 Обороты: $rpm об/мин | 🌡 Антифриз: $coolant °C\n" +
                               "🚗 Скорость: $speed км/ч | ⚡ Педаль: $pedal% | ⏱ УОЗ: $uoz°\n" +
                               "💨 Воздух: $maf кг/ч | 💉 Впрыск: $injRounded мс | 💧 Корр: $sign$stftRounded%\n" +
                               "🔋 АКБ: $voltage В | 📊 Шум (Цил 1-4): [$noise1] [$noise2] [$noise3] [$noise4]"
                    } catch (e: Exception) {
                        return null
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

