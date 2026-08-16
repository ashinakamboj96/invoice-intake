# Trusted Invoice Intake

A focused invoice-intelligence product built for the Zamp.ai
engineering take-home assignment.

Upload a PDF or image invoice → extract and structure it →
validate with deterministic rules → surface uncertainty to a
human reviewer → produce a trusted, queryable invoice record.

**Live demo:** https://invoice-intake-a1ti.onrender.com/

> Note: hosted on Render free tier — first request after inactivity
> may take 30–60 seconds to cold start.

---

## What it does

- Accepts PDF and image invoices (JPG, PNG, TIFF)
- Extracts embedded text (digital PDFs) or runs OCR (scanned docs)
- Uses an LLM to map extracted text into a canonical invoice schema
- Runs deterministic validation: arithmetic checks, format checks,
  duplicate detection
- Surfaces failures to a human reviewer with evidence
  (OCR confidence per field, specific rule that failed)
- Human can approve, correct, or dismiss each issue
- Complete Review triggers revalidation against corrected data
- Accepted invoices are searchable by vendor, date, amount, status

---

## Running locally

**Prerequisites:**
- Java 21
- Maven
- PostgreSQL 16
- An OpenAI API key

**1. Clone and configure:**
```bash
git clone https://github.com/ashinakamboj96/invoice-intake.git
cd invoice-intake
cp .env.example .env
# Edit .env and add your OpenAI API key
```

**2. Create the database:**
```bash
createdb invoice_intake
```

**3. Start the app:**
```bash
export $(cat .env | xargs) && mvn spring-boot:run
```

Flyway runs automatically on startup and creates all tables.

**4. Open the app:**
```
http://localhost:8080
```

---

## Running with Docker

```bash
docker build -t invoice-intake .
docker run -p 8080:8080 \
  -e LLM_API_KEY=your-key-here \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/invoice_intake \
  -e SPRING_DATASOURCE_USERNAME=your-db-user \
  invoice-intake
```

---

## Apple Silicon — Tesseract note

If you get a `UnsatisfiedLinkError` when running locally:
```
-Djna.library.path=/opt/homebrew/lib
```
Add this as a JVM argument in IntelliJ:
Run → Edit Configurations → VM options

The Docker image handles this automatically — note that the
`tesseract-ocr` apt package on Debian/Ubuntu installs Tesseract
**4.1.1**, not 5, so its trained-data path is
`/usr/share/tesseract-ocr/4.00/tessdata` inside the container.
This is already set as the default `OCR_TESSDATA_PATH` in both
the `Dockerfile` and `render.yaml`.

---

## Configuration

| Variable | Default | Description |
|---|---|---|
| `LLM_API_KEY` | (required) | OpenAI API key |
| `SPRING_DATASOURCE_URL` | localhost/invoice_intake | Postgres URL |
| `SPRING_DATASOURCE_USERNAME` | ashinakamboj | Postgres username |
| `SPRING_DATASOURCE_PASSWORD` | (empty) | Postgres password |
| `OCR_TESSDATA_PATH` | /opt/homebrew/share/tessdata | Tesseract data path |
| `validation.ocr-confidence-threshold` | 0.90 | OCR confidence threshold |
| `validation.min-text-length` | 50 | Min chars to skip OCR fallback |

---

## Architecture

```
Upload → DocumentTypeDetector
           ├── PDF_TEXT → PdfTextExtractor (PDFBox)
           └── OCR     → OcrExtractor (Tesseract)
         → LlmStructurer (OpenAI — semantic mapping only)
         → EvidenceMapper (OCR confidence → schema fields)
         → ValidationEngine (deterministic rules)
         → ACCEPTED / NEEDS_REVIEW / FAILED
         → Human review → Complete Review → revalidation
```

The JSON API lives under `/api/invoices` (list, detail); the
server-rendered HTML pages live at `/` and `/invoices/{id}`.
Upload (`POST /invoices`), file download
(`GET /invoices/{id}/file`), and review submission
(`POST /invoices/{id}/complete-review`) sit alongside the HTML
routes since none of them collide on path + method.

See `decisions.md` for the reasoning behind every major
architectural choice.

---

## Running tests

```bash
mvn test
```

56 tests, focused on:
- Validation engine (arithmetic, format, duplicate detection)
- Evidence mapping (OCR confidence to schema fields)
- Review service (correction, revalidation, duplicate handling,
  cross-round approval memory)
- LLM structuring (JSON parsing, null handling)
- Extraction (PDF text, OCR fallback logic)
- Invoice search/list (filtering, pagination)

---

## Sample invoices for testing

Upload any of these to see the system in action:
- A clean PDF invoice → should reach ACCEPTED immediately
- A photo of a handwritten or printed invoice → OCR path,
  likely NEEDS_REVIEW with confidence warnings
- The same invoice twice → EXACT_DUPLICATE flagged on second upload
