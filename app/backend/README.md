# WhatsApp B2B Order & Invoice Parser Backend (DistriParse / Auto-PO)

A production-grade Python backend built with **FastAPI**, **Google Gemini 2.5 Flash** (`google-genai` SDK), and **Meta WhatsApp Cloud API**.

---

## 🏗 System Architecture

```text
[ Retailer (WhatsApp) ]
         │
         ▼  (Inbound Webhook HTTP POST)
[ Meta Cloud API ]
         │
         ▼  (HTTPS POST /webhook)
[ FastAPI Server (Render / Railway) ]
    ├── Webhook Router (HMAC & Challenge Handshake GET /webhook)
    ├── Gemini Service (Gemini 2.5 Flash + Strict Pydantic Schema)
    │     └── [Fallback Engine (Timeout / Offline Resilience)]
    ├── WhatsApp Service (Async Outbound Reply via Meta Graph API)
    └── Downstream (ERP / Tally / Accounting / Google Sheets)
```

### Pydantic Output Contract (`OrderResult`)

Gemini strictly enforces this structured JSON schema on every extraction:

```json
{
  "retailer_name": "Sharma Kirana Store",
  "order_date": "2026-09-22",
  "line_items": [
    {
      "original_text": "10 peti maggi",
      "normalized_name": "Maggi 2-Minute Noodles 70g (Pack of 24)",
      "item_code": "F002",
      "quantity": 10.0,
      "unit_of_measure": "box",
      "matched_in_catalog": true
    },
    {
      "original_text": "5 patta paracetamol",
      "normalized_name": "Paracetamol 500mg Strip",
      "item_code": "P001",
      "quantity": 5.0,
      "unit_of_measure": "strip",
      "matched_in_catalog": true
    },
    {
      "original_text": "20 philip led bulb 9 watt today evening quickly.",
      "normalized_name": "LED Bulb 9W",
      "item_code": "E002",
      "quantity": 20.0,
      "unit_of_measure": "pieces",
      "matched_in_catalog": true
    }
  ],
  "order_notes": "Urgency noted: today evening quickly"
}
```

---

## 🛠 Local Development & Testing

### 1. Installation

```bash
cd backend
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

pip install -r requirements.txt
```

### 2. Environment Configuration

Copy the template `.env.example` into `.env`:

```bash
cp .env.example .env
```

Set the following variables:
- `GEMINI_API_KEY`: Your Google AI Studio API key (`AIzaSy...`).
- `VERIFY_TOKEN`: A secret string you choose for the Meta Webhook handshake (e.g., `distriparse_secret_token_123`).
- `WHATSAPP_TOKEN`: Permanent System User Access Token from Meta Business Suite.

### 3. Run the Development Server

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

- **Interactive API Documentation (Swagger):** `http://localhost:8000/docs`
- **Health Check:** `http://localhost:8000/health`

### 4. Running the Pytest Suite

```bash
pytest -v
```

This runs:
- `tests/test_webhook.py`: Meta verification handshake, 403 token mismatch, 400 malformed non-JSON payloads, receipt events without messages, non-text message handling.
- `tests/test_parser.py`: Strict schema enforcement, Hinglish unit normalization (`peti` $\rightarrow$ `box`, `patta` $\rightarrow$ `strip`), uncataloged item flagging (`matched_in_catalog=False`), and timeout fallback handling.

---

## 🚀 Deploying to Production (Render & Railway)

Meta WhatsApp Cloud API requires a publicly accessible **HTTPS** URL. Deploying to Render or Railway provides an SSL endpoint out of the box.

### Option A: Hosting on Render (Recommended)

1. Push your repository to **GitHub** or **GitLab**.
2. Go to [Render Dashboard](https://dashboard.render.com/) $\rightarrow$ Click **New +** $\rightarrow$ **Web Service**.
3. Select your repository.
4. Configure the settings:
   - **Name:** `distriparse-whatsapp-webhook`
   - **Root Directory:** `backend` (if repo root is not `backend`)
   - **Runtime:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - **Plan:** Free or Starter
5. Under **Environment Variables**, add:
   - `GEMINI_API_KEY` = `your_gemini_api_key`
   - `VERIFY_TOKEN` = `your_meta_verify_token`
   - `WHATSAPP_TOKEN` = `your_meta_system_user_access_token`
6. Click **Create Web Service**.
7. Render will provide a URL like: `https://distriparse-whatsapp-webhook.onrender.com`.

---

### Option B: Hosting on Railway

1. Install the Railway CLI or connect your GitHub repository at [railway.app](https://railway.app/).
2. Create a **New Project** $\rightarrow$ **Deploy from GitHub repo**.
3. Railway automatically detects `requirements.txt` and `Procfile`.
4. Under **Variables**, add:
   - `GEMINI_API_KEY` = `your_gemini_api_key`
   - `VERIFY_TOKEN` = `your_meta_verify_token`
   - `WHATSAPP_TOKEN` = `your_meta_system_user_access_token`
5. Under **Settings** $\rightarrow$ **Networking**, click **Generate Domain** (e.g., `distriparse-production.up.railway.app`).

---

## 📱 Meta WhatsApp Cloud API Configuration

Once your service is live on Render or Railway:

1. Open the [Meta for Developers Portal](https://developers.facebook.com/).
2. Navigate to **WhatsApp** $\rightarrow$ **Configuration** in the left sidebar.
3. In the **Webhook** section, click **Edit**:
   - **Callback URL:** `https://your-service.onrender.com/webhook`
   - **Verify Token:** The exact string you set in `VERIFY_TOKEN` (e.g. `distriparse_secret_token_123`).
4. Click **Verify and Save**. Meta will send a `GET /webhook?hub.mode=subscribe&hub.challenge=...` request. The FastAPI server validates the token and responds with the challenge, displaying a green checkmark.
5. Under **Webhook Fields**, click **Manage** and subscribe to **`messages`**.
6. Send a test WhatsApp message to your business test number:
   > *"Bhaiya, send 10 peti maggi, 5 patta paracetamol, and 20 philip led bulb 9 watt today evening quickly."*
7. Check your Render/Railway logs to observe Gemini 2.5 Flash parsing the message and returning the automated confirmation breakdown.
