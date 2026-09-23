# WhatsApp B2B Order & Invoice Parser Backend (ParchAI / Parchi)

A production-grade Python backend built with **FastAPI**, **Google Gemini 2.5 Flash** (`google-genai` SDK), **Meta WhatsApp Cloud API**, and **Google Sheets** (`gspread`).

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
    ├── Google Sheets Service (Dynamic Catalog & Order Logging)
    │     ├── Live Read: "Catalog" worksheet (User SKU mapping)
    │     └── Live Write: "Orders" worksheet (Append pending orders)
    ├── Gemini Service (Gemini 2.5 Flash + Strict Pydantic Schema)
    │     └── [Fallback Engine (Timeout / Offline Resilience)]
    └── WhatsApp Service (Async Outbound Reply via Meta Graph API)
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
- `VERIFY_TOKEN`: A secret string you choose for the Meta Webhook handshake (e.g., `my_secure_verify_token_123`).
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

---

## 🚀 Deploying to Production (Render & Railway)

### Option A: Hosting on Render (Recommended)

1. Push your repository to **GitHub**.
2. Go to [Render Dashboard](https://dashboard.render.com/) $\rightarrow$ Click **New +** $\rightarrow$ **Web Service**.
3. Select your repository.
4. Configure the settings:
   - **Name:** `distriparse-whatsapp-webhook`
   - **Root Directory:** `backend`
   - **Runtime:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
5. Under **Environment Variables**, add:
   - `GEMINI_API_KEY` = `your_gemini_api_key`
   - `VERIFY_TOKEN` = `my_secure_verify_token_123`
   - `WHATSAPP_TOKEN` = `your_meta_system_user_access_token`
6. Click **Create Web Service**.
7. Render will provide a URL like: `https://distriparse-whatsapp-webhook.onrender.com`.

---

### Option B: Hosting on Railway

1. Connect your GitHub repository at [railway.app](https://railway.app/).
2. Create a **New Project** $\rightarrow$ **Deploy from GitHub repo**.
3. Railway automatically detects `requirements.txt` and `Procfile`.
4. Under **Variables**, add:
   - `GEMINI_API_KEY` = `your_gemini_api_key`
   - `VERIFY_TOKEN` = `my_secure_verify_token_123`
   - `WHATSAPP_TOKEN` = `your_meta_system_user_access_token`
5. Under **Settings** $\rightarrow$ **Networking**, click **Generate Domain**.

---

## 📱 Meta WhatsApp Cloud API Configuration

1. In [Meta Developers Portal](https://developers.facebook.com/), open your app $\rightarrow$ **WhatsApp** $\rightarrow$ **Configuration**.
2. Under **Webhook**, click **Edit**:
   - **Callback URL:** `https://your-service.onrender.com/webhook`
   - **Verify Token:** The exact string you set in `VERIFY_TOKEN` (`my_secure_verify_token_123`).
3. Click **Verify and Save**. Meta will display a green checkmark.
4. Under **Webhook Fields**, subscribe to **`messages`**.
