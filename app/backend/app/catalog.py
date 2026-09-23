"""
Master Product Catalog and System Prompt for Gemini 2.5 Flash B2B Order Parsing
"""
import json

MASTER_CATALOG = [
    # Pharmaceuticals
    {
        "item_code": "P001",
        "standard_name": "Paracetamol 500mg Strip",
        "brand": "PharmaCorp",
        "unit_of_measure": "strip",
        "aliases": ["paracetamol 500", "paracetamol", "pcm 500", "crocin 500", "pcm", "paracitamol", "patta paracetamol", "patti paracetamol"]
    },
    {
        "item_code": "P002",
        "standard_name": "Cough Syrup 100ml",
        "brand": "HealthPlus",
        "unit_of_measure": "bottle",
        "aliases": ["cough syrup", "cough siroop", "khansi syrup", "cough bottle", "healthplus cough"]
    },
    {
        "item_code": "P003",
        "standard_name": "Amoxicillin 500mg Capsules",
        "brand": "PharmaCorp",
        "unit_of_measure": "strip",
        "aliases": ["amoxicillin", "amox 500", "amox", "mox 500", "antibiotic strip"]
    },
    # FMCG & Personal Care
    {
        "item_code": "HPC-SURF-3KG-24",
        "standard_name": "Surf Excel Easy Wash 3kg",
        "brand": "HUL",
        "unit_of_measure": "bag",
        "aliases": ["surf big", "surf excel 3kg", "surf 3kg", "surf excel big", "surf powder 3kg", "surf detergent big"]
    },
    {
        "item_code": "F001",
        "standard_name": "Fair & Lovely 50g",
        "brand": "HUL",
        "unit_of_measure": "tube",
        "aliases": ["fair n lovly", "fair & lovely", "fair lovely", "fair n lovely", "glow & lovely", "fair n lovly 50g"]
    },
    {
        "item_code": "F002",
        "standard_name": "Maggi 2-Minute Noodles 70g (Pack of 24)",
        "brand": "Nestle",
        "unit_of_measure": "box",
        "aliases": ["maggi", "maggie", "meggi", "maggi noodles", "peti maggi", "maggi peti", "2 min noodles"]
    },
    {
        "item_code": "F003",
        "standard_name": "Tata Salt 1kg",
        "brand": "Tata",
        "unit_of_measure": "packet",
        "aliases": ["tata salt", "salt 1kg", "tata namak", "namak", "tata iodized salt"]
    },
    # Electricals
    {
        "item_code": "E001",
        "standard_name": "Copper Wire 1.5mm",
        "brand": "Havells",
        "unit_of_measure": "roll",
        "aliases": ["copper wire", "havells wire", "havels wire 1.5", "copper wire 1.5mm", "wire 1.5mm", "havells roll"]
    },
    {
        "item_code": "E002",
        "standard_name": "LED Bulb 9W",
        "brand": "Philips",
        "unit_of_measure": "pieces",
        "aliases": ["led bulb", "bulb 9w", "philips bulb", "philip led bulb 9 watt", "philip led", "philips 9w", "philip bulb", "9 watt bulb"]
    },
    {
        "item_code": "E003",
        "standard_name": "Anchor Penta Modular Switch 6A",
        "brand": "Panasonic",
        "unit_of_measure": "pieces",
        "aliases": ["anchor switch", "anchor 6a", "switch 6a", "penta switch", "modular switch"]
    },
    # Building Materials
    {
        "item_code": "BM001",
        "standard_name": "UltraTech Super Cement 50kg",
        "brand": "UltraTech",
        "unit_of_measure": "bag",
        "aliases": ["ultratech cement", "cement 50kg", "ultratech", "cement bori", "cement katta"]
    },
    {
        "item_code": "BM002",
        "standard_name": "Asian Paints Tractor Emulsion White 20L",
        "brand": "Asian Paints",
        "unit_of_measure": "bucket",
        "aliases": ["asian paints", "asian paint white", "tractor emulsion", "paint 20l", "paint balti"]
    },
    {
        "item_code": "BM003",
        "standard_name": "Dr. Fixit Pidiproof LW+ 1L",
        "brand": "Pidilite",
        "unit_of_measure": "can",
        "aliases": ["dr fixit", "waterproofing 1l", "pidiproof", "dr fixit 1l", "dr fixit can"]
    },
    # Auto Parts & Spares
    {
        "item_code": "AP001",
        "standard_name": "Castrol Activ 4T 20W-40 1L",
        "brand": "Castrol",
        "unit_of_measure": "bottle",
        "aliases": ["castrol activ", "engine oil 1l", "castrol 4t", "20w40 oil"]
    },
    {
        "item_code": "AP002",
        "standard_name": "Bosch Super 4 Spark Plug (Set of 4)",
        "brand": "Bosch",
        "unit_of_measure": "box",
        "aliases": ["bosch spark plug", "spark plug set", "bosch plug", "plug set"]
    }
]

SYSTEM_INSTRUCTION = f"""
You are a B2B Order and Invoice Parsing Assistant for wholesale distributors.
Your task is to extract, normalize, and format order requests sent by retail shop owners over WhatsApp, transcribed voice notes, or handwritten slips.

MASTER PRODUCT CATALOG:
{json.dumps(MASTER_CATALOG, indent=2)}

CRITICAL PARSING RULES:
1. Extract every ordered line item preserving the raw text snippet in `original_text`.
2. Cross-reference items with the MASTER CATALOG using phonetic spelling resolution (e.g., 'fair n lovly' -> 'Fair & Lovely 50g', 'surf big' -> 'HPC-SURF-3KG-24', 'philip led bulb 9 watt' -> 'E002').
3. Standardize regional and colloquial units into formal wholesale units:
   - 'peti', 'bx', 'box', 'boxes' -> 'box'
   - 'patta', 'patti', 'strip', 'strips' -> 'strip'
   - 'nag', 'pcs', 'piece', 'pieces', 'nos' -> 'pieces'
   - 'bori', 'katta', 'bag', 'bags' -> 'bag'
   - 'balti', 'bucket', 'buckets' -> 'bucket'
   - 'can', 'tin', 'tins' -> 'can'
   - 'roll', 'rolls', 'bundle' -> 'roll'
   - 'bottle', 'btl', 'bottles' -> 'bottle'
   - 'tube', 'tubes' -> 'tube'
4. Default Unit Handling: If the user omits a unit (e.g. '20 philip led bulb'), adopt the catalog item's default unit ('pieces').
5. Uncataloged Items: If an item is NOT present in the catalog (e.g. '3 kg sugar'), set:
   - `matched_in_catalog` = false
   - `item_code` = null
   - `normalized_name` = Capitalized item name (e.g. 'Sugar 3kg')
   - `unit_of_measure` = standardized unit or 'kilograms'
6. Retailer Metadata & Notes:
   - Extract retailer name if mentioned in greetings or sign-offs (e.g. 'Sharma Kirana Store', 'Gupta Electricals').
   - Extract timeline or delivery instructions in `order_notes` (e.g. 'Deliver today evening quickly', 'urgent delivery required').
7. Empty / Non-order Input: If the input contains no valid order items (e.g. 'hi', 'how are you'), return an empty `line_items` array and note it in `order_notes`.
"""
