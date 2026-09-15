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
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val title = TextView(this).apply {
            text = "Niva Reader: Руководство и Логи"
            textSize = 20f
            setPadding(0, 0, 0, 12)
        }
        mainLayout.addView(title)

        // Меню кнопок
        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 12)
        }

        // Ряд 1: Кнопки разделов мануала
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8)
        }

        val btnEngine = Button(this).apply {
            text = "Двигатель"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showManualText(
                    "Раздел: Двигатель и ЭБУ",
                    "• Диагностика и параметры датчиков\n• Проверка калибровки контроллера\n• Регламент замены расходников"
                )
            }
        }

        val btnSuspension = Button(this).apply {
            text = "Подвеска"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showManualText(
                    "Раздел: Подвеска и колеса",
                    "• Обслуживание ступичных узлов\n• Регулировка давления в шинах (для зимы рекомендуется 1.8 атм)\n• Проверка элементов подвески"
                )
            }
        }

        val btnAudio = Button(this).apply {
            text = "Электрика"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showManualText(
                    "Раздел: Электрика и аудио",
                    "• Прокладка силовых линий и кабеля 1.5 мм²\n• Прямое подключение головного устройства к АКБ\n• Контроль предохранителей"
                )
            }
        }

        row1.addView(btnEngine)
        row1.addView(btnSuspension)
        row1.addView(btnAudio)
        menuLayout.addView(row1)

        // Ряд 2: Кнопка выбора лог-файла с телефона
        val btnOpenLog = Button(this).apply {
            text = "📁 Выбрать лог-файл ЭБУ (.csv / .txt / .log)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(intent, PICK_FILE_REQUEST)
            }
        }
        menuLayout.addView(btnOpenLog)
        mainLayout.addView(menuLayout)

        // Текст статуса / подсказки
        statusText = TextView(this).apply {
            text = "Выберите раздел выше или загрузите лог-файл."
            textSize = 14f
            setPadding(0, 4, 0, 8)
        }
        mainLayout.addView(statusText)

        // Контейнер для динамического контента (текст мануала или таблица логов)
        contentCont


ainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(contentContainer)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)

        // Приветственный экран по умолчанию
        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала в верхнем меню или откройте файл лога с памяти телефона для анализа датчиков.")
    }

    private fun showManualText(heading: String, body: String) {
        contentContainer.removeAllViews()
        statusText.text = heading

        val tvHeading = TextView(this).apply {
            text = heading
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        val tvBody = TextView(this).apply {
            text = body
            textSize = 15f
        }

        contentContainer.addView(tvHeading)
        contentContainer.addView(tvBody)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readAndParseLogFile(uri)
            }
        }
    }

    private fun readAndParseLogFile(uri: Uri) {
        contentContainer.removeAllViews()
        var totalRows = 0

        val tableLayout = TableLayout(this).apply {
            isStretchAllColumns = true
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Шапка таблицы
            addTableRow(tableLayout, "Параметр / Событие", "Значение / Статус", true)

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    when {
                        trimmed.contains("=") -> {
                            val parts = trimmed.split("=", limit = 2)
                            addTableRow(tableLayout, parts[0].trim(), parts[1].trim(), false)
                            totalRows++
                        }
                        trimmed.contains(":") && !trimmed.startsWith("[") -> {
                            val parts = trimmed.split(":", limit = 2)
                            addTableRow(tableLayout, parts[0].trim(), parts[1].trim(), false)
                            totalRows++
                        }
                        trimmed.contains(",") || trimmed.contains(";") || trimmed.contains("\t") -> {
                            val parts = trimmed.split(Regex("[,;\\t]"))
                            if (parts.size >= 2) {
                                addTableRow(tableLayout, parts[0].trim(), parts[1].trim(), false)
                                totalRows++
                            } else {
                                addTableRow(tableLayout, trimmed, "", false)
                                totalRows++
                            }
                        }
                        else -> {
                            addTableRow(tableLayout, trimmed, "", false)
                            totalRows++
                        }
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            inputStream?.close()

            statusText.text = "Лог загружен. Обработано строк: $totalRows"
            contentContainer.addView(tableLayout)

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения файла: ${e.localizedMessage}"
        }
    }

    private fun addTableRow(table: TableLayout, col1: String, col2: String, isHeader: Boolean) {
        val row = TableRow(this).apply {
            setPadding(


0, 6, 0, 6)
        }

        val tv1 = TextView(this).apply {
            text = col1
            textSize = if (isHeader) 15f else 13f
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            setPadding(6, 6, 6, 6)
        }

        val tv2 = TextView(this).apply {
            text = col2
            textSize = if (isHeader) 15f else 13f
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            setPadding(6, 6, 6, 6)
            gravity = Gravity.END
        }

        row.addView(tv1)
        row.addView(tv2)
        table.addView(row)
    }
}
