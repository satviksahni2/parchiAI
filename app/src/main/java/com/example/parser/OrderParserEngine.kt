package com.example.parser

import com.example.data.model.CatalogItem
import com.example.data.model.ParsedOrder
import com.example.data.model.ParsedOrderItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object OrderParserEngine {

    fun parse(
        rawText: String,
        catalog: List<CatalogItem> = CatalogItem.DEFAULT_CATALOG,
        inputFormat: String = "text_message"
    ): ParsedOrder {
        val retailerInfo = extractRetailerMetadata(rawText)
        val lineSegments = segmentOrderItems(rawText)

        val parsedItems = mutableListOf<ParsedOrderItem>()
        for (segment in lineSegments) {
            val item = parseLineSegment(segment, catalog)
            if (item != null) {
                parsedItems.add(item)
            }
        }

        // Generate Order ID e.g. ORD-20260922-8412
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        val orderId = "ORD-$dateStamp-$randomSuffix"

        val summary = ParsedOrder.calculateSummary(parsedItems)

        return ParsedOrder(
            orderId = orderId,
            retailerName = retailerInfo.name,
            retailerPhone = retailerInfo.phone,
            retailerAddress = retailerInfo.address,
            orderDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            inputFormat = inputFormat,
            rawInput = rawText.trim(),
            items = parsedItems,
            summary = summary,
            status = "PARSED"
        )
    }

    private data class RetailerMetadata(
        val name: String,
        val phone: String,
        val address: String
    )

    private fun extractRetailerMetadata(text: String): RetailerMetadata {
        var name = "Retail Shop Partner"
        var phone = ""
        var address = ""

        // Extract Phone (Indian mobile numbers: +91-XXXXX or 10-digit starting 6-9)
        val phoneRegex = Regex("(?:\\+91[\\s-]?)?[6-9]\\d{9}")
        val phoneMatch = phoneRegex.find(text)
        if (phoneMatch != null) {
            phone = phoneMatch.value
        }

        // Extract Shop / Retailer Name
        val shopPatterns = listOf(
            Regex("(?:shop|store|retailer|kirana|pharmacy|medical|traders):\\s*([A-Za-z0-9\\s&.'-]+?)(?:,|\\n|\\.|phone|$)", RegexOption.IGNORE_CASE),
            Regex("(?:bill to|send invoice to|party|from):\\s*([A-Za-z0-9\\s&.'-]+?)(?:,|\\n|\\.|phone|$)", RegexOption.IGNORE_CASE),
            Regex("(?:main|hum|mera)\\s+([A-Za-z0-9\\s&.'-]+?(?:store|traders|shop|kirana|medical|enterprises))\\s+(?:se|bol)", RegexOption.IGNORE_CASE),
            Regex("([A-Za-z0-9\\s&.'-]+?(?:store|traders|shop|kirana|medical|enterprises|mart))\\s*bol\\s+raha", RegexOption.IGNORE_CASE)
        )

        for (pattern in shopPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val extracted = match.groupValues[1].trim()
                if (extracted.length in 3..40) {
                    name = extracted
                    break
                }
            }
        }

        // Extract Location/Address if mentioned
        val addrRegex = Regex("(?:lajpat nagar|chandni chowk|karol bagh|sector\\s*\\d+|main market|station road|gandhi nagar|mg road)", RegexOption.IGNORE_CASE)
        val addrMatch = addrRegex.find(text)
        if (addrMatch != null) {
            address = addrMatch.value
        }

        return RetailerMetadata(name = name, phone = phone, address = address)
    }

    private fun segmentOrderItems(text: String): List<String> {
        val cleanedLines = mutableListOf<String>()

        // Split by lines first
        val rawLines = text.lines()
        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // Filter out metadata-only lines
            val lower = trimmed.lowercase()
            if (lower.startsWith("shop:") || lower.startsWith("retailer:") || lower.startsWith("party:") ||
                lower.startsWith("bill to:") || lower.startsWith("phone:") || lower.startsWith("mobile:") ||
                lower.startsWith("urgent order") || lower.startsWith("bhaiya urgent") ||
                lower.startsWith("hello") || lower.startsWith("send invoice") ||
                lower.startsWith("order slip") || lower.startsWith("delivery:")) {
                continue
            }

            // Check if the line contains multiple items separated by comma, bullet, or Hinglish "aur" / "and"
            // Example: "10 peti maggi, 5 strip paracetamol 500, 2 bottle cough syrup"
            if (trimmed.contains(",") || trimmed.contains(" aur ") || trimmed.contains(" and ")) {
                val parts = trimmed.split(Regex(",|\\baur\\b|\\band\\b|\\+"))
                for (part in parts) {
                    val pTrimmed = part.trim()
                    if (pTrimmed.isNotBlank() && containsQuantityOrProduct(pTrimmed)) {
                        cleanedLines.add(pTrimmed)
                    }
                }
            } else {
                cleanedLines.add(trimmed)
            }
        }

        return cleanedLines
    }

    private fun containsQuantityOrProduct(s: String): Boolean {
        return Regex("\\d").containsMatchIn(s) ||
                listOf("maggi", "paracetamol", "cough", "fair", "wire", "bulb", "sugar", "strip", "peti", "pcs", "bottle", "patta", "surf", "cement", "paint", "philip").any { s.contains(it, ignoreCase = true) }
    }

    private fun parseLineSegment(rawSegment: String, catalog: List<CatalogItem>): ParsedOrderItem? {
        val segment = rawSegment.trim()
            .replace(Regex("^[-*•]\\s*"), "") // strip bullet points
            .replace(Regex("^\\d+[:.)-]\\s*"), "") // strip numbered lists like 1. or 2)
            .replace(Regex("^(?:bhaiya|bhai|sir|please|plz|send|bhejo|bhej do|bhej dena|pack|order)\\s*,?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        if (segment.isBlank()) return null

        var quantity = 1.0
        var rawUnit: String? = null
        var productName = segment

        val leadingQtyMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)?\\s*(.*)$").find(segment)
        val trailingQtyMatch = Regex("^(.*?)\\s+[-:x]?\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)?$").find(segment)
        val xQtyMatch = Regex("^(.*?)\\s*[-:x]\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)?$").find(segment)

        if (leadingQtyMatch != null && leadingQtyMatch.groupValues[1].isNotBlank()) {
            val parsedQty = leadingQtyMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            val potentialUnit = leadingQtyMatch.groupValues[2].trim()
            val rem = leadingQtyMatch.groupValues[3].trim()

            if (isKnownUnit(potentialUnit)) {
                quantity = parsedQty
                rawUnit = potentialUnit
                productName = rem
            } else {
                quantity = parsedQty
                productName = if (potentialUnit.isNotEmpty()) "$potentialUnit $rem".trim() else rem
            }
        } else if (trailingQtyMatch != null) {
            val rem = trailingQtyMatch.groupValues[1].trim()
            val parsedQty = trailingQtyMatch.groupValues[2].toDoubleOrNull() ?: 1.0
            val potentialUnit = trailingQtyMatch.groupValues.getOrNull(3)?.trim()

            quantity = parsedQty
            if (isKnownUnit(potentialUnit)) {
                rawUnit = potentialUnit
            }
            productName = rem
        } else if (xQtyMatch != null) {
            val rem = xQtyMatch.groupValues[1].trim()
            val parsedQty = xQtyMatch.groupValues[2].toDoubleOrNull() ?: 1.0
            val potentialUnit = xQtyMatch.groupValues.getOrNull(3)?.trim()

            quantity = parsedQty
            if (isKnownUnit(potentialUnit)) {
                rawUnit = potentialUnit
            }
            productName = rem
        }

        // Clean product name and remove trailing conversational time/urgency markers
        productName = productName
            .replace(Regex("[-:x=]"), " ")
            .replace(Regex("\\b(?:urgent|urgently|today|evening|morning|afternoon|night|tonight|quickly|jaldi|asap|fast|soon|bhej do|bhejo|bhej dena|chahiye|pack kar do)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[.,!?;]+$"), "")
            .trim()

        if (productName.isBlank()) return null

        val standardizedUnit = NormalizationRules.standardizeUnit(rawUnit)
        val extractedBrand = NormalizationRules.extractBrand(segment)

        // Cross-reference with Catalog
        val matchResult = findBestCatalogMatch(productName, extractedBrand, catalog)

        return if (matchResult != null) {
            val matchedCatalog = matchResult.catalogItem
            val unitPrice = matchedCatalog.priceInr
            val totalPrice = Math.round(unitPrice * quantity * 100.0) / 100.0

            val effectiveUnit = if (rawUnit != null) {
                standardizedUnit
            } else {
                NormalizationRules.standardizeUnit(matchedCatalog.unitType)
            }

            val notes = buildString {
                if (rawUnit != null && rawUnit.lowercase() != effectiveUnit.lowercase()) {
                    append("Standardized unit from '$rawUnit' -> '$effectiveUnit'. ")
                } else if (rawUnit == null) {
                    append("Adopted catalog unit '$effectiveUnit'. ")
                }
                if (!productName.equals(matchedCatalog.standardName, ignoreCase = true)) {
                    append("Normalized '${productName.trim()}' to '${matchedCatalog.standardName}'.")
                }
            }.trim()

            ParsedOrderItem(
                itemCode = matchedCatalog.itemCode,
                standardName = matchedCatalog.standardName,
                brand = matchedCatalog.brand,
                requestedRawName = rawSegment.trim(),
                quantity = quantity,
                unitType = effectiveUnit,
                unitPriceInr = unitPrice,
                totalPriceInr = totalPrice,
                matchedInCatalog = true,
                confidence = matchResult.confidence,
                normalizationNote = if (notes.isNotBlank()) notes else "Direct catalog match"
            )
        } else {
            // Unmatched item: flag matched_in_catalog as false and leave item_code blank
            ParsedOrderItem(
                itemCode = "",
                standardName = capitalizeWords(productName),
                brand = extractedBrand,
                requestedRawName = rawSegment.trim(),
                quantity = quantity,
                unitType = standardizedUnit,
                unitPriceInr = 0.0,
                totalPriceInr = 0.0,
                matchedInCatalog = false,
                confidence = 0.0,
                normalizationNote = "Item not found in current catalog. Flagged for distributor review."
            )
        }
    }

    private fun isKnownUnit(unit: String?): Boolean {
        if (unit.isNullOrBlank()) return false
        val u = unit.lowercase()
        return u in listOf(
            "peti", "petee", "bx", "box", "boxes", "pcs", "pc", "nag", "piece",
            "pieces", "kg", "kgs", "kilo", "kilogram", "kilograms", "pkt", "packet",
            "strip", "strips", "patti", "patta", "patte", "btl", "bottle", "bottles", "tube", "tubes",
            "roll", "rolls", "bundle", "carton", "ctn", "nos", "bag", "bags", "bori",
            "bucket", "buckets", "balti", "can", "cans", "tin", "tins"
        )
    }

    private data class MatchResult(
        val catalogItem: CatalogItem,
        val confidence: Double
    )

    private fun findBestCatalogMatch(
        rawName: String,
        extractedBrand: String?,
        catalog: List<CatalogItem>
    ): MatchResult? {
        val cleanName = rawName.lowercase().trim()

        var bestItem: CatalogItem? = null
        var highestScore = 0.0

        for (item in catalog) {
            // 1. Check exact or alias match
            for (alias in item.aliases) {
                if (cleanName.contains(alias.lowercase()) || alias.lowercase().contains(cleanName)) {
                    val score = 0.95
                    if (score > highestScore) {
                        highestScore = score
                        bestItem = item
                    }
                }
            }

            // 2. Check brand + keyword match
            if (extractedBrand != null && extractedBrand.equals(item.brand, ignoreCase = true)) {
                // If brand matches and some keywords match
                val itemWords = item.standardName.lowercase().split(" ")
                val matchedWords = itemWords.count { cleanName.contains(it) }
                if (matchedWords >= 1) {
                    val score = 0.90 + (matchedWords * 0.03)
                    if (score > highestScore) {
                        highestScore = score
                        bestItem = item
                    }
                }
            }

            // 3. Specific catalog fuzzy matchers
            when (item.itemCode) {
                "P001" -> { // Paracetamol 500mg Strip
                    if (cleanName.contains("paracetamol") || cleanName.contains("paracitamol") ||
                        cleanName.contains("pcm") || cleanName.contains("crocin") ||
                        (cleanName.contains("500") && (cleanName.contains("strip") || cleanName.contains("mg")))) {
                        val score = 0.98
                        if (score > highestScore) {
                            highestScore = score
                            bestItem = item
                        }
                    }
                }
                "P002" -> { // Cough Syrup 100ml
                    if (cleanName.contains("cough") || cleanName.contains("siroop") ||
                        cleanName.contains("khansi") || (cleanName.contains("syrup") && cleanName.contains("100"))) {
                        val score = 0.98
                        if (score > highestScore) {
                            highestScore = score
                            bestItem = item
                        }
                    }
                }
                "F001" -> { // Fair & Lovely 50g
                    if (cleanName.contains("fair") && (cleanName.contains("lovly") || cleanName.contains("lovely") || cleanName.contains("cream"))) {
                        val score = 0.99
                        if (score > highestScore) {
                            highestScore = score
                            bestItem = item
                        }
                    }
                }
                "F002" -> { // Maggi 2-Minute Noodles 70g
                    if (cleanName.contains("maggi") || cleanName.contains("maggie") ||
                        cleanName.contains("meggi") || cleanName.contains("noodles") || cleanName.contains("2 min")) {
                        val score = 0.99
                        if (score > highestScore) {
                            highestScore = score
                            bestItem = item
                        }
                    }
                }
                "E001" -> { // Copper Wire 1.5mm
                    if (cleanName.contains("copper") || cleanName.contains("wire") ||
                        cleanName.contains("1.5") || cleanName.contains("havells") || cleanName.contains("havels")) {
                        if (cleanName.contains("wire") || cleanName.contains("copper") || cleanName.contains("1.5")) {
                            val score = 0.97
                            if (score > highestScore) {
                                highestScore = score
                                bestItem = item
                            }
                        }
                    }
                }
                "E002" -> { // LED Bulb 9W
                    if ((cleanName.contains("bulb") || cleanName.contains("led")) &&
                        (cleanName.contains("9w") || cleanName.contains("9 watt") || cleanName.contains("philips") || cleanName.contains("led"))) {
                        val score = 0.97
                        if (score > highestScore) {
                            highestScore = score
                            bestItem = item
                        }
                    }
                }
            }

            // 4. Fuzzy distance fallback
            val similarity = NormalizationRules.similarityScore(cleanName, item.standardName)
            if (similarity > 0.65 && similarity > highestScore) {
                highestScore = similarity
                bestItem = item
            }
        }

        return if (bestItem != null && highestScore >= 0.70) {
            MatchResult(bestItem, Math.round(highestScore * 100.0) / 100.0)
        } else {
            null
        }
    }

    private fun capitalizeWords(str: String): String {
        return str.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}
