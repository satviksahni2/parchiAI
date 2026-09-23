package com.example.parser

import android.util.Log
import com.example.BuildConfig
import com.example.data.JsonHelper
import com.example.data.model.CatalogItem
import com.example.data.model.ParsedOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiOrderParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun parseWithGemini(
        rawInput: String,
        imageBase64: String? = null,
        imageMimeType: String? = "image/jpeg",
        catalog: List<CatalogItem> = CatalogItem.DEFAULT_CATALOG,
        inputFormat: String = "text_message",
        selectedModel: String = "gemini-2.5-flash"
    ): Result<ParsedOrder> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Smoothly use high-precision local deterministic engine
            return@withContext Result.success(OrderParserEngine.parse(rawInput, catalog, inputFormat))
        }

        try {
            val catalogDescription = catalog.joinToString("\n") {
                "${it.itemCode} | ${it.standardName} | ${it.brand} | ${it.unitType} | ₹${it.priceInr} | Aliases: [${it.aliases.joinToString(", ")}]"
            }

            val systemInstruction = """
                Role: B2B Order and Invoice Parsing Assistant for wholesale distributors.
                You are a strict, factual middleware parsing retail shop orders received via WhatsApp text messages, voice transcripts, or photos of handwritten order slips.
                
                RULES OF ENGAGEMENT:
                1. Extraction: Extract all order items, product names, requested brands, and exact quantities.
                2. Cross-Reference Catalog: Compare extracted items against the [MASTER PRODUCT CATALOG] provided below.
                3. Normalization: Map colloquial and phonetic spellings to exact catalog SKUs (e.g. "surf big" or "surf 3kg" -> "HPC-SURF-3KG-24", "fair n lovly" -> "F001", "pcm 500" -> "P001", "10 peti maggi" -> "F002").
                4. Unit Standardization: Convert regional or shorthand units into standard formats:
                   - "bx", "peti", "carton" -> "box"
                   - "pcs", "nag", "nos" -> "pieces"
                   - "kg", "kilo" -> "kilograms"
                   - "strip", "patti" -> "strip"
                   - "btl", "bottle" -> "bottle"
                   - "tube" -> "tube"
                   - "roll", "bundle" -> "roll"
                   - "bag", "bori" -> "bag"
                   - "bucket", "balti" -> "bucket"
                   - "can", "tin" -> "can"
                5. Confidence Flagging: If an item CANNOT be matched with certainty to the catalog, extract the raw requested text, but set "matched_in_catalog" to false, set "item_code" to "", and unit price to 0.0.
                6. Hallucination Prevention: Strictly infer quantities and items from the user input. NEVER assume default quantities or invent items not explicitly requested.
                7. Retailer Details: Extract retailer name, phone, or address if present in text/slip.
                
                [MASTER PRODUCT CATALOG]
                $catalogDescription
            """.trimIndent()

            val promptText = if (imageBase64 != null) {
                """
                Parse this handwritten retail order slip or order invoice image.
                Extract every item, requested quantity, units, shop name, and match against the Master Product Catalog.
                ${if (rawInput.isNotBlank()) "Additional contextual notes: $rawInput" else ""}
                """.trimIndent()
            } else {
                """
                Extract and normalize this retailer WhatsApp order request:
                $rawInput
                """.trimIndent()
            }

            // Build request parts (multimodal if imageBase64 is provided)
            val partsArray = JSONArray().apply {
                if (!imageBase64.isNullOrBlank()) {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", imageMimeType ?: "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }
                put(JSONObject().apply {
                    put("text", promptText)
                })
            }

            val requestJson = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArr)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    // Deterministic temperature per PRD Section 4.1
                    put("temperature", 0.1)
                })
            }

            // Model selection per PRD Section 4.1
            val resolvedModel = when (selectedModel) {
                "gemini-2.5-pro", "gemini-1.5-pro", "pro" -> "gemini-3.1-pro-preview"
                else -> "gemini-2.5-flash"
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("GeminiOrderParser", "Gemini API error ${response.code}: $responseBody. Falling back to local engine.")
                return@withContext Result.success(OrderParserEngine.parse(rawInput, catalog, inputFormat))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text")

            if (!textOutput.isNullOrBlank()) {
                val parsed = JsonHelper.fromJson(textOutput.trim())
                if (parsed != null && parsed.items.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            }

            // Fallback to local deterministic engine
            Result.success(OrderParserEngine.parse(rawInput, catalog, inputFormat))
        } catch (e: Exception) {
            Log.e("GeminiOrderParser", "Error calling Gemini: ${e.message}", e)
            Result.success(OrderParserEngine.parse(rawInput, catalog, inputFormat))
        }
    }
}
