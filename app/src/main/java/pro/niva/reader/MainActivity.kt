package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
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
        mainLayout.setPadding(20, 20, 20, 20)

        val title = TextView(this)
        title.text = "Niva Reader: Руководство и Логи"
        title.textSize = 20f
        title.setPadding(0, 0, 0, 12)
        mainLayout.addView(title)

        val menuLayout = LinearLayout(this)
        menuLayout.orientation = LinearLayout.VERTICAL
        menuLayout.setPadding(0, 0, 0, 12)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.setPadding(0, 0, 0, 8)

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
        btnOpenLog.text = "📁 Выбрать лог-файл (.csv / .txt / .log)"
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
        statusText.text = "Выберите раздел выше или загрузите лог-файл."
        statusText.textSize = 14f
        statusText.setPadding(0, 4, 0, 8)
        mainLayout.addView(statusText)

        contentContainer = LinearLayout(this)
        contentContainer.orientation = LinearLayout.VERTICAL
        contentContainer.setPadding(0, 8, 0, 8)

        val scrollView = ScrollView(this)
        scrollView.addView(contentContainer)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала или загрузите лог-файл с памяти телефона для анализа датчиков.")
    }

    private fun showManualText(heading: String, body: String) {
        contentContainer.removeAllViews()
        statusText.text = heading

        val tvHeading = TextView(this)
        tvHeading.text = heading
        tvHeading.textSize = 18f
        tvHeading.setTypeface(null, android.graphics.Typeface.BOLD)
        tvHeading.setPadding(0, 0, 0, 12)

        val tvBody = TextView(this)
        tvBody.text = body
        tvBody.textSize = 15f

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
        var totalRows = 0

        val tableLayout = TableLayout(this)
        tableLayout.isStretchAllColumns = true

        try {
            val lines = mutableListOf<String>()
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                while (line != null) {
                    val t = line.trim()
                    if (t.isNotEmpty()) {
                        lines.add(t)
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }

            addTableRow(tableLayout, "Пакет / Направление", "Данные / Байты ответа", true)

            var i = 0
            while (i < lines.size) {
                val cur = lines[i]
                if (cur.equals("Send", ignoreCase = true) || cur.equals("Receive", ignoreCase = true)) {
                    var payload = ""
                    if (i + 1 < lines.size) {
                        val next = lines[i + 1]
                        if (!next.equals("Send", ignoreCase = true) && 
                            !next.equals("Receive", ignoreCase = true) && 
                            !next.equals("Time", ignoreCase = true)) {
                            payload = next
                            i++
                        }
                    }
                    addTableRow(tableLayout, "[$cur]", payload, false)
                    totalRows++
                } else if (cur.equals("Time", ignoreCase = true)) {
                    // Пропускаем метки времени, чтобы лог был чистым и компактным
                    if (i + 1 < lines.size) {
                        i++
                    }
                } else {
                    when {
                        cur.contains("=") -> {
                            val parts = cur.split("=", limit = 2)
                            addTableRow(tableLayout, parts[0].trim(), parts[1].trim(), false)
                            totalRows++
                        }
                        cur.contains(":") && !cur.startsWith("[") -> {
                            val parts = cur.split(":", limit = 2)
                            addTableRow(tableLayout, parts[0].trim(), parts[1].trim(), false)
                            totalRows++
                        }
                        else -> {
                            addTableRow(tableLayout, cur, "", false)
                            totalRows++
                        }
                    }
                }
                i++
            }

            statusText.text = "Лог расшифрован. Строк: $totalRows"
            contentContainer.addView(tableLayout)

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения файла: ${e.localizedMessage}"
        }
    }

    private fun addTableRow(table: TableLayout, col1: String, col2: String, isHeader: Boolean) {
        val row = TableRow(this)
        row.setPadding(0, 6, 0, 6)

        val tv1 = TextView(this)
        tv1.text = col1
        tv1.textSize = if (isHeader) 15f else 13f
        tv1.setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tv1.setPadding(6, 6, 6, 6)

        val tv2 = TextView(this)
        tv2.text = col2
        tv2.textSize = if (isHeader) 15f else 13f
        tv2.setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tv2.setPadding(6, 6, 6, 6)
        tv2.gravity = Gravity.END

        row.addView(tv1)
        row.addView(tv2)
        table.addView(row)
    }
}
