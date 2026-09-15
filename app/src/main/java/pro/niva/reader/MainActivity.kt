    private fun tryDecodeBoschPacket(line: String): String? {
        if (!line.startsWith("Receive: 62")) return null
        try {
            val clean = line.replace("Receive:", "").trim()
            val parts = clean.split(" ")
            if (parts.size >= 3) {
                val did = "${parts[1]}${parts[2]}"
                
                // 1. Паспорта ЭБУ (VIN, софт и т.д.)
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

                // 2. Пакет телеметрии 0001 с использованием XML-карт B17s* / B17o*
                if (did == "0001" && parts.size > 10) {
                    val scales = ecuParamsMap["B17s01"] ?: listOf()
                    val offsets = ecuParamsMap["B17o01"] ?: listOf()

                    val sb = StringBuilder("📊 Телеметрия [0001]:\n")
                    
                    // Пробегаем по байтам полезной нагрузки и применяем коэффициенты из XML
                    // Индексы данных начинаются с 3-го элемента (последовательность байт)
                    var paramIndex = 1
                    for (i in 3 until parts.size) {
                        val rawHex = parts[i]
                        val rawValue = rawHex.toIntOrNull(16) ?: continue
                        
                        // Берем масштаб и смещение из таблиц XML (если они там есть для этого индекса)
                        val scale = scales.getOrNull(i - 3) ?: 1
                        val offset = offsets.getOrNull(i - 3) ?: 0
                        
                        // Применяем заводскую формулу пересчета
                        val calculated = (rawValue * scale) + offset
                        
                        sb.append("• П[$paramIndex] (hex:$rawHex) = $calculated (s:$scale, o:$offset)\n")
                        paramIndex++
                        
                        // Ограничим вывод первыми 10-12 параметрами для наглядности, чтобы экран не забивать
                        if (paramIndex > 10) {
                            sb.append("... и еще ${parts.size - i - 1} байт")
                            break
                        }
                    }
                    return sb.toString().trim()
                }
            }
        } catch (e: Exception) {}
        return null
    }

