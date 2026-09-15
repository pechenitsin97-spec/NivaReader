package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
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
        mainLayout.setPadding(16, 16, 16, 16)

        val title = TextView(this)
        title.text = "Niva Reader: OpenDiag & Firmware"
        title.textSize = 18f
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 8)
        mainLayout.addView(title)

        val menuLayout = LinearLayout(this)
        menuLayout.orientation = LinearLayout.VERTICAL
        menuLayout.setPadding(0, 0, 0, 8)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.setPadding(0, 0, 0, 6)

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

        // Кнопка проверки прошивки с точным именем файла
        val btnFirmware = Button(this)
        btnFirmware.text = "🔍 Проверить прошивку (B515HJ04)"
        btnFirmware.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        btnFirmware.setOnClickListener {
            readFirmwareFile()
        }
        menuLayout.addView(btnFirmware)

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
        statusText.text = "Выберите раздел выше или проверьте прошивку."
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

        showManualText("Добро пожаловать!", "Выберите нужный раздел мануала, откройте лог OpenDiag или нажмите кнопку проверки прошивки.")
    }

    private fun showManualText(heading: String, body: String) {
        contentContainer.removeAllViews()
        statusText.text = heading

        val tvHeading = TextView(this)
        tvHeading.text = heading
        tvHeading.textSize = 16f
        tvHeading.setTextColor(Color.BLACK)
        tvHeading.setTypeface(null, android.graphics.Typeface.BOLD)
        tvHeading.setPadding(0, 0, 0, 8)

        val tvBody = TextView(this)
        tvBody.text = body
        tvBody.textSize = 14f
        tvBody.setTextColor(Color.DKGRAY)

        contentContainer.addView(tvHeading)
        contentContainer.addView(tvBody)
    }

    private fun readFirmwareFile() {
        contentContainer.removeAllViews()
        try {
            // Читаем файл с точным именем, которое вы загрузили в assets
            val inputStream = assets.open("10SW004677_B515HJ04.bin")
            val size = inputStream.available()
            
            val buffer = ByteArray(minOf(size, 32))
            inputStream.read(buffer)
            inputStream.close()

            val hexHeader = buffer.joinToString(" ") { String.format("%02X", it) }

            statusText.text = "Прошивка найдена! Размер: $size байт"
            showManualText(
                "Анализ прошивки Bosch ME17.9.7 (B515HJ04)",
                "Файл успешно прочитан из памяти приложения (assets).\n\n• Имя файла: 10SW004677_B515HJ04.bin\n• Общий размер: $size байт\n• Первые 32 байта (HEX): $hexHeader"
            )

        } catch (e: Exception) {
            statusText.text = "Файл прошивки не найден"
            showManualText(
                "Ошибка чтения прошивки",
                "Не удалось обнаружить файл '10SW004677_B515HJ04.bin' в папке assets.\n\nУбедитесь, что загруженный файл в репозитории называется ровно так же."
            )
        }
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
        tableLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line = reader.readLine()
                
                var delimiter = ";"
                if (line != null) {
                    if (line.contains(",")) delimiter = ","
                    else if (line.contains("\t")) delimiter = "\t"
                    else if (line.contains(";")) delimiter = ";"

                    val headers = line.split(delimiter)
                    val headerRow = TableRow(this)
                    headerRow.setPadding(0, 4, 0, 4)
                    for (h in headers) {
                        val tv = TextView(this)
                        tv.text = " ${h.trim()} "
                        tv.textSize = 12f
                        tv.setTextColor(Color.BLACK)
                        tv.setTypeface(null, android.graphics.Typeface.BOLD)
                        tv.setPadding(8, 6, 8, 6)
                        tv.gravity = Gravity.CENTER
                        headerRow.addView(tv)
                    }
                    tableLayout.addView(headerRow)
                    totalRows++
                }

                line = reader.readLine()
                while (line != null) {
                    val t = line.trim()
                    if (t.isNotEmpty()) {
                        val cols = t.split(delimiter)
                        val row = TableRow(this)
                        row.setPadding(0, 2, 0, 2)
                        for (c in cols) {
                            val tv = TextView(this)
                            tv.text = " ${c.trim()} "
                            tv.textSize = 11f
                            tv.setTextColor(Color.DKGRAY)
                            tv.setPadding(8, 4, 8, 4)
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

            statusText.text = "Лог загружен. Строк: $totalRows (двигайте таблицу в стороны)"

            val horizontalScroll = HorizontalScrollView(this)
            horizontalScroll.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            horizontalScroll.addView(tableLayout)

            contentContainer.addView(horizontalScroll)

        } catch (e: Exception) {
            statusText.text = "Ошибка чтения: ${e.localizedMessage}"
        }
    }
}
