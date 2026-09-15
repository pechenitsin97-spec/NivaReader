    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // 1. Если это паспортные данные (VIN, версия ПО и т.д. в диапазоне 0090 - 00A3)
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

                // 2. Если это основной поток датчиков (DID 0001)
                if (did == "0001" && parts.size > 10) {
                    // Возьмем для примера пару байт из потока и применим коэффициенты из XML
                    val scale = ecuParamsMap["B17s01"]?.getOrNull(0) ?: 1
                    
                    // Допустим, обороты или температура зашиты в определенных байтах пакета
                    val rawByte1 = parts[5].toInt(16)
                    val rawByte2 = parts[6].toInt(16)
                    val combined = (rawByte1 * 256) + rawByte2

                    return "🚗 Телеметрия [0001] (scale: $scale): сырые байты = $combined"
                }

                return "Bosch DID пакет [$did] (Байт всего: ${parts.size - 3})"
            }
        } catch (e: Exception) {}
        return null
    }
