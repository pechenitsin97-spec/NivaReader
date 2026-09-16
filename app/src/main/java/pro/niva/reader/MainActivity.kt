package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
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

class MainActivity : Activity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var statusText: TextView
    private val PICK_FILE_REQUEST = 101
    
    // Карта параметров (оставляем для совместимости и загрузки паспортов)
    private val ecuParamsMap: MutableMap<String, List<Int>> = mutableMapOf()

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
        }
    }

    private fun readAndParseLogFile(uri: Uri) {
        contentContainer.removeAllViews()
        var totalEvents = 0
        var telemetryCount = 0

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                while (line != null) {
                    val text = line.trim()
                    
                    // Обрабатываем только строки с данными от ЭБУ
                    if (text.isNotEmpty() && text.startsWith("Receive: 62")) {
                        
                        val decodedPid = tryDecodeBoschPacket(text)
                        
                        // Если пакет успешно расшифрован
                        if (decodedPid != null) {
                            val cardLayout = LinearLayout(this)
                            cardLayout.orientation = LinearLayout.VERTICAL
                            cardLayout.setPadding(16, 12, 16, 12)
                            
                            // Чередуем цвета карточек для красоты (зебра)
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
                
                // 1. Паспорта ЭБУ 
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

                // 2. Чистая телеметрия 0001 (ПЛЮС НАПРЯЖЕНИЕ АКБ И ПРОПУСКИ ЗАЖИГАНИЯ)
                if (did == "0001" && parts.size > 50) {
                    try {
                        val tempRaw = parts[4].toIntOrNull(16) ?: 40
                        val coolant = tempRaw - 40

                        val rpmH = parts[7].toIntOrNull(16) ?: 0
                        val rpmL = parts[8].toIntOrNull(16) ?: 0
                        val rpm = ((rpmH * 256) + rpmL) / 4

                        val speed = parts[9].toIntOrNull(16) ?: 0

                        val tpsRaw = parts[10].toIntOrNull(16) ?: 0
                        val tps = (tpsRaw * 100) / 255

                        val mafH = parts[13].toIntOrNull(16) ?: 0
                        val mafL = parts[14].toIntOrNull(16) ?: 0
                        val maf = ((mafH * 256) + mafL) / 10.0

                        val voltRaw = parts[21].toIntOrNull(16) ?: 0
                        val voltage = voltRaw / 10.0

                        // Счетчики пропусков воспламенения по цилиндрам (Байты 47, 48, 49, 50)
                        val misfire1 = parts[47].toIntOrNull(16) ?: 0
                        val misfire2 = parts[48].toIntOrNull(16) ?: 0
                        val misfire3 = parts[49].toIntOrNull(16) ?: 0
                        val misfire4 = parts[50].toIntOrNull(16) ?: 0

                        return "🔥 Обороты: $rpm об/мин | 🌡 Антифриз: $coolant °C\n" +
                               "🚗 Скорость: $speed км/ч | ⚡ Дроссель: $tps%\n" +
                               "💨 Воздух (ДМРВ): $maf кг/ч | 🔋 АКБ: $voltage В\n" +
                               "💥 Пропуски (Цил 1-2-3-4): [$misfire1] [$misfire2] [$misfire3] [$misfire4]"
                    } catch (e: Exception) {
                        return null
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }
}

