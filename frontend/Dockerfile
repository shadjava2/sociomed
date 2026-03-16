# Build stage (nom complet pour Podman : pas de short-name)
FROM docker.io/library/node:20-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY package.json package-lock.json* ./
RUN npm ci 2>/dev/null || npm install

COPY . .
RUN npm run build

# Run stage — Nginx (nom complet pour Podman)
FROM docker.io/library/nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
