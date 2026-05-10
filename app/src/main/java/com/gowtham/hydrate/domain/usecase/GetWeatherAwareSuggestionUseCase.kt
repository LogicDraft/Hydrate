package com.gowtham.hydrate.domain.usecase

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetWeatherAwareSuggestionUseCase @Inject constructor() {

    suspend operator fun invoke(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=12.9719&longitude=77.5937&daily=temperature_2m_max,temperature_2m_min,uv_index_max&hourly=temperature_2m,relative_humidity_2m,weather_code&timezone=auto"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            connection.inputStream.bufferedReader().use { reader ->
                val payload = reader.readText()
                val maxTemp = "\"temperature_2m_max\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)"
                    .toRegex()
                    .find(payload)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toDoubleOrNull()
                    ?: return@use null

                val uvIndexMax = "\"uv_index_max\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)"
                    .toRegex()
                    .find(payload)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toDoubleOrNull() ?: 0.0

                val rounded = maxTemp.roundToInt()
                val extraMlFromHeat = when {
                    rounded >= 38 -> 500
                    rounded >= 34 -> 400
                    rounded >= 30 -> 300
                    rounded >= 27 -> 200
                    else -> 0
                }

                val extraMlFromUv = when {
                    uvIndexMax >= 10 -> 250
                    uvIndexMax >= 8 -> 200
                    uvIndexMax >= 6 -> 100
                    else -> 0
                }

                val extraMl = extraMlFromHeat + extraMlFromUv
                val uvRounded = uvIndexMax.roundToInt()

                if (extraMl > 0) {
                    "Bengaluru today: max ${rounded}°C, UV ${uvRounded}. Consider +${extraMl}ml extra."
                } else {
                    "Bengaluru today: max ${rounded}°C, UV ${uvRounded}. Hydration target looks normal."
                }
            }
        }.getOrNull()
    }
}