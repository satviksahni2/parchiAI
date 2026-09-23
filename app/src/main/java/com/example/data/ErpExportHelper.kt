package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.ParsedOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErpExportHelper {

    /**
     * Generates standard Tally Prime / Tally 9 compatible XML payload for importing
     * a Purchase Order voucher into Tally ERP.
     */
    fun generateTallyXml(order: ParsedOrder, companyName: String = "Apex Wholesale Distributors Ltd."): String {
        val dateFormatted = try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = parser.parse(order.orderDate) ?: Date()
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(d)
        } catch (e: Exception) {
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        }

        val voucherNumber = order.orderId.replace("ORD-", "PO-")
        val partyLedger = escapeXml(if (order.retailerName.isNotBlank()) order.retailerName else "Sundry Debtors - Walk-in")

        val sb = StringBuilder()
        sb.appendLine("""<ENVELOPE>""")
        sb.appendLine("""  <HEADER>""")
        sb.appendLine("""    <TALLYREQUEST>Import Data</TALLYREQUEST>""")
        sb.appendLine("""  </HEADER>""")
        sb.appendLine("""  <BODY>""")
        sb.appendLine("""    <IMPORTDATA>""")
        sb.appendLine("""      <REQUESTDESC>""")
        sb.appendLine("""        <REPORTNAME>Vouchers</REPORTNAME>""")
        sb.appendLine("""        <STATICVARIABLES>""")
        sb.appendLine("""          <SVCURRENTCOMPANY>${escapeXml(companyName)}</SVCURRENTCOMPANY>""")
        sb.appendLine("""        </STATICVARIABLES>""")
        sb.appendLine("""      </REQUESTDESC>""")
        sb.appendLine("""      <REQUESTDATA>""")
        sb.appendLine("""        <TALLYMESSAGE xmlns:UDF="TallyUDF">""")
        sb.appendLine("""          <VOUCHER VCHTYPE="Purchase Order" ACTION="Create" OBJVIEW="Invoice Voucher View">""")
        sb.appendLine("""            <DATE>$dateFormatted</DATE>""")
        sb.appendLine("""            <NARRATION>WhatsApp B2B Order parsed by Auto-PO middleware. Retailer Phone: ${escapeXml(order.retailerPhone)}</NARRATION>""")
        sb.appendLine("""            <VOUCHERTYPENAME>Purchase Order</VOUCHERTYPENAME>""")
        sb.appendLine("""            <VOUCHERNUMBER>$voucherNumber</VOUCHERNUMBER>""")
        sb.appendLine("""            <PARTYLEDGERNAME>$partyLedger</PARTYLEDGERNAME>""")
        sb.appendLine("""            <BASICBUYERNAME>$partyLedger</BASICBUYERNAME>""")
        sb.appendLine("""            <PERSISTEDVIEW>Invoice Voucher View</PERSISTEDVIEW>""")

        // Line items inventory entries
        for (item in order.items) {
            val itemName = escapeXml(if (item.matchedInCatalog) item.standardName else item.requestedRawName)
            val qtyStr = "${item.quantity} ${item.unitType}"
            val rateStr = "₹${item.unitPriceInr}/${item.unitType}"
            val amountStr = "-%.2f".format(item.totalPriceInr) // In Tally purchase, items credited

            sb.appendLine("""            <ALLINVENTORYENTRIES.LIST>""")
            sb.appendLine("""              <STOCKITEMNAME>$itemName</STOCKITEMNAME>""")
            sb.appendLine("""              <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>""")
            sb.appendLine("""              <RATE>${item.unitPriceInr}/${item.unitType}</RATE>""")
            sb.appendLine("""              <AMOUNT>$amountStr</AMOUNT>""")
            sb.appendLine("""              <ACTUALQTY>$qtyStr</ACTUALQTY>""")
            sb.appendLine("""              <BILLEDQTY>$qtyStr</BILLEDQTY>""")
            if (item.itemCode.isNotBlank()) {
                sb.appendLine("""              <UDF:ERPITEMCODE.LIST DESC="`ERPItemCode`">""")
                sb.appendLine("""                <UDF:ERPITEMCODE>${escapeXml(item.itemCode)}</UDF:ERPITEMCODE>""")
                sb.appendLine("""              </UDF:ERPITEMCODE.LIST>""")
            }
            sb.appendLine("""            </ALLINVENTORYENTRIES.LIST>""")
        }

        // Ledger entry for party total
        sb.appendLine("""            <LEDGERENTRIES.LIST>""")
        sb.appendLine("""              <LEDGERNAME>$partyLedger</LEDGERNAME>""")
        sb.appendLine("""              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>""")
        sb.appendLine("""              <AMOUNT>%.2f</AMOUNT>""".format(order.summary.grandTotalInr))
        sb.appendLine("""            </LEDGERENTRIES.LIST>""")

        sb.appendLine("""          </VOUCHER>""")
        sb.appendLine("""        </TALLYMESSAGE>""")
        sb.appendLine("""      </REQUESTDATA>""")
        sb.appendLine("""    </IMPORTDATA>""")
        sb.appendLine("""  </BODY>""")
        sb.appendLine("""</ENVELOPE>""")

        return sb.toString()
    }

    /**
     * Generates CSV format for Excel, Busy, and traditional ERP import
     */
    fun generateExcelCsv(order: ParsedOrder): String {
        val sb = StringBuilder()
        sb.appendLine("Order ID,Date,Retailer,Phone,Item Code,Standard Name,Brand,Quantity,Unit,Unit Price (INR),Total Price (INR),Catalog Matched,Status")

        for (item in order.items) {
            val line = listOf(
                order.orderId,
                order.orderDate,
                "\"${order.retailerName.replace("\"", "\"\"")}\"",
                order.retailerPhone ?: "",
                item.itemCode,
                "\"${item.standardName.replace("\"", "\"\"")}\"",
                "\"${(item.brand ?: "").replace("\"", "\"\"")}\"",
                item.quantity.toString(),
                item.unitType,
                "%.2f".format(item.unitPriceInr),
                "%.2f".format(item.totalPriceInr),
                if (item.matchedInCatalog) "YES" else "FLAGGED",
                if (item.matchedInCatalog) "NORMALIZED" else "REVIEW_REQUIRED"
            ).joinToString(",")
            sb.appendLine(line)
        }
        return sb.toString()
    }

    /**
     * Generates automated WhatsApp reply to retailer as specified in PRD Section 2.2:
     * "The webhook replies to the retailer on WhatsApp: '✅ Order received and logged. Total items: 12.'"
     */
    fun generateWhatsAppConfirmation(order: ParsedOrder): String {
        val sb = StringBuilder()
        sb.appendLine("✅ *Order Received & Logged*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📋 *Order ID:* ${order.orderId}")
        if (order.retailerName.isNotBlank()) {
            sb.appendLine("🏪 *Shop:* ${order.retailerName}")
        }
        sb.appendLine("📅 *Date:* ${order.orderDate}")
        sb.appendLine("📦 *Total Items:* ${order.summary.totalItemsRequested} (${order.summary.catalogMatchedCount} matched)")
        if (order.summary.unmatchedCount > 0) {
            sb.appendLine("⚠️ *Flagged for Review:* ${order.summary.unmatchedCount} item(s)")
        }
        sb.appendLine("💰 *Estimated Total:* ₹${"%.2f".format(order.summary.grandTotalInr)}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("*Line Items:*")
        order.items.forEachIndexed { index, item ->
            val statusEmoji = if (item.matchedInCatalog) "✓" else "⚠️"
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
            sb.appendLine("${index + 1}. $statusEmoji *${item.standardName}*: $qtyStr ${item.unitType} (₹${"%.2f".format(item.totalPriceInr)})")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🚀 *Status:* Queued for ERP dispatch. Our fulfillment team will notify you when loaded.")
        sb.appendLine("🙏 _Thank you for doing business with us!_")
        return sb.toString()
    }

    /**
     * Opens WhatsApp (or share sheet) to send confirmation directly to retailer
     */
    fun sendWhatsAppMessage(context: Context, phoneNumber: String?, messageText: String) {
        val cleanPhone = (phoneNumber ?: "").replace(Regex("[^0-9]"), "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = if (cleanPhone.isNotBlank()) {
                val phoneWithCountry = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$phoneWithCountry&text=${Uri.encode(messageText)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(messageText)}")
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Order Confirmation via"))
        }
    }

    /**
     * Copy text to system clipboard with user toast feedback
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
