# Despliegue en Railway

BarberHub se despliega como tres servicios dentro de un mismo proyecto de Railway:

| Servicio | Origen | Qué hace |
|---|---|---|
| `Postgres` | Plantilla de PostgreSQL de Railway | Base de datos |
| `api` | Este repo, carpeta `backend/` | API Spring Boot (Dockerfile) |
| `web` | Este repo, carpeta `frontend/` | Angular servido por Nginx, que reenvía `/api` a la API por la red privada |

Solo `web` necesita dominio público. La API queda privada y se accede a ella a través de Nginx, así no hace falta configurar CORS.

## 1. Crear el proyecto y la base de datos

1. En [railway.com](https://railway.com) → **New Project** → **Deploy PostgreSQL**.

## 2. Servicio `api`

1. En el proyecto: **New** → **GitHub Repo** → este repositorio.
2. **Settings** → nombre del servicio: `api` · **Root Directory**: `backend`.
3. **Variables**:

| Variable | Valor |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DATABASE_USER` | `${{Postgres.PGUSER}}` |
| `DATABASE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRET` | una cadena aleatoria de al menos 32 caracteres |
| `PORT` | `8080` |
| `DEMO_ENABLED` | `true` (crea la barbería de ejemplo `/barberia-demo`) |
| `DEMO_PASSWORD` | `demo12345` (es la que llena el botón "Usar la cuenta de ejemplo" del login) |

Las migraciones de Flyway se aplican solas al arrancar, incluida la extensión `btree_gist`.

## 3. Servicio `web`

1. **New** → **GitHub Repo** → el mismo repositorio.
2. **Settings** → nombre: `web` · **Root Directory**: `frontend`.
3. **Variables**:

| Variable | Valor |
|---|---|
| `API_UPSTREAM` | `http://api.railway.internal:8080` |

4. **Settings → Networking → Generate Domain**. Esa es la URL pública de la demo.

## 4. Comprobar

- `https://<dominio>/` → página de inicio.
- `https://<dominio>/barberia-demo` → página de reservas de la barbería de ejemplo.
- `https://<dominio>/entrar` → panel con el botón "Usar la cuenta de ejemplo" (`barberia-demo` / `demo@barberhub.co` / `demo12345`).

## Costo

Railway cobra por uso. Para una demo con poco tráfico, los tres servicios suelen quedar dentro del crédito del plan Hobby. Si no hay tráfico, puedes activar **Serverless** en `api` y `web` para que se suspendan solos.
