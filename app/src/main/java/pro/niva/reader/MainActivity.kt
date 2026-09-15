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

    // Хранилище для таблиц коэффициентов из XML[span_1](start_span)[span_1](end_span)
    private val ecuParamsMap = mutableMapOf<String, List<Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Загружаем заводские карты ЭБУ из файла при старте
        ecuParamsMap.putAll(loadEcuParams(assets))

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
        statusText.text = "Карты ЭБУ загружены. Записей коэффициентов: ${ecuParamsMap.size}. Откройте лог."
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
    }

    private fun loadEcuParams(assetManager: AssetManager): Map<String, List<Int>> {
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
                                val intList = text.split(",").mapNotNull { it.toIntOrNull() }
                                map[currentParamName] = intList
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                while (line != null) {
                    val text = line.trim()
                    if (text.isNotEmpty() && (text.startsWith("Send:") || text.startsWith("Receive:") || text.startsWith("ECU"))) {
                        
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

                        val tvData = TextView(this)
                        tvData.text = text
                        tvData.textSize = 13f
                        tvData.setTextColor(if (text.startsWith("Send:")) Color.parseColor("#0066CC") else Color.parseColor("#008800"))
                        cardLayout.addView(tvData)

                        // Распознаем Bosch ME17 пакеты (Receive: 62 ...)
                        val decodedPid = tryDecodeBoschPacket(text)
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
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }
            statusText.text = "Лог разобран. Записей: $totalEvents (Bosch ME17 парсер активен)"
        } catch (e: Exception) {
            statusText.text = "Ошибка чтения лога: ${e.localizedMessage}"
        }
    }

    // Декодировщик ответов Bosch ME17 (начинаются с Receive: 62)
    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                // Идентификатор параметра (например, 00 01)
                val did = "${parts[1]}${parts[2]}"
                
                // Берем коэффициенты из XML для расчетов (если они есть в карте)
                val scale = ecuParamsMap["B17s01"]?.getOrNull(0) ?: 1
                val offset = ecuParamsMap["B17o01"]?.getOrNull(0) ?: 0

                when (did) {
                    "0001" -> {
                        if (parts.size > 5) {
                            // Пример расчета по байтам с применением коэффициентов из XML
                            val valRaw = parts[5].toInt(16)
                            return "Параметр ЭБУ [0001] (scale: $scale, offset: $offset): сырое значение = $valRaw"
                        }
                    }
                }
                return "Bosch DID пакет [$did] (Байт всего: ${parts.size - 3})"
            }
        } catch (e: Exception) {}
        return null
    }
}
