package com.example

import com.example.data.ErpExportHelper
import com.example.data.JsonHelper
import com.example.data.model.CatalogItem
import com.example.data.model.ParsedOrder
import com.example.data.model.ParsedOrderItem
import com.example.parser.NormalizationRules
import com.example.parser.OrderParserEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive feature test suite for Parchi (WhatsApp B2B Order & Invoice Parser)
 * Tests every single feature: unit normalization, catalog mapping, uncataloged items,
 * invoice GST computations, Tally XML generation, CSV export, and WhatsApp confirmations.
 */
class ParchiFeatureTest {

    private val catalog = CatalogItem.DEFAULT_CATALOG

    @Test
    fun testRegionalUnitNormalization() {
        // Test Hindi/Hinglish colloquial trade units to standardized wholesale units
        assertEquals("box", NormalizationRules.normalizeUnit("peti"))
        assertEquals("box", NormalizationRules.normalizeUnit("bx"))
        assertEquals("box", NormalizationRules.normalizeUnit("carton"))
        assertEquals("strip", NormalizationRules.normalizeUnit("patta"))
        assertEquals("strip", NormalizationRules.normalizeUnit("patti"))
        assertEquals("pieces", NormalizationRules.normalizeUnit("nag"))
        assertEquals("pieces", NormalizationRules.normalizeUnit("pcs"))
        assertEquals("bag", NormalizationRules.normalizeUnit("bori"))
        assertEquals("bag", NormalizationRules.normalizeUnit("katta"))
        assertEquals("bucket", NormalizationRules.normalizeUnit("balti"))
        assertEquals("can", NormalizationRules.normalizeUnit("tin"))
        assertEquals("roll", NormalizationRules.normalizeUnit("bundle"))
        assertEquals("bottle", NormalizationRules.normalizeUnit("btl"))
    }

    @Test
    fun testPhoneticCatalogMatching() {
        // Test phonetic spelling variations matching to exact catalog items
        val maggiMatch = NormalizationRules.findCatalogMatch("peti maggi noodles", catalog)
        assertNotNull(maggiMatch)
        assertEquals("F002", maggiMatch?.itemCode)
        assertEquals("Maggi 2-Minute Noodles 70g (Pack of 24)", maggiMatch?.standardName)

        val pcmMatch = NormalizationRules.findCatalogMatch("5 patta paracetamol 500", catalog)
        assertNotNull(pcmMatch)
        assertEquals("P001", pcmMatch?.itemCode)

        val bulbMatch = NormalizationRules.findCatalogMatch("20 philips led bulb 9w", catalog)
        assertNotNull(bulbMatch)
        assertEquals("E002", bulbMatch?.itemCode)

        val surfMatch = NormalizationRules.findCatalogMatch("4 bag surf big", catalog)
        assertNotNull(surfMatch)
        assertEquals("HPC-SURF-3KG-24", surfMatch?.itemCode)
    }

    @Test
    fun testUncatalogedItemHandling() {
        // Uncataloged items must be safely parsed with matchedInCatalog = false
        val rawInput = "10 peti maggi, 5 kg basmati rice special, 2 bori cement"
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")

        assertEquals(3, order.items.size)
        val rice = order.items.find { it.normalizedName.contains("Rice", ignoreCase = true) }
        assertNotNull("Rice item should be present", rice)
        assertFalse("Uncataloged item should have matchedInCatalog = false", rice!!.matchedInCatalog)
        assertNull("Uncataloged item should have null itemCode", rice.itemCode)
        assertEquals(5.0, rice.quantity, 0.001)
    }

    @Test
    fun testOrderSummaryAndGstCalculations() {
        val rawInput = "Bhaiya send 10 peti maggi, 5 strip paracetamol 500, and 20 philip led bulb 9 watt today evening quickly."
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")

        assertEquals(3, order.items.size)
        assertEquals(3, order.summary.catalogMatchedCount)
        assertEquals(0, order.summary.unmatchedCount)

        // Verify financial totals and GST breakdown
        assertTrue("Subtotal must be greater than zero", order.summary.subtotal > 0)
        assertTrue("Grand total must be greater than subtotal due to GST", order.summary.grandTotal >= order.summary.subtotal)
        assertEquals(
            order.summary.subtotal + order.summary.totalTax,
            order.summary.grandTotal,
            0.05
        )
        // Verify Intra-state GST split (CGST == SGST)
        assertEquals(order.summary.cgst, order.summary.sgst, 0.05)
    }

    @Test
    fun testTallyPrimeXmlExport() {
        val rawInput = "10 peti maggi, 5 strip paracetamol 500"
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")
        val xml = ErpExportHelper.generateTallyPrimeXml(order)

        assertTrue("Tally XML must contain ENVELOPE header", xml.contains("<ENVELOPE>"))
        assertTrue("Tally XML must contain TALLYMESSAGE", xml.contains("<TALLYMESSAGE"))
        assertTrue("Tally XML must contain Sales Voucher", xml.contains("<VOUCHER VCHTYPE=\"Sales\""))
        assertTrue("Tally XML must contain Maggi item", xml.contains("Maggi 2-Minute Noodles"))
        assertTrue("Tally XML must contain closing ENVELOPE tag", xml.contains("</ENVELOPE>"))
    }

    @Test
    fun testUniversalCsvExport() {
        val rawInput = "10 peti maggi, 5 strip paracetamol 500"
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")
        val csv = ErpExportHelper.generateUniversalCsv(order)

        assertTrue("CSV must have header line", csv.contains("Item Code,Product Name,Quantity,Unit,Unit Price,Amount,GST Rate,Tax Amount"))
        assertTrue("CSV must contain item code F002", csv.contains("F002"))
        assertTrue("CSV must contain item code P001", csv.contains("P001"))
    }

    @Test
    fun testWhatsAppConfirmationMessageFormatting() {
        val rawInput = "10 peti maggi, 5 strip paracetamol 500. Shop: Sharma Kirana Store"
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")
        val confirmation = ErpExportHelper.generateWhatsAppConfirmationText(order)

        assertTrue("Confirmation must have header", confirmation.contains("Order Received & Logged"))
        assertTrue("Confirmation must show retailer name", confirmation.contains("Sharma Kirana Store"))
        assertTrue("Confirmation must list Maggi", confirmation.contains("Maggi"))
        assertTrue("Confirmation must list Paracetamol", confirmation.contains("Paracetamol"))
        assertTrue("Confirmation must show status", confirmation.contains("Queued for ERP purchase order export"))
    }

    @Test
    fun testJsonHelperSerialization() {
        val rawInput = "10 peti maggi, 4 bag surf big"
        val order = OrderParserEngine.parse(rawInput, catalog, "text_message")
        val json = JsonHelper.toJson(order)

        assertTrue("JSON must contain orderId", json.contains("\"orderId\""))
        assertTrue("JSON must contain line items", json.contains("\"items\""))
        assertTrue("JSON must contain summary", json.contains("\"summary\""))
        assertTrue("JSON must contain F002", json.contains("F002"))
    }
}
