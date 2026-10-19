# Gold Tracker — Centar Zlata (Spring Boot + React)

Aplikacija koja svako jutro provjerava podstranice na **centarzlata.com**
(zlatne poluge, zlatnici, srebro...) i detektira:
- 🆕 nove proizvode
- 💰 promjene cijena
- ⚠️ promjene dostupnosti (dostupno / nedostupno)
- ❌ proizvode koji su nestali sa stranice

Sve promjene se logiraju u bazu, prikazuju na dashboardu i šalju na email.

## Migracija s Node/Express + vanilla JS

Ovo je prepravljena verzija originalnog Node projekta, na istom
stack obrascu kao ostali projekti (Spring Boot + React + Docker + Nginx +
GitHub Actions + GHCR):

| | Prije | Sad |
|---|---|---|
| Backend | Node/Express | **Spring Boot 3 (Java 21)** |
| Scraping | axios + cheerio | **Java HttpClient + Jsoup** |
| Baza | SQLite (better-sqlite3) | **PostgreSQL + Flyway** |
| Scheduler | node-cron | **Spring `@Scheduled` (dinamički cron iz `.env`)** |
| Email | nodemailer | **Spring `JavaMailSender`** |
| Frontend | vanilla JS/HTML/CSS | **React (Vite)** |
| Serviranje frontenda | Express static | **Nginx** (proxira `/api` na backend) |

**API JSON polja su sada camelCase** (npr. `pageLabel`, `inStock`, `imageUrl`,
`detectedAt`) umjesto snake_case iz Node verzije (`page_label`, `in_stock`...) —
prirodnije za Java/Jackson, a React frontend je već usklađen.

**Napomena o `SCRAPER_ENGINE=playwright`:** Node verzija je imala fallback na
headless Chrome (Playwright) ako axios dobije blokadu. Ta opcija **nije
prenesena** u Java verziju (headless browser u Javi bi zahtijevao dodatnu,
tešku ovisnost poput Selenium/Playwright-Java). Ako centarzlata.com počne
blokirati obične HTTP zahtjeve, javi pa dodajemo tu opciju.

**Napomena o `/api/products/{slug}/history`:** slug je puni URL proizvoda i
sadrži `/`, pa je endpoint promijenjen u query parametar:
`GET /api/products/history?slug=...` (u Node verziji je postojao kao path
parametar, ali ga stari frontend nije ni koristio).

### Popravci iz Node verzije su preneseni

Oba popravka iz kolovoza 2026. (kontaminacija cijena između susjednih
proizvoda zbog preširokog CSS selektora, i lažne "uklonjen proizvod"
obavijesti kad dohvat stranice padne) su ugrađeni od početka u
`ScraperService`/`ScraperPersistenceService`, zajedno sa sanity-checkom za
apsurdne skokove cijena (`PRICE_SANITY_MAX_RATIO`).

## Pokretanje lokalno (Docker Compose)

```bash
cp .env.example .env
# uredi .env: EMAIL/SMTP podaci, po zelji CRON_SCHEDULE
docker compose up -d --build
```

- Frontend (dashboard): http://localhost:8080
- Backend API direktno: http://localhost:4100/api/...
- Postgres podaci ostaju u Docker volumenu `gold_tracker_db_data`

## Razvoj bez Dockera

