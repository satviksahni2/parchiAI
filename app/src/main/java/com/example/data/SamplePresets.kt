package com.example.data

data class SampleOrderPreset(
    val title: String,
    val subtitle: String,
    val category: String,
    val inputFormat: String,
    val text: String
)

object SamplePresets {
    val ALL_PRESETS = listOf(
        SampleOrderPreset(
            title = "Conversational WhatsApp",
            subtitle = "Hinglish single-line with 'patta', 'peti' & time urgency",
            category = "Hinglish / Chat",
            inputFormat = "text_message",
            text = "Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."
        ),
        SampleOrderPreset(
            title = "Kirana Store WhatsApp Order",
            subtitle = "FMCG order with 'surf big' -> HPC-SURF-3KG-24 & Hinglish units",
            category = "FMCG / WhatsApp",
            inputFormat = "text_message",
            text = """
                Bhaiya urgent order bhej do:
                10 peti maggi,
                4 bag surf big,
                5 strip paracetamol 500,
                2 bottle cough syrup healthplus,
                12 pcs philips led bulb,
                4 tube fair n lovly 50g,
                1 roll copper wire 1.5mm havells,
                aur 3 kg sugar.
                
                Send invoice to: Sharma Kirana Store, Lajpat Nagar
                Phone: 9876543210
            """.trimIndent()
        ),
        SampleOrderPreset(
            title = "Electrical Shop Voice Note",
            subtitle = "Transcribed voice note with 'nag', 'rolls' and modular switches",
            category = "Electricals",
            inputFormat = "voice_transcript",
            text = """
                Hello bhaiya, Gupta Electricals bol raha hoon main market se.
                Kal subah tak 2 roll copper wire 1.5mm havells bhej dena,
                aur 25 nag led bulb 9w philips urgently dispatch karwa do.
                50 pcs anchor switch 6a aur 10 pcs syska tube light bhi pack kar dena.
                Ek peti maggi noodles bhi staff ke liye add kar dena.
                Bill to: Gupta Electricals & Hardware, Station Road
                Phone: 9811223344
            """.trimIndent()
        ),
        SampleOrderPreset(
            title = "Building Materials Order",
            subtitle = "Cement, Asian Paints bucket & Dr. Fixit waterproofing",
            category = "Building Materials",
            inputFormat = "text_message",
            text = """
                URGENT SITE DISPATCH:
                - 100 bag ultratech cement 50kg
                - 5 bucket asian paints white 20l
                - 12 can dr fixit waterproofing 1l
                - 2 roll havells copper wire 1.5mm
                
                Delivery Site: Balaji Construction Hub, Sector 62
                Contact: Rajesh Contractor (9810998877)
            """.trimIndent()
        ),
        SampleOrderPreset(
            title = "Auto Parts & Garage WhatsApp",
            subtitle = "Engine oil bottles, spark plugs & wholesale spares",
            category = "Auto Parts",
            inputFormat = "text_message",
            text = """
                Sharma Spares order list:
                12 bottle castrol activ 4t 1l
                20 pcs bosch spark plug m14
                5 roll copper wire 1.5mm
                4 pcs led bulb 9w philips
                
                Store: Sharma Auto Spares, Mayapuri Industrial Area
                Phone: 9871234567
            """.trimIndent()
        ),
        SampleOrderPreset(
            title = "Chemist Handwritten Slip",
            subtitle = "OCR transcription of handwritten medicine slip",
            category = "Handwritten Slip",
            inputFormat = "handwritten_slip",
            text = """
                ORDER SLIP - Verma Medical & Cosmetics
                - Paracetamol 500mg strip : 15 strip
                - Cough syrup 100ml : 8 bottle
                - Amoxicillin 500mg : 10 strip
                - Cetirizine 10mg : 20 strip
                - Fair & Lovely 50g : 6 tube
                - Band-aid adhesive strips : 50 pcs
                
                Retailer: Verma Medical Hall
                Phone: 9899887766
            """.trimIndent()
        ),
        SampleOrderPreset(
            title = "Phonetic Misspellings Benchmark",
            subtitle = "Tests resolution of 'surf big', 'fair n lovly', 'siroop', 'meggi'",
            category = "Phonetic Test",
            inputFormat = "text_message",
            text = """
                urgent dispatch required:
                1) 6 tube fair n lovly 50g
                2) 20 strip paracitamol 500
                3) 5 btl health plus cough siroop
                4) 10 peti meggi 2 min noodles
                5) 3 bag surf big
                6) 3 roll havels copper wire 1.5
                7) 15 pcs bulb 9 watt philips
                8) 5 kg loose washing powder
                
                Store: Krishna Daily Needs
                Phone: 9711223300
            """.trimIndent()
        )
    )
}
