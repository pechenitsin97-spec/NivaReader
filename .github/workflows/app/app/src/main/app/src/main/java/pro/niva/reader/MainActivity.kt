package pro.niva.reader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : Activity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50) }
        val title = TextView(this).apply { text = "Niva Log Reader"; textSize = 24f; setPadding(0, 0, 0, 50) }
        statusText = TextView(this).apply { text = "Нажми кнопку и выбери сырой файл appLog"; textSize = 16f; setPadding(0, 0, 0, 50) }
        val btn = Button(this).apply {
            text = "ВЫБРАТЬ ЛОГ"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                startActivityForResult(intent, 1)
            }
        }
        layout.addView(title); layout.addView(statusText); layout.addView(btn)
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) data?.data?.let { processLog(it) }
    }

    private fun processLog(uri: Uri) {
        statusText.text = "Обработка..."
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csv = StringBuilder()
            csv.append("Напряжение (В);Темп. ОЖ (C);Обороты (об/мин);УОЗ (ПКВ);Впрыск (мс);Дроссель (%);Воздух (кг/ч)\n")
            
            var count = 0
            reader.forEachLine { line ->
                if (line.startsWith("Receive: 62 00 01")) {
                    val b = line.replace("Receive: 62 00 01 ", "").trim().split(" ")
                    if (b.size >= 18) {
                        try {
                            val volts = b[1].toInt(16) / 10.0
                            val temp = b[3].toInt(16) * 0.75 - 48
                            val rpm = (b[4].toInt(16) * 256 + b[5].toInt(16)) / 4.0
                            val ign = (b[6].toInt(16) * 256 + b[7].toInt(16)) * 0.75
                            val inj = (b[8].toInt(16) * 256 + b[9].toInt(16)) * 0.00634
                            val thr = (b[10].toInt(16) * 256 + b[11].toInt(16)) * (100.0 / 4096.0)
                            val maf = (b[16].toInt(16) * 256 + b[17].toInt(16)) / 10.0
                            
                            val row = String.format("%.1f;%.1f;%.1f;%.1f;%.3f;%.1f;%.1f\n", volts, temp, rpm, ign, inj, thr, maf)
                            csv.append(row.replace(".", ","))
                            count++
                        } catch (e: Exception) {}
                    }
                }
            }
            reader.close()
            
            val fileName = "Niva_Log_${System.currentTimeMillis()}.csv"
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outFile = File(downloads, fileName)
            FileOutputStream(outFile).use { it.write(csv.toString().toByteArray()) }
            
            statusText.text = "Успех! Расшифровано строк: $count\n\nИщи таблицу $fileName в папке Загрузки (Download) на телефоне!"
        } catch (e: Exception) {
            statusText.text = "Ошибка: ${e.message}"
        }
    }
}