**Backend** (treba lokalni Postgres na 5432, ili `docker compose up db`):
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
(profil `dev` aktivira CORS dopuštenje za Vite dev server na `:5173` — bez njega
Spring blokira POST/PATCH/DELETE pozive s tog origina; u Docker/produkciji se
ovaj profil ne postavlja, nije ni potreban jer Nginx radi proxy na isti origin.)
Backend sluša na `:4100`.

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```
Vite dev server (`:5173`) proxira `/api` pozive na `localhost:4100` (vidi
`vite.config.js`) — nema potrebe za CORS podešavanjem.

Ručna provjera bez čekanja na scheduler:
```bash
curl -X POST http://localhost:4100/api/run-now
```

## Postavke koje vjerojatno želiš prilagoditi

1. **`.env` → `CRON_SCHEDULE`** — default je `0 7 * * *` (svaki dan u 7:00),
   isti format kao prije (5-poljni unix cron).
2. **`.env` → SMTP podaci** — za Gmail treba "App Password", ne redovnu lozinku.
3. **Stranice za praćenje** — dodane su 4 default podstranice
   (seedaju se pri prvom pokretanju, vidi `DataSeeder.java`). Dodaj/ukloni/uredi
   ih kroz tab **"Stranice za praćenje"** na dashboardu — ne treba dirati kod.

## Ako scraper prestane pronalaziti proizvode

WooCommerce teme povremeno mijenjaju HTML strukturu, a stranica može imati
i anti-bot zaštitu (Cloudflare i sl.):

1. Otvori jednu od kategorija u browseru → Inspect element → provjeri
   klase oko proizvoda (`li.product`, `.woocommerce-loop-product__title`, `.price`)
   i uskladi selektore u `backend/src/main/java/.../service/ScraperService.java`
   → `parseProducts()`.
2. `REQUEST_DELAY_MS` u `.env` kontrolira pauzu između zahtjeva prema
   stranicama — ne smanjuj previše, da se ne opterećuje njihov server.
3. Ako se u logovima (`docker compose logs -f backend`) pojavljuju upozorenja
   "Preskočena sumnjiva promjena cijene...", to znači da je sanity-check
   uhvatio potencijalno krivo parsanu cijenu za taj proizvod — vrijedi
   provjeriti njegovu karticu ručno.

## CI/CD — automatski build na push

Na svaki push na `main`, GitHub Actions (`.github/workflows/docker-publish.yml`)
builda **dva** Docker image-a (backend i frontend, matrix build) i pušta ih na
GitHub Container Registry:
- `ghcr.io/simebarisic/gold-tracker-backend:latest`
- `ghcr.io/simebarisic/gold-tracker-frontend:latest`

Ne treba dodatna konfiguracija — koristi ugrađeni `GITHUB_TOKEN`. Build je za
`linux/amd64`.

### Na home serveru: povlačenje gotovih image-a

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Ako je repo (pa time i GHCR paketi) privatan, prvo se treba prijaviti na
serveru jednom:

```bash
echo <GitHub_PAT_s_read:packages_pravom> | docker login ghcr.io -u simebarisic --password-stdin
```

(PAT napravi na GitHub → Settings → Developer settings → Personal access
tokens, scope `read:packages`.) Alternativa: postaviti oba GHCR paketa na
*public*, pa login nije potreban.

## Struktura projekta

```
gold-tracker/
├── backend/                          # Spring Boot API
│   ├── src/main/java/com/simebarisic/goldtracker/
│   │   ├── model/                    # JPA entiteti
│   │   ├── repository/               # Spring Data repozitoriji
│   │   ├── service/                  # ScraperService, NotifierService...
│   │   ├── controller/               # REST kontroleri
│   │   ├── dto/                      # request/response objekti
│   │   └── config/                   # scheduler, CORS, seed default stranica
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/V1__init.sql # Flyway sema
│   └── Dockerfile
├── frontend/                         # React (Vite) dashboard
│   ├── src/
│   │   ├── App.jsx                   # tabovi, "Provjeri sada"
│   │   ├── api.js                    # klijent za backend API
│   │   └── components/               # ProductsTab, ChangesTab, PagesTab, RunsTab
│   ├── nginx.conf                    # proxira /api na backend u produkciji
│   └── Dockerfile
├── docker-compose.yml                # lokalni build (postgres + backend + frontend)
├── docker-compose.prod.yml           # povlaci gotove image s GHCR-a
└── .github/workflows/docker-publish.yml
```

## Kubernetes (lokalno, za učenje)

Kustomize manifesti su u `k8s/`, lokalni k3d klaster se diže jednom naredbom:

```bash
brew install k3d kubectl      # jednom
./scripts/k8s-local.sh up
```

Detalji (struktura, adrese, specifičnosti ovog projekta, CI): [`k8s/README.md`](k8s/README.md).
