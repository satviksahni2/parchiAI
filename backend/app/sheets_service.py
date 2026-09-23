"""
Multi-Tenant Google Sheets Service (gspread) for ParchAI
Enables every business / distributor to have their own separate Google Sheet.
Supports:
1. Routing by WhatsApp Business Phone Number ID (phone_number_id)
2. Direct connection via Google Sheet URL or ID
3. Automated sheet provisioning (creates sheet, formats tabs, and shares with user's email)
"""
import os
import json
import logging
from typing import Optional, Dict, Any, Tuple
from app.config import settings
from app.models import OrderResult

logger = logging.getLogger("ParchAI.SheetsService")

REGISTRY_FILE = os.path.join(os.path.dirname(__file__), "..", "distributor_sheets.json")

class GoogleSheetsService:
    def __init__(self):
        self.gc = None
        self.default_spreadsheet = None
        self.is_connected = False
        self.sheet_cache: Dict[str, Any] = {}
        self.registry: Dict[str, Dict[str, str]] = {}
        self._load_registry()
        self._initialize_sheets()

    def _load_registry(self):
        """Loads phone_number_id -> spreadsheet_id mappings from JSON file."""
        if os.path.exists(REGISTRY_FILE):
            try:
                with open(REGISTRY_FILE, "r", encoding="utf-8") as f:
                    self.registry = json.load(f)
                logger.info(f"Loaded {len(self.registry)} distributor sheet registrations.")
            except Exception as e:
                logger.error(f"Failed to read distributor_sheets.json: {e}")
                self.registry = {}
        else:
            self.registry = {}

    def _save_registry(self):
        """Saves mappings to distributor_sheets.json."""
        try:
            with open(REGISTRY_FILE, "w", encoding="utf-8") as f:
                json.dump(self.registry, f, indent=2)
            logger.info("Saved distributor sheet registry successfully.")
        except Exception as e:
            logger.error(f"Failed to save distributor_sheets.json: {e}")

    def _initialize_sheets(self):
        """Initializes gspread client using service_account.json or environment JSON."""
        try:
            import gspread

            service_account_path = settings.SERVICE_ACCOUNT_FILE
            service_account_json = settings.GOOGLE_SERVICE_ACCOUNT_JSON

            if service_account_json and service_account_json.strip():
                creds_dict = json.loads(service_account_json.strip())
                self.gc = gspread.service_account_from_dict(creds_dict)
                logger.info("Initialized gspread using GOOGLE_SERVICE_ACCOUNT_JSON environment variable.")
            elif os.path.exists(service_account_path):
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

            # Open default fallback spreadsheet if configured
            try:
                sheet_title = settings.SHEETS_SPREADSHEET_NAME
                self.default_spreadsheet = self.gc.open(sheet_title)
                self._ensure_standard_worksheets(self.default_spreadsheet)
                self.is_connected = True
                logger.info(f"Connected to default Google Sheet: '{sheet_title}'")
            except Exception as e:
                logger.info(f"Default sheet '{settings.SHEETS_SPREADSHEET_NAME}' not found or inaccessible: {e}. Multi-user mode ready.")
                self.is_connected = True

        except Exception as e:
            logger.error(f"Failed to initialize gspread: {e}")
            self.is_connected = False

    def _ensure_standard_worksheets(self, spreadsheet) -> Tuple[Any, Any]:
        """Ensures 'Catalog' and 'Orders' worksheets exist with standard column headers."""
        # 1. Orders worksheet
        try:
            orders_sheet = spreadsheet.worksheet("Orders")
        except Exception:
            orders_sheet = spreadsheet.add_worksheet(title="Orders", rows=1000, cols=8)
            orders_sheet.append_row([
                "Order Date", "Retailer Name", "Phone", "Normalized Name",
                "Quantity", "Unit of Measure", "Item Code", "Status"
            ])

        # 2. Catalog worksheet
        try:
            catalog_sheet = spreadsheet.worksheet("Catalog")
        except Exception:
            catalog_sheet = spreadsheet.add_worksheet(title="Catalog", rows=200, cols=5)
            catalog_sheet.append_row([
                "Item Code", "Standard Name", "Brand", "Unit Type", "Price"
            ])
            # Add sample row
            catalog_sheet.append_row([
                "F001", "Maggi 2-Minute Noodles 70g (Pack of 24)", "Nestle", "box", "360.00"
            ])

        return orders_sheet, catalog_sheet

    def get_spreadsheet_for_identifier(self, identifier: Optional[str] = None):
        """
        Retrieves the spreadsheet for a given phone_number_id, sheet_id, or URL.
        Falls back to default spreadsheet if not mapped.
        """
        if not self.gc:
            return None

        if not identifier:
            return self.default_spreadsheet

        # Check in memory cache
        if identifier in self.sheet_cache:
            return self.sheet_cache[identifier]

        # Check registry by phone_number_id
        target_sheet_id = identifier
        if identifier in self.registry:
            target_sheet_id = self.registry[identifier].get("spreadsheet_id", identifier)

        # Clean URL if user passed full Google Sheets link
        if "docs.google.com/spreadsheets/d/" in target_sheet_id:
            try:
                target_sheet_id = target_sheet_id.split("/d/")[1].split("/")[0]
            except Exception:
                pass

        try:
            sh = self.gc.open_by_key(target_sheet_id)
            self.sheet_cache[identifier] = sh
            return sh
        except Exception:
            try:
                sh = self.gc.open(target_sheet_id)
                self.sheet_cache[identifier] = sh
                return sh
            except Exception as e:
                logger.warning(f"Could not open sheet for identifier '{identifier}': {e}. Using default.")
                return self.default_spreadsheet

    def get_dynamic_catalog(self, identifier: Optional[str] = None) -> str:
        """
        Fetches the user-defined catalog directly from that user's specific Google Sheet.
        """
        sh = self.get_spreadsheet_for_identifier(identifier)
        if not sh:
            return ""

        try:
            catalog_sheet = sh.worksheet("Catalog")
            records = catalog_sheet.get_all_records()
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
            logger.error(f"Error reading catalog for identifier '{identifier}': {e}")
            return ""

    def log_order_to_sheets(
        self,
        name: str,
        phone: str,
        order: OrderResult,
        identifier: Optional[str] = None
    ) -> bool:
        """
        Logs parsed order line items into the user's specific 'Orders' worksheet.
        """
        sh = self.get_spreadsheet_for_identifier(identifier)
        if not sh:
            logger.warning(f"No sheet available for '{identifier}'. Skipping remote log.")
            return False

        try:
            orders_sheet, _ = self._ensure_standard_worksheets(sh)
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
                    "Pending"
                ])

            if rows_to_insert:
                orders_sheet.append_rows(rows_to_insert)
                logger.info(f"Successfully logged {len(rows_to_insert)} items to sheet '{sh.title}' for {name}.")
                return True
            return False
        except Exception as e:
            logger.error(f"Failed to log order to sheet for '{identifier}': {e}")
            return False

    def create_sheet_for_distributor(
        self,
        distributor_name: str,
        user_email: str,
        phone_number_id: str
    ) -> Dict[str, Any]:
        """
        Automatically provisions a brand-new Google Sheet for a user/distributor:
        1. Creates spreadsheet: 'Parchi - {distributor_name}'
        2. Configures 'Orders' & 'Catalog' worksheets with formatted headers
        3. Shares the sheet with user_email (Editor permission)
        4. Registers phone_number_id -> spreadsheet_id
        """
        if not self.gc:
            raise RuntimeError("Google Sheets service account is not configured.")

        title = f"Parchi - {distributor_name}"
        logger.info(f"Provisioning new Google Sheet '{title}' for {user_email}...")

        sh = self.gc.create(title)
        self._ensure_standard_worksheets(sh)

        # Share with user's Google account
        if user_email and "@" in user_email:
            try:
                sh.share(user_email.strip(), perm_type="user", role="writer", notify=True)
                logger.info(f"Shared sheet '{title}' with {user_email} as writer.")
            except Exception as e:
                logger.warning(f"Could not share sheet with {user_email}: {e}")

        # Register mapping
        self.registry[phone_number_id] = {
            "distributor_name": distributor_name,
            "user_email": user_email,
            "spreadsheet_id": sh.id,
            "spreadsheet_url": sh.url
        }
        self._save_registry()
        self.sheet_cache[phone_number_id] = sh

        return {
            "status": "SUCCESS",
            "distributor_name": distributor_name,
            "phone_number_id": phone_number_id,
            "spreadsheet_id": sh.id,
            "spreadsheet_url": sh.url,
            "shared_with": user_email
        }

    def register_existing_sheet(
        self,
        phone_number_id: str,
        spreadsheet_id_or_url: str,
        distributor_name: str,
        user_email: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Connects a user's pre-existing Google Sheet by URL or ID.
        """
        if not self.gc:
            raise RuntimeError("Google Sheets service account is not configured.")

        clean_id = spreadsheet_id_or_url.strip()
        if "docs.google.com/spreadsheets/d/" in clean_id:
            try:
                clean_id = clean_id.split("/d/")[1].split("/")[0]
            except Exception:
                pass

        # Verify access
        sh = self.gc.open_by_key(clean_id)
        self._ensure_standard_worksheets(sh)

        self.registry[phone_number_id] = {
            "distributor_name": distributor_name,
            "user_email": user_email or "",
            "spreadsheet_id": sh.id,
            "spreadsheet_url": sh.url
        }
        self._save_registry()
        self.sheet_cache[phone_number_id] = sh

        return {
            "status": "SUCCESS",
            "distributor_name": distributor_name,
            "phone_number_id": phone_number_id,
            "spreadsheet_id": sh.id,
            "spreadsheet_url": sh.url
        }

    def list_registered_distributors(self) -> Dict[str, Any]:
        """Returns all registered phone_number_id to Google Sheet mappings."""
        return {
            "total": len(self.registry),
            "distributors": self.registry
        }

# Global Sheets Service Singleton
sheets_service = GoogleSheetsService()
