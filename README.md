# new_assignment_1

🔌 Urja Meter Ops API Service

Welcome to the Urja Meter Ops API Service! This service provides a clean, well-structured, and programmatic HTTP API layer over the legacy "Urja Meter Ops" internal web portal (https://flockenergy.tech).
By acting as an automated reverse proxy and data parser, it translates unstructured or legacy session-bound web elements into a modern, standardized OpenAPI 3.x compliant interface. Data and product teams can now seamlessly consume smart-meter details, grid topology, and consumption logs without scraping web pages or performing manual copy-pastes.

🏗️ Architecture & Core Structure

The service is built using a decoupled Adapter/Scraper Pattern to separate upstream legacy portal mechanics from downstream API responses.
  ┌─────────────────┐       ┌───────────────────────┐       ┌─────────────────────┐
  │  Client/Program │ ───>  │  Modern API Service   │ ───>  │   Urja Meter Ops    │
  │  (Data/Product) │ <───  │ (FastAPI / Express)   │ <───  │   Legacy Portal     │
  └─────────────────┘       └───────────────────────┘       └─────────────────────┘
                             Parsed JSON responses           HTML Scrape / Hidden API

Repository Layout

• /src — Core application logic, routing, and controller endpoints.
• /src/scraper — Upstream interface layer (handles portal authentication, state tracking, and raw DOM parsing).
• openapi.json — Comprehensive REST API schema definitions.
• PROTOCOL.md — In-depth analysis of the legacy portal's hidden mechanics, auth structures, and discovery notes.
• REFLECTION.md — Detailed post-mortem and self-evaluation.

🚀 Installation & Running Locally


Prerequisites

• Runtime: Python 3.10+ (or Node.js v18+ if choosing a JS stack)
• Network: Active internet connection to authenticate with https://flockenergy.tech

Setup Steps

1. Clone the Repositorybash
git clone https://github.com
cd urja-api-service
Use code with caution.
2. Environment Configuration
Create a .env file in the root directory:env
PORT=8000
PORTAL_USERNAME=operator@urja.local
PORTAL_PASSWORD=urja-ops-2026
CACHE_TTL_SECONDS=300
Use code with caution.
3. Install Dependencies & Launch
	• If using Python (FastAPI / Uvicorn):bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn src.main:app --reload --port 8000
Use code with caution.
	• If using Node.js (Express / Fastify):bash
npm install
npm run dev
Use code with caution.
4. Interactive API Exploration
Once started, open your browser and navigate to:
	• Swagger UI / Interactive Specs: http://localhost:8000/docs
	• Raw Schema Asset: http://localhost:8000/openapi.json

📡 Sample Request & Response


Get Meter Information

• Endpoint: GET /api/v1/meters/{meter_id}
• Headers: X-API-Key: optional_internal_key
Sample Request (cURL):
bash
curl -X GET "http://localhost:8000/api/v1/meters/MTR-89240" \
     -H "accept: application/json"
Use code with caution.
Sample Response (JSON):
json
{
  "meter_id": "MTR-89240",
  "status": "active",
  "installation_date": "2024-03-15",
  "network_node": {
    "substation": "Substation-Alpha",
    "feeder": "Feeder-04",
    "transformer": "TX-Transformer-9"
  },
  "location": {
    "latitude": 28.6139,
    "longitude": 77.2090,
    "address": "Operational Zone 4, Block C"
  },
  "last_read": {
    "timestamp": "2026-10-12T12:00:00Z",
    "value_kwh": 1420.5
  }
}
Use code with caution.

🧠 Design Decisions & Trade-Offs

• Decision: Direct HTTP client emulation over headless browsers (Puppeteer/Playwright)
	• Why: Simulating direct HTTP form-posts and carrying session headers programmatically provides faster execution times (sub-50ms versus 1000ms+ overhead for spinning up Chromium instances). It dramatically reduces the local memory footprint.
	• Trade-off: If the legacy team deploys heavy client-side React code or obfuscated client-side authentication signatures in the future, this parsing technique will require a structural rewrite.
• Decision: In-Memory Token Caching
	• Why: Authenticating against the legacy application on every inbound API call creates artificial load on their system and slows down our responses. Our adapter reuses cookies until they encounter a 401 Unauthorized state.
	• Trade-off: Multi-instance deployments could suffer horizontal state drift unless shifted to a centralized Redis cache.

🛑 What Was Intentionally Skipped

• Write / Mutation Actions (POST, PUT, DELETE on meters): The legacy application is treated as strictly read-only per project parameters. No administration commands were designed.
• Persistent Database Storage: We deliberately avoided spinning up an attached database layer (PostgreSQL/MongoDB). Introducing a database creates sync latency and dual-write problems. The API serves as a real-time data facade.
• Comprehensive Multi-User RBAC: A basic API key boundary is supported, but full OAuth2/OIDC validation flows for downstream clients were skipped to keep the implementation focused on data translation.

🔮 Future Enhancements & Scalability

If granted a larger timeline, the service would benefit from the following structural components:
• Background Worker Polling: Periodically syncing the entire legacy dataset to a read-optimized Redis cache during off-peak windows. This removes the legacy system entirely from the real-time request path.
• Circuit Breakers: Implementing a resilience policy (such as Resilience4j or pybreaker) so that if the legacy portal crashes, our service fails gracefully with cached fallbacks instead of timing out.

📄 Protocol and Evaluation Material

Detailed implementation deep-dives are located inside the repository links below:
• Legacy Reverse Engineering Notes: Read PROTOCOL.md to understand session rules, network hierarchies, and scraping vulnerabilities.
• Self-Evaluation & Reflection: Read REFLECTION.md for individual project analysis, mistakes corrected along the way, and a code critique.
