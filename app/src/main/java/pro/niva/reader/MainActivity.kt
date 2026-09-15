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
        title.text = "Niva Reader: OpenDiag Log Viewer"
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
        btnOpenLog.text = "📁 Открыть лог OpenDiag (.log / .txt / .csv)"
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
        statusText.text = "Выберите раздел выше или откройте лог-файл."
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

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала или откройте лог OpenDiag для просмотра в виде таблицы с колонками.")
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
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                // Определяем разделитель в файле лога (точка с запятой, запятая или табуляция)
                var delimiter = ";"
                if (line != null) {
                    if (line.contains(",")) delimiter = ","
                    else if (line.contains("\t")) delimiter = "\t"
                    else if (line.contains(";")) delimiter = ";"

                    // Первая строка — шапка таблицы (названия колонок датчиков)
                    val headers = line.split(delimiter)
                    val headerRow = TableRow(this)
                    headerRow.setPadding(0, 8, 0, 8)
                    for (h in headers) {
                        val tv = TextView(this)
                        tv.text = h.trim()
                        tv.textSize = 13f
                        tv.setTypeface(null, android.graphics.Typeface.BOLD)
                        tv.setPadding(6, 6, 6, 6)
                        tv.gravity = Gravity.CENTER
                        headerRow.addView(tv)
                    }
                    tableLayout.addView(headerRow)
                    totalRows++
                }

                // Последующие строки — значения параметров датчиков
                line = reader.readLine()
                while (line != null) {
                    val t = line.trim()
                    if (t.isNotEmpty()) {
                        val cols = t.split(delimiter)
                        val row = TableRow(this)
                        row.setPadding(0, 4, 0, 4)
                        for (c in cols) {
                            val tv = TextView(this)
                            tv.text = c.trim()
                            tv.textSize = 12f
                            tv.setPadding(6, 4, 6, 4)
                            tv.gravity = Gravity.CENTER
                            row.addView(tv)
                        }
                        tableLayout.addView(row)
                        totalRows++
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            }

            statusText.text = "Лог открыт как в DiagView. Строк: $totalRows"
            contentContainer.addView(tableLayout)

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения файла: ${e.localizedMessage}"
        }
    }
}
