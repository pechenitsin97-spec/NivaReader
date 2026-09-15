    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // 1. Паспорта и идентификаторы ЭБУ (работает отлично)
                if (did >= "0090" && did <= "00A3") {
                    val sb = StringBuilder()
                    for (i in 3 until parts.size) {
                        val hex = parts[i]
                        if (hex.length == 2 && hex != "AA") {
                            val charCode = hex.toInt(16)
                            if (charCode in 32..126) {
                                sb.append(charCode.toChar())
                            }
                        }
                    }
                    val textResult = sb.toString().trim()
                    if (textResult.isNotEmpty()) {
                        return "📝 Паспорт ЭБУ [$did]: $textResult"
                    }
                }

                // 2. Основной пакет телеметрии 0001 (реальные датчики)
                if (did == "0001" && parts.size > 20) {
                    try {
                        // Подбираем байты для Bosch ME17.9.7
                        // Обороты (обычно 2 байта, делим на 4)
                        val rpmRaw = (parts[5].toInt(16) * 256) + parts[6].toInt(16)
                        val rpm = rpmRaw / 4

                        // Температура охлаждающей жидкости (обычно сырой байт минус 40)
                        val tempRaw = parts[8].toInt(16)
                        val coolant = tempRaw - 40

                        // Положение дроссельной заслонки (%)
                        val tpsRaw = parts[12].toInt(16)
                        val tps = (tpsRaw * 100) / 255

                        // Напряжение бортсети
                        val voltRaw = parts[16].toInt(16)
                        val voltage = voltRaw * 0.1

                        return "🔥 Обороты: $rpm об/мин  |  🌡 Антифриз: $coolant °C  |  ⚡ Дроссель: $tps%  |  🔋 АКБ: ${String.format("%.1f", voltage)}V"
                    } catch (e: Exception) {
                        // Если какой-то байт не парсится, не падаем
                    }
                }

                return null // Мелкие служебные пакеты скрываем, чтобы не загромыхать экран
            }
        } catch (e: Exception) {}
        return null
    }
