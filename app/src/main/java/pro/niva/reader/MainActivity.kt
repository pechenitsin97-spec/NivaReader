    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // 1. Паспортные данные (текст)
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

                // 2. Основной поток датчиков (DID 0001)
                if (did == "0001" && parts.size > 15) {
                    // Индексы байтов для Bosch ME17.9.7
                    // Обороты двигателя (2 байта)
                    val rpmA = parts[5].toInt(16)
                    val rpmB = parts[6].toInt(16)
                    val rpm = ((rpmA * 256) + rpmB) / 4

                    // Температура охлаждающей жидкости
                    val coolantRaw = parts[8].toInt(16)
                    val coolant = coolantRaw - 40

                    // Положение дроссельной заслонки
                    val tpsRaw = parts[12].toInt(16)
                    val tps = (tpsRaw * 100) / 255

                    // Напряжение бортовой сети
                    val voltRaw = parts[18].toInt(16)
                    val voltage = voltRaw / 10.0

                    return "🔥 Обороты: $rpm об/мин | 🌡 Антифриз: $coolant°C | ⚡ Дроссель: $tps% | 🔋 АКБ: ${voltage}В"
                }

                return "Bosch DID пакет [$did] (Байт всего: ${parts.size - 3})"
            }
        } catch (e: Exception) {}
        return null
    }
