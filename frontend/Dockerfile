# --- build ---
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build

# --- runtime: nginx sirve el build y reenvía /api al backend ---
FROM nginx:1.27-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# API_UPSTREAM: URL del backend (en docker compose, http://api:8080; en Railway, la URL interna del servicio).
ENV API_UPSTREAM=http://api:8080
ENV PORT=80
EXPOSE 80
