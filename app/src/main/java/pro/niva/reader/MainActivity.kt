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

    // Хранилище для таблиц коэффициентов из XML[span_2](start_span)[span_2](end_span)
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

    // Парсер XML-файла с коэффициентами[span_3](start_span)[span_3](end_span)
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

                        // Применяем декодер с учетом загруженных таблиц
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
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }
            statusText.text = "Лог разобран. Записей: $totalEvents (Коэффициенты ЭБУ активны)"
        } catch (e: Exception) {
            statusText.statusTextSafe("Ошибка чтения лога: ${e.localizedMessage}")
        }
    }

    private fun TextView.statusTextSafe(msg: String) {
        text = msg
    }

    private fun tryDecodeOBDPid(line: String): String? {
        if (!line.startsWith("Receive:")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3 && parts[0] == "41") {
                val pid = parts[1]
                when (pid) {
                    "05" -> {
                        val a = parts[2].toInt(16)
                        // Пример использования динамического смещения из XML (например, из карты B17o01)[span_4](start_span)[span_4](end_span)
                        val offset = ecuParamsMap["B17o01"]?.getOrNull(0) ?: 40
                        return "Температура антифриза: ${a - offset} °C"
                    }
                    "0C" -> {
                        if (parts.size >= 4) {
                            val a = parts[2].toInt(16)
                            val b = parts[3].toInt(16)
                            // Пример использования коэффициента из массива B17s01[span_5](start_span)[span_5](end_span)
                            val scale = ecuParamsMap["B17s01"]?.getOrNull(0) ?: 4
                            return "Обороты двигателя: ${((a * 256) + b) / scale} об/мин"
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }
}
