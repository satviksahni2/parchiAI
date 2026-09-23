"""
Google Sheets Service (gspread) for ParchAI
Handles real-time dynamic catalog retrieval and live order logging to Google Sheets.
"""
import os
import json
import logging
from typing import Optional, List
from app.config import settings
from app.models import OrderResult

logger = logging.getLogger("ParchAI.SheetsService")

class GoogleSheetsService:
    def __init__(self):
        self.gc = None
        self.spreadsheet = None
        self.orders_sheet = None
        self.catalog_sheet = None
        self.is_connected = False
        self._initialize_sheets()

    def _initialize_sheets(self):
        """Initializes gspread client using service_account.json or environment JSON."""
        try:
            import gspread

            service_account_path = settings.SERVICE_ACCOUNT_FILE
            service_account_json = settings.GOOGLE_SERVICE_ACCOUNT_JSON

            if service_account_json and service_account_json.strip():
                # Load from JSON string in environment variable (Render / Cloud environment)
                creds_dict = json.loads(service_account_json.strip())
                self.gc = gspread.service_account_from_dict(creds_dict)
                logger.info("Initialized gspread using GOOGLE_SERVICE_ACCOUNT_JSON environment variable.")
            elif os.path.exists(service_account_path):
                # Load from file
                self.gc = gspread.service_account(filename=service_account_path)
                logger.info(f"Initialized gspread using service account file: {service_account_path}")
            elif os.path.exists(os.path.join("..", service_account_path)):
                alt_path = os.path.join("..", service_account_path)
                self.gc = gspread.service_account(filename=alt_path)
                logger.info(f"Initialized gspread using service account file at: {alt_path}")
            else:
                logger.warning(
                    f"No service_account.json found at '{service_account_path}' and GOOGLE_SERVICE_ACCOUNT_JSON is unset. "
                    "Google Sheets integration will operate in offline/local mode."
                )
                return

            # Open spreadsheet
            sheet_title = settings.SHEETS_SPREADSHEET_NAME
            self.spreadsheet = self.gc.open(sheet_title)
            
            # Open or verify worksheets
            try:
                self.orders_sheet = self.spreadsheet.worksheet("Orders")
            except Exception:
                logger.info("Creating 'Orders' worksheet...")
                self.orders_sheet = self.spreadsheet.add_worksheet(title="Orders", rows=1000, cols=8)
                self.orders_sheet.append_row([
                    "Order Date", "Retailer Name", "Phone", "Normalized Name",
                    "Quantity", "Unit of Measure", "Item Code", "Status"
                ])

            try:
                self.catalog_sheet = self.spreadsheet.worksheet("Catalog")
            except Exception:
                logger.info("Creating 'Catalog' worksheet...")
                self.catalog_sheet = self.spreadsheet.add_worksheet(title="Catalog", rows=100, cols=5)
                self.catalog_sheet.append_row([
                    "Item Code", "Standard Name", "Brand", "Unit Type", "Price"
                ])

            self.is_connected = True
            logger.info(f"Connected to Google Sheets: '{sheet_title}' successfully!")
        except Exception as e:
            logger.error(f"Failed to connect to Google Sheets: {e}")
            self.is_connected = False

    def get_dynamic_catalog(self) -> str:
        """
        Fetches the user-defined catalog directly from Google Sheets.
        Returns a formatted string for injection into Gemini's system instruction.
        """
        if not self.is_connected or not self.catalog_sheet:
            return ""

        try:
            records = self.catalog_sheet.get_all_records()
            if not records:
                return ""

            catalog_text = "Item Code | Standard Name | Brand | Unit Type | Price\n"
            for row in records:
                catalog_text += (
                    f"{row.get('Item Code', '')} | "
                    f"{row.get('Standard Name', '')} | "
                    f"{row.get('Brand', '')} | "
                    f"{row.get('Unit Type', '')} | "
                    f"{row.get('Price', '')}\n"
                )
            return catalog_text
        except Exception as e:
            logger.error(f"Error fetching dynamic catalog from Google Sheets: {e}")
            return ""

    def log_order_to_sheets(self, name: str, phone: str, order: OrderResult) -> bool:
        """
        Formats the parsed OrderResult into rows and appends them to the 'Orders' worksheet.
        """
        if not self.is_connected or not self.orders_sheet:
            logger.warning(f"Google Sheets not connected. Skipping remote log for {name} ({phone}).")
            return False

        try:
            rows_to_insert = []
            for item in order.line_items:
                rows_to_insert.append([
                    order.order_date or "Today",
                    name or "Unknown Retailer",
                    phone or "N/A",
                    item.normalized_name,
                    item.quantity,
                    item.unit_of_measure,
                    item.item_code or "UNMATCHED",
                    "Pending"  # Status column for manual tracking
                ])

            if rows_to_insert:
                self.orders_sheet.append_rows(rows_to_insert)
                logger.info(f"Successfully logged {len(rows_to_insert)} items to 'Orders' sheet for {name}.")
                return True
            return False
        except Exception as e:
            logger.error(f"Failed to append rows to Google Sheets: {e}")
            return False

# Global Sheets Service Singleton
sheets_service = GoogleSheetsService()
