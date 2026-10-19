# Kubernetes - Gold Tracker

Isti uzorak kao u moj-kompic projektu: Kustomize (ugrađen u `kubectl`), lokalni
k3d klaster (k3s u Dockeru) s Traefikom i cert-managerom, te CI koji na svakom
PR-u digne privremeni klaster i testira deploy.

```
k8s/
├── base/                  zajedničko (Deployment/Service/..., bez namespacea i hostova)
│   ├── postgres.yaml      StatefulSet + headless Service + PVC
│   ├── backup.yaml        CronJob: dnevni pg_dump u zaseban PVC
└── overlays/
    ├── local/             k3d + Traefik + cert-manager (Ingress, TLS, secret.env)
    └── ci/                GitHub Actions (bez Ingressa, testni secreti)
```

## Pokretanje

Preduvjeti: Docker Desktop + `brew install k3d kubectl`.

```bash
./scripts/k8s-local.sh up        # klaster + cert-manager + build + deploy
./scripts/k8s-local.sh status
./scripts/k8s-local.sh deploy    # nakon promjene koda: rebuild + rollout
./scripts/k8s-local.sh trust-ca  # macOS vjeruje lokalnom CA -> HTTPS bez upozorenja
./scripts/k8s-local.sh stop      # pauza (podaci ostaju); "up" nastavlja
./scripts/k8s-local.sh down      # briše klaster i podatke
```

Adrese:

- https://gold.localtest.me

Svaki projekt ima svoj k3d klaster (`gold-tracker`), a svi koriste portove
80/443 - istovremeno radi samo jedan. Skripta te upozori ako je drugi upaljen
(`k3d cluster stop <ime>`).

Baza (Postgres StatefulSet) ima vlastiti PVC, a CronJob `postgres-backup` svaki dan
u 03:00 radi `pg_dump` u zaseban PVC (`./scripts/k8s-local.sh backup` za ručni backup).

## Specifično za ovaj projekt

- Backend je dobio `spring-boot-starter-actuator` (health probe za Kubernetes).
- Frontend nginx radi `proxy_pass` na `http://backend:4100` - backend Service se zato zove `backend`.
- Backend ima dnevni scheduler (scraper + email) pa je `replicas: 1` i strategija
  `Recreate` - da nikad dva poda istovremeno ne pošalju email.
- Email je lokalno isključen (`EMAIL_ENABLED=false`). Za prave emailove popuni
  `SMTP_*`/`EMAIL_*` u `k8s/overlays/local/secret.env` i postavi `EMAIL_ENABLED=true`
  u `k8s/overlays/local/kustomization.yaml`.

## CI

`.github/workflows/k8s.yml`: validira manifeste (kubeconform), builda image(e),
digne efemerni k3d klaster na runneru, deploya `overlays/ci` i napravi smoke test.
