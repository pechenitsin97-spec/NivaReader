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
        title.text = "Niva Reader: Диагностика"
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
        btnOpenLog.text = "📁 Выбрать лог-файл (.log / .txt)"
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

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала или загрузите лог-файл для вывода в виде таблицы с четкими колонками.")
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

            // Создаем шапку таблицы с 3 колонками
            addTableHeader(tableLayout, "Параметр", "Значение", "Статус / Ед.")

            var i = 0
            while (i < lines.size) {
                val cur = lines[i]
                
                if (!cur.equals("Time", ignoreCase = true) && 
                    !cur.equals("State", ignoreCase = true) && 
                    !cur.equals("Connect", ignoreCase = true) &&
                    !cur.all { it.isDigit() || it == '.' || it == ':' }) {
                    
                    var paramName = cur
                    var paramVal = ""
                    var paramUnit = ""

                    if (cur.equals("Send", ignoreCase = true) || cur.equals("Receive", ignoreCase = true)) {
                        if (i + 1 < lines.size) {
                            paramVal = lines[i + 1]
                            i++
                        }
                        paramName = if (cur.equals("Send", ignoreCase = true)) "📤 Запрос ЭБУ" else "📥 Ответ ЭБУ"
                        paramUnit = "байт"
                    } else if (cur.contains("=")) {
                        val parts = cur.split("=", limit = 2)
                        paramName = parts[0].trim()
                        paramVal = parts[1].trim()
                    } else if (cur.contains(":")) {
                        val parts = cur.split(":", limit = 2)
                        paramName = parts[0].trim()
                        paramVal = parts[1].trim()
                    } else {
                        paramVal = "OK"
                    }

                    addTableRow(tableLayout, paramName, paramVal, paramUnit)
                    totalRows++
                }
                i++
            }

            statusText.text = "Загружено строк: $totalRows"
            contentContainer.addView(tableLayout)

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения: ${e.localizedMessage}"
        }
    }

    private fun addTableHeader(table: TableLayout, col1: String, col2: String, col3: String) {
        val row = TableRow(this)
        row.setPadding(0, 8, 0, 8)

        val tv1 = createCell(col1, true, Gravity.START)
        val tv2 = createCell(col2, true, Gravity.CENTER)
        val tv3 = createCell(col3, true, Gravity.END)

        row.addView(tv1)
        row.addView(tv2)
        row.addView(tv3)
        table.addView(row)
    }

    private fun addTableRow(table: TableLayout, col1: String, col2: String, col3: String) {
        val row = TableRow(this)
        row.setPadding(0, 6, 0, 6)

        val tv1 = createCell(col1, false, Gravity.START)
        val tv2 = createCell(col2, false, Gravity.CENTER)
        val tv3 = createCell(col3, false, Gravity.END)

        row.addView(tv1)
        row.addView(tv2)
        row.addView(tv3)
        table.addView(row)
    }

    private fun createCell(text: String, isHeader: Boolean, gravity: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = if (isHeader) 14f else 12f
        tv.setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tv.setPadding(4, 4, 4, 4)
        tv.gravity = gravity
        return tv
    }
}
