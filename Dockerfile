# syntax=docker/dockerfile:1
ARG BUILD_HASH=dev-build
ARG UID=0
ARG GID=0

######## WebUI frontend ########
FROM --platform=$BUILDPLATFORM node:22-alpine3.20 AS build
ARG BUILD_HASH

WORKDIR /app

RUN apk add --no-cache git

COPY package.json package-lock.json ./
RUN npm ci --force

COPY . .
ENV APP_BUILD_HASH=${BUILD_HASH}
RUN npm run build

######## WebUI backend ########
FROM python:3.11-slim-bookworm AS base

ARG UID
ARG GID

ENV PYTHONUNBUFFERED=1

ENV ENV=prod \
    PORT=8080

WORKDIR /app/backend

ENV HOME=/root

RUN if [ $UID -ne 0 ]; then \
    if [ $GID -ne 0 ]; then \
    addgroup --gid $GID app; \
    fi; \
    adduser --uid $UID --gid $GID --home $HOME --disabled-password --no-create-home app; \
    fi

RUN mkdir -p $HOME/.cache/chroma
RUN echo -n 00000000-0000-0000-0000-000000000000 > $HOME/.cache/chroma/telemetry_user_id
RUN chown -R $UID:$GID /app $HOME

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    git build-essential pandoc gcc netcat-openbsd curl jq ca-certificates \
    python3-dev \
    ffmpeg zstd \
    && rm -rf /var/lib/apt/lists/*

COPY --chown=$UID:$GID ./backend/requirements.txt ./requirements.txt

ENV UV_LINK_MODE=copy

RUN pip3 install --no-cache-dir uv && \
    uv pip install --system -r requirements.txt --no-cache-dir && \
    mkdir -p /app/backend/data && \
    chown -R $UID:$GID /app/backend/data/

COPY --chown=$UID:$GID --from=build /app/build /app/build
COPY --chown=$UID:$GID --from=build /app/CHANGELOG.md /app/CHANGELOG.md
COPY --chown=$UID:$GID --from=build /app/package.json /app/package.json

COPY --chown=$UID:$GID ./backend .

# The backend rewrites its bundled static assets (favicons, splash, manifest,
# loader.js, ...) under open_webui/static at startup. Make that directory
# writable by an arbitrary UID -- which under OpenShift's restricted SCC is
# always a member of GID 0 -- so those writes don't fail with EACCES and crash
# the boot log with "[Errno 13] Permission denied". `chmod -R g=u` mirrors the
# owner bits onto the group (the Red Hat arbitrary-UID idiom). This is applied
# unconditionally because it targets a directory the app writes on every start;
# the broader, opt-in USE_PERMISSION_HARDENING below covers the rest of /app.
RUN chgrp -R 0 /app/backend/open_webui/static && \
    chmod -R g=u /app/backend/open_webui/static

EXPOSE 8080

HEALTHCHECK CMD curl --silent --fail http://localhost:${PORT:-8080}/health | jq -ne 'input.status == true' || exit 1

USER $UID:$GID

ARG BUILD_HASH
ENV WEBUI_BUILD_VERSION=${BUILD_HASH}
ENV DOCKER=true

CMD [ "bash", "start.sh"]
