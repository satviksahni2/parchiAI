package com.example.parser

object NormalizationRules {

    /**
     * Standardizes units of measurement according to B2B wholesale conventions:
     * - "bx" or "peti" -> "box"
     * - "pcs" or "nag" -> "pieces"
     * - "kg" -> "kilograms"
     * - "strip" -> "strip"
     * - "bottle" -> "bottle"
     * - "packet" -> "packet"
     * - "tube" -> "tube"
     * - "roll" -> "roll"
     * - "bag" or "bori" -> "bag"
     * - "bucket" or "balti" -> "bucket"
     * - "can" or "tin" -> "can"
     */
    fun standardizeUnit(rawUnit: String?): String {
        if (rawUnit.isNullOrBlank()) return "units"
        val u = rawUnit.trim().lowercase()

        return when {
            u in listOf("peti", "petee", "bx", "bxs", "box", "boxes", "carton", "cartons", "ctn", "case", "cases") -> "box"
            u in listOf("nag", "pcs", "pc", "piece", "pieces", "nos", "no", "unit", "units") -> "pieces"
            u in listOf("kg", "kgs", "kilo", "kilos", "kilogram", "kilograms") -> "kilograms"
            u in listOf("pkt", "pkts", "packet", "packets", "pouch", "pouches") -> "packet"
            u in listOf("strip", "strips", "patti", "patta", "patte") -> "strip"
            u in listOf("btl", "btls", "bottle", "bottles", "shishi") -> "bottle"
            u in listOf("tube", "tubes") -> "tube"
            u in listOf("roll", "rolls", "bundle", "bundles", "rll") -> "roll"
            u in listOf("bag", "bags", "bori", "katta", "kattas") -> "bag"
            u in listOf("bucket", "buckets", "balti") -> "bucket"
            u in listOf("can", "cans", "tin", "tins", "canister") -> "can"
            u in listOf("dz", "doz", "dozen", "darjan") -> "dozen"
            u in listOf("gm", "gms", "g", "gram", "grams") -> "grams"
            u in listOf("ltr", "liter", "liters", "litre", "litres", "l") -> "liters"
            u in listOf("m", "meter", "meters", "mtr") -> "meters"
            else -> u
        }
    }

    /**
     * Common wholesale brands lookup across Pharma, FMCG, Electrical, Building Materials, Auto Parts
     */
    val KNOWN_BRANDS = mapOf(
        "pharmacorp" to "PharmaCorp",
        "pharma" to "PharmaCorp",
        "healthplus" to "HealthPlus",
        "health plus" to "HealthPlus",
        "hul" to "HUL",
        "hindustan unilever" to "HUL",
        "nestle" to "Nestle",
        "tata" to "Tata",
        "britannia" to "Britannia",
        "havells" to "Havells",
        "havels" to "Havells",
        "philips" to "Philips",
        "phillips" to "Philips",
        "philip" to "Philips",
        "panasonic" to "Panasonic",
        "anchor" to "Panasonic",
        "syska" to "Syska",
        "ultratech" to "UltraTech",
        "ultra tech" to "UltraTech",
        "asian paints" to "Asian Paints",
        "asian paint" to "Asian Paints",
        "pidilite" to "Pidilite",
        "castrol" to "Castrol",
        "bosch" to "Bosch"
    )

    fun extractBrand(text: String): String? {
        val lower = text.lowercase()
        for ((key, standardBrand) in KNOWN_BRANDS) {
            val regex = Regex("\\b${Regex.escape(key)}\\b")
            if (regex.containsMatchIn(lower)) {
                return standardBrand
            }
        }
        return null
    }

    /**
     * Levenshtein Distance for fuzzy string similarity
     */
    fun similarityScore(s1: String, s2: String): Double {
        val clean1 = s1.lowercase().replace(Regex("[^a-z0-9]"), "")
        val clean2 = s2.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (clean1 == clean2) return 1.0
        if (clean1.isEmpty() || clean2.isEmpty()) return 0.0

        val maxLen = maxOf(clean1.length, clean2.length)
        val distance = levenshteinDistance(clean1, clean2)
        return (1.0 - (distance.toDouble() / maxLen)).coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0) { it }
        var newcost = IntArray(len0) { 0 }

        for (i in 1 until len1) {
            newcost[0] = i
            for (j in 1 until len0) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newcost[j - 1] + 1
                newcost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newcost
            newcost = swap
        }
        return cost[len0 - 1]
    }
}
