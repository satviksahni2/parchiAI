package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ErpExportHelper
import com.example.parser.OrderParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OrderParse B2B", appName)
    }

    @Test
    fun `parse order normalizes products and units to catalog`() {
        val rawInput = """
            10 peti maggi
            4 tube fair n lovly 50g
            5 strip paracetamol 500
            2 btl cough syrup 100ml
            1 roll copper wire 1.5mm havells
            12 pcs philips led bulb
            3 kg sugar
            Shop: Sharma Kirana Store
        """.trimIndent()

        val parsed = OrderParserEngine.parse(rawInput)

        // Sharma Kirana Store extracted
        assertEquals("Sharma Kirana Store", parsed.retailerName)

        // 7 items extracted
        assertEquals(7, parsed.items.size)

        // Maggi matched to F002 and "peti" standardized to "box"
        val maggi = parsed.items.find { it.standardName.contains("Maggi") }!!
        assertEquals("F002", maggi.itemCode)
        assertEquals("box", maggi.unitType)
        assertEquals(10.0, maggi.quantity, 0.01)
        assertTrue(maggi.matchedInCatalog)

        // Fair & Lovely phonetic misspelling resolved
        val fnl = parsed.items.find { it.standardName.contains("Fair & Lovely") }!!
        assertEquals("F001", fnl.itemCode)
        assertEquals("tube", fnl.unitType)
        assertEquals(4.0, fnl.quantity, 0.01)
        assertTrue(fnl.matchedInCatalog)

        // Sugar unmatched in catalog, item_code blank
        val sugar = parsed.items.find { it.standardName.contains("Sugar") }!!
        assertEquals("", sugar.itemCode)
        assertFalse(sugar.matchedInCatalog)
        assertEquals("kilograms", sugar.unitType)
    }

    @Test
    fun `parse PRD specific surf big and building materials`() {
        val rawInput = """
            4 bag surf big
            100 bag ultratech cement 50kg
            5 bucket asian paints white 20l
            12 can dr fixit waterproofing 1l
            Retailer: Balaji Hardware & Traders
        """.trimIndent()

        val parsed = OrderParserEngine.parse(rawInput)

        assertEquals("Balaji Hardware & Traders", parsed.retailerName)
        assertEquals(4, parsed.items.size)

        // Surf big matched to PRD SKU HPC-SURF-3KG-24
        val surf = parsed.items.find { it.itemCode == "HPC-SURF-3KG-24" }
        assertTrue("Surf big should match HPC-SURF-3KG-24", surf != null)
        assertEquals("bag", surf?.unitType)
        assertEquals(4.0, surf?.quantity ?: 0.0, 0.01)

        // UltraTech Cement matched
        val cement = parsed.items.find { it.itemCode == "BM001" }
        assertTrue("UltraTech cement should match BM001", cement != null)

        // Tally XML generation test
        val xml = ErpExportHelper.generateTallyXml(parsed)
        assertTrue(xml.contains("<VOUCHER VCHTYPE=\"Purchase Order\""))
        assertTrue(xml.contains("HPC-SURF-3KG-24"))

        // WhatsApp confirmation generation test
        val waMsg = ErpExportHelper.generateWhatsAppConfirmation(parsed)
        assertTrue(waMsg.contains("Order Received & Logged"))
        assertTrue(waMsg.contains("Balaji Hardware & Traders"))
    }

    @Test
    fun `parse conversational whatsapp message with bhaiya, patta, peti and time urgency`() {
        val rawInput = "Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."
        val parsed = OrderParserEngine.parse(rawInput)

        // 3 items extracted
        assertEquals(3, parsed.items.size)

        // 1. Maggi
        val maggi = parsed.items.find { it.itemCode == "F002" }
        assertTrue("Maggi should be matched to F002", maggi != null)
        assertEquals(10.0, maggi?.quantity ?: 0.0, 0.01)
        assertEquals("box", maggi?.unitType)
        assertTrue(maggi?.matchedInCatalog == true)

        // 2. Paracetamol
        val pcm = parsed.items.find { it.itemCode == "P001" }
        assertTrue("Paracetamol should be matched to P001", pcm != null)
        assertEquals(5.0, pcm?.quantity ?: 0.0, 0.01)
        assertEquals("strip", pcm?.unitType)
        assertTrue(pcm?.matchedInCatalog == true)

        // 3. Philips LED Bulb 9W
        val bulb = parsed.items.find { it.itemCode == "E002" }
        assertTrue("LED bulb should be matched to E002", bulb != null)
        assertEquals(20.0, bulb?.quantity ?: 0.0, 0.01)
        assertEquals("pieces", bulb?.unitType)
        assertTrue(bulb?.matchedInCatalog == true)

        // Summary calculations
        assertEquals(3, parsed.summary.totalItemsRequested)
        assertEquals(3, parsed.summary.catalogMatchedCount)
        assertEquals(0, parsed.summary.unmatchedCount)
        assertTrue(parsed.summary.grandTotalInr > 0)
    }
}
