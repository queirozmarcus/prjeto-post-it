# Stage 1: Build
FROM node:20-slim AS build
WORKDIR /app

COPY package*.json ./
# npm ci installs exactly what is in package-lock.json (reproducible, no version drift).
# npm install would silently upgrade within semver ranges and mutate the lockfile.
RUN npm ci --prefer-offline

COPY . .
RUN npm run build

# Stage 2: Production
FROM nginx:stable-alpine

# Create a non-root user to run nginx.
# nginx master process still starts as root to bind port 80, then drops to this user
# for worker processes. The --no-cache flag avoids leaving the apk index in the layer.
RUN apk add --no-cache shadow && \
    groupadd -r nginx-app && \
    useradd -r -g nginx-app -s /sbin/nologin nginx-app && \
    apk del shadow

COPY --from=build /app/dist /usr/share/nginx/html

# Custom nginx config for SPA routing and API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# nginx needs to write pid and temp files; ensure the non-root worker can do so
RUN chown -R nginx-app:nginx-app /var/cache/nginx /var/log/nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown nginx-app:nginx-app /var/run/nginx.pid

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
