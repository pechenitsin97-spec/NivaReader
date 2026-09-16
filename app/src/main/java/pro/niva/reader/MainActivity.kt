    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // 1. Паспорта ЭБУ (работают отлично)
                if (did >= "0090" && did <= "00A3") {
                    val sb = StringBuilder()
                    for (i in 3 until parts.size) {
                        val hex = parts[i]
                        if (hex.length == 2 && hex != "AA") {
                            val charCode = hex.toIntOrNull(16) ?: continue
                            if (charCode in 32..126) {
                                sb.append(charCode.toChar())
                            }
                        }
                    }
                    val textResult = sb.toString().trim()
                    if (textResult.isNotEmpty()) {
                        return "📝 Паспорт [$did]: $textResult"
                    }
                }

                // 2. Расшифрованная телеметрия 0001 для Bosch ME17.9.7 (Нива)
                if (did == "0001" && parts.size > 14) {
                    try {
                        // Температура (П[2] = parts[4])
                        val tempRaw = parts[4].toIntOrNull(16) ?: 40
                        val coolant = tempRaw - 40

                        // Обороты (П[5], П[6] = parts[7], parts[8])
                        val rpmH = parts[7].toIntOrNull(16) ?: 0
                        val rpmL = parts[8].toIntOrNull(16) ?: 0
                        val rpm = ((rpmH * 256) + rpmL) / 4

                        // Скорость (П[7] = parts[9])
                        val speed = parts[9].toIntOrNull(16) ?: 0

                        // Дроссель (П[8] = parts[10])
                        val tpsRaw = parts[10].toIntOrNull(16) ?: 0
                        val tps = (tpsRaw * 100) / 255

                        // Массовый расход воздуха (ДМРВ) (П[11], П[12] = parts[13], parts[14])
                        val mafH = parts[13].toIntOrNull(16) ?: 0
                        val mafL = parts[14].toIntOrNull(16) ?: 0
                        val maf = ((mafH * 256) + mafL) / 10.0

                        return "🔥 Обороты: $rpm об/мин | 🌡 Антифриз: $coolant °C\n🚗 Скорость: $speed км/ч | ⚡ Дроссель: $tps%\n💨 Воздух (ДМРВ): $maf кг/ч"
                    } catch (e: Exception) {
                        return null
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

