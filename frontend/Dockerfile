# ---- stage 1: build ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- stage 2: nginx ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# nginx worker radi kao korisnik "nginx" - fajlovi (npr. iz public/) s pravima
# 600 bi mu bili nečitljivi (403). Svima dopusti čitanje.
RUN chmod -R a+rX /usr/share/nginx/html

EXPOSE 80
