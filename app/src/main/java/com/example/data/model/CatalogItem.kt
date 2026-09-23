package com.example.data.model

data class CatalogItem(
    val itemCode: String,
    val standardName: String,
    val brand: String,
    val unitType: String,
    val priceInr: Double,
    val category: String = "General",
    val aliases: List<String> = emptyList()
) {
    companion object {
        val DEFAULT_CATALOG = listOf(
            // Pharmaceuticals
            CatalogItem(
                itemCode = "P001",
                standardName = "Paracetamol 500mg Strip",
                brand = "PharmaCorp",
                unitType = "Strip",
                priceInr = 25.00,
                category = "Pharmaceuticals",
                aliases = listOf(
                    "paracetamol", "paracitamol", "paracitamole", "pcm", "pcm 500",
                    "paracetamol 500", "paracetamol 500mg", "pcm 500mg", "crocin 500",
                    "calpol 500", "fever tablet", "paracetamol strip", "pcm strip"
                )
            ),
            CatalogItem(
                itemCode = "P002",
                standardName = "Cough Syrup 100ml",
                brand = "HealthPlus",
                unitType = "Bottle",
                priceInr = 85.00,
                category = "Pharmaceuticals",
                aliases = listOf(
                    "cough syrup", "cough siroop", "khansi syrup", "cough syrup 100ml",
                    "health plus cough syrup", "healthplus cough", "healthplus syrup",
                    "cough bottle", "cough medicine"
                )
            ),
            CatalogItem(
                itemCode = "P003",
                standardName = "Amoxicillin 500mg Capsules",
                brand = "PharmaCorp",
                unitType = "Strip",
                priceInr = 65.00,
                category = "Pharmaceuticals",
                aliases = listOf(
                    "amoxicillin", "amox 500", "amox", "amoxicillin 500", "amoxicillin 500mg",
                    "mox 500", "amoxycillin", "antibiotic strip"
                )
            ),
            CatalogItem(
                itemCode = "P004",
                standardName = "Cetirizine 10mg Tablets",
                brand = "HealthPlus",
                unitType = "Strip",
                priceInr = 18.00,
                category = "Pharmaceuticals",
                aliases = listOf(
                    "cetirizine", "cetrizine", "cetzine", "okacet", "cetirizine 10mg",
                    "allergy tablet", "cetirizine strip"
                )
            ),

            // FMCG & Personal Care
            CatalogItem(
                itemCode = "HPC-SURF-3KG-24",
                standardName = "Surf Excel Easy Wash 3kg",
                brand = "HUL",
                unitType = "Bag",
                priceInr = 420.00,
                category = "FMCG / Detergents",
                aliases = listOf(
                    "surf big", "surf excel 3kg", "surf 3kg", "surf excel big",
                    "surf packet 3kg", "surf powder 3kg", "surf excel powder 3kg",
                    "surf excel", "surf detergent big", "surf detergent 3kg"
                )
            ),
            CatalogItem(
                itemCode = "F001",
                standardName = "Fair & Lovely 50g",
                brand = "HUL",
                unitType = "Tube",
                priceInr = 110.00,
                category = "Personal Care",
                aliases = listOf(
                    "fair n lovly", "fair & lovely", "fair and lovely", "fair lovely",
                    "fair n lovely", "glow lovely", "glow & lovely", "fair lovely 50g",
                    "hul fair lovely", "fair n lovly 50g", "fair n lovely cream", "glow and lovely"
                )
            ),
            CatalogItem(
                itemCode = "F002",
                standardName = "Maggi 2-Minute Noodles 70g",
                brand = "Nestle",
                unitType = "Packet",
                priceInr = 14.00,
                category = "FMCG / Food",
                aliases = listOf(
                    "maggi", "maggie", "meggi", "maggi noodles", "nestle maggi",
                    "maggi 2 min", "maggi 2-minute noodles", "2 min noodles",
                    "maggi 70g", "maggie noodles", "maggi packet", "maggi peti"
                )
            ),
            CatalogItem(
                itemCode = "F003",
                standardName = "Tata Salt 1kg",
                brand = "Tata",
                unitType = "Packet",
                priceInr = 28.00,
                category = "FMCG / Food",
                aliases = listOf(
                    "tata salt", "salt 1kg", "tata namak", "namak", "tata salt 1kg",
                    "iodized salt", "tata iodized salt"
                )
            ),
            CatalogItem(
                itemCode = "F004",
                standardName = "Britannia Good Day 100g",
                brand = "Britannia",
                unitType = "Packet",
                priceInr = 30.00,
                category = "FMCG / Food",
                aliases = listOf(
                    "good day", "good day butter", "britannia good day", "good day biscuits",
                    "britannia biscuits", "goodday", "goodday 100g"
                )
            ),

            // Electricals & Hardware
            CatalogItem(
                itemCode = "E001",
                standardName = "Copper Wire 1.5mm",
                brand = "Havells",
                unitType = "Roll",
                priceInr = 950.00,
                category = "Electricals",
                aliases = listOf(
                    "copper wire", "havells wire", "havels wire", "havels wire 1.5",
                    "copper wire 1.5mm", "wire 1.5mm", "havells copper wire",
                    "1.5mm copper wire", "havells roll", "havel wire", "wire roll"
                )
            ),
            CatalogItem(
                itemCode = "E002",
                standardName = "LED Bulb 9W",
                brand = "Philips",
                unitType = "Piece",
                priceInr = 120.00,
                category = "Electricals",
                aliases = listOf(
                    "led bulb", "bulb 9w", "philips bulb", "philips 9w", "philips led",
                    "led bulb 9 watt", "bulb 9 watt", "philips led bulb", "9w bulb",
                    "philips 9w led", "philips bulb 9w"
                )
            ),
            CatalogItem(
                itemCode = "E003",
                standardName = "Anchor Penta Modular Switch 6A",
                brand = "Panasonic",
                unitType = "Piece",
                priceInr = 35.00,
                category = "Electricals",
                aliases = listOf(
                    "anchor switch", "anchor switch 6a", "penta switch", "penta switch 6a",
                    "switch 6a", "anchor modular switch", "anchor 6a", "6a switch"
                )
            ),
            CatalogItem(
                itemCode = "E004",
                standardName = "Syska LED Tube Light 20W",
                brand = "Syska",
                unitType = "Piece",
                priceInr = 240.00,
                category = "Electricals",
                aliases = listOf(
                    "syska tube light", "tube light 20w", "syska 20w", "led tube light",
                    "syska tube", "tubelight 20w", "syska batten"
                )
            ),

            // Building Materials
            CatalogItem(
                itemCode = "BM001",
                standardName = "UltraTech Cement 50kg",
                brand = "UltraTech",
                unitType = "Bag",
                priceInr = 380.00,
                category = "Building Materials",
                aliases = listOf(
                    "ultratech cement", "ultratech", "cement 50kg", "cement bag",
                    "ultra tech cement", "ultra tech 50kg", "cement bori"
                )
            ),
            CatalogItem(
                itemCode = "BM002",
                standardName = "Asian Paints Apex White 20L",
                brand = "Asian Paints",
                unitType = "Bucket",
                priceInr = 3200.00,
                category = "Building Materials",
                aliases = listOf(
                    "asian paints", "asian paint 20l", "apex white 20l", "white paint bucket",
                    "asian paints white 20l", "distemper 20l", "paint bucket 20l"
                )
            ),
            CatalogItem(
                itemCode = "BM003",
                standardName = "Dr. Fixit Waterproofing 1L",
                brand = "Pidilite",
                unitType = "Can",
                priceInr = 210.00,
                category = "Building Materials",
                aliases = listOf(
                    "dr fixit", "dr. fixit", "doctor fixit", "dr fixit 1l", "waterproofing liquid",
                    "dr fixit waterproofing", "pidilite dr fixit"
                )
            ),

            // Auto Parts & Lubricants
            CatalogItem(
                itemCode = "AP001",
                standardName = "Castrol Activ 4T 1L Engine Oil",
                brand = "Castrol",
                unitType = "Bottle",
                priceInr = 390.00,
                category = "Auto Parts",
                aliases = listOf(
                    "castrol oil", "castrol activ 4t", "castrol 4t", "engine oil 1l",
                    "castrol engine oil", "castrol activ 1l", "4t oil"
                )
            ),
            CatalogItem(
                itemCode = "AP002",
                standardName = "Bosch Spark Plug M14",
                brand = "Bosch",
                unitType = "Piece",
                priceInr = 115.00,
                category = "Auto Parts",
                aliases = listOf(
                    "bosch spark plug", "spark plug", "bosch plug", "plug m14",
                    "spark plug m14", "bosch spark plug m14"
                )
            )
        )
    }
}
