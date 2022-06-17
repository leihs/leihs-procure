ARG RUBY_VERSION=2.7.6-slim-bullseye
ARG NODEJS_VERSION=14-bullseye-slim
ARG JAVA_VERSION=11-slim-bullseye
ARG LEIHS_UI_VERSION=dev

# more build args in later stages:
# ARG WORKDIR=/leihs/procure
# ARG LEIHS_UI_WORKDIR=/leihs-ui
# ARG GIT_COMMIT_ID=unknown-commit-id
# ARG GIT_TREE_ID=unknown-tree-id

# === STAGE: BASE JAVA ========================================================================== #
FROM openjdk:${JAVA_VERSION} as leihs-base-java

# === STAGE: BASE RUBY ========================================================================== #
FROM ruby:${RUBY_VERSION} as leihs-base-ruby

# === STAGE: BASE NODEJS ======================================================================== #
FROM node:${NODEJS_VERSION} as leihs-base-nodejs

# === STAGE: SHARED UI ========================================================================== #
FROM leihs-ui:$LEIHS_UI_VERSION as leihs-ui-dist

# === STAGE: BUILD UBERJAR ====================================================================== #
FROM leihs-base-java as builder

ARG WORKDIR=/leihs/procure
WORKDIR "$WORKDIR"

# OS deps
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        git build-essential python3 && \
    rm -rf /var/lib/apt/lists/*

# "merge" in nodejs installation from offical image (matching debian version).
COPY --from=leihs-base-nodejs /usr /usr/
# smoke check
RUN node --version && npm --version

# "merge" in ruby installation from offical image (matching debian version).
COPY --from=leihs-base-ruby /usr /usr/
RUN echo 'gem: --no-document' >> /usr/local/etc/gemrc
# smoke check
RUN ruby --version && echo "GEM: $(gem --version)" && bundle --version

# prepare clojure dependencies
# `clojure -X:deps prep`        -> Prepare all unprepped libs in the dep tree
# `clojure -T:build-leihs prep` -> hack, throws an error, but download deps before that ;)
WORKDIR "${WORKDIR}/server"
ENV PATH=${WORKDIR}/server/shared-clj/clojure/bin:$PATH
COPY server/deps.edn ./
COPY server/scripts/build.clj ./scripts/
COPY server/shared-clj ./shared-clj/
RUN clojure -X:deps prep && \
    ( clojure -T:build-leihs prep || true )

# client
WORKDIR "$WORKDIR/client"
COPY client/package.json client/package-lock.json client/.npmrc ./
RUN npm ci --no-audit --loglevel=warn

# copy sources and shared/prebuilt dependencies
ARG LEIHS_UI_WORKDIR=/leihs-ui
COPY --from=leihs-ui-dist ${LEIHS_UI_WORKDIR}/dist leihs-ui/dist/
COPY --from=leihs-ui-dist ${LEIHS_UI_WORKDIR}/bootstrap-theme-leihs/build leihs-ui/bootstrap-theme-leihs/build/
# COPY --from=leihs-ui-dist ${LEIHS_UI_WORKDIR}/bootstrap-theme-leihs-mobile/build leihs-ui/bootstrap-theme-leihs-mobile/build/

# BUILD: see bin/build (`function build`) for the steps, leaving out those that are prepared (leihs ui, dependencies, …)
# NOTE: could be further (docker-cache-)optimized by only copying the needed sources before build, but that seems brittle (e.g. copy src/client first, then build client…)
WORKDIR "$WORKDIR/client"
COPY client ./
RUN PUBLIC_URL="/procure" npm run ci-build

# runtime server dep, also needed for build. exiftool, installed from a ruby gem. put in path so it runs without ruby present.
WORKDIR "$WORKDIR"
COPY Gemfile Gemfile.lock ./
RUN BUNDLE_SYSTEM=true BUNDLE_WITHOUT=default BUNDLE_WITH=production bundle install
ENV PATH=/usr/local/lib/ruby/gems/2.7.0/gems/exiftool_vendored-11.44.0/bin:$PATH
RUN exiftool -ver

WORKDIR "$WORKDIR/server"
COPY server ./
RUN bin/clj-uberjar

# === STAGE: RUNNER ========================================================================= #
FROM builder as runner
WORKDIR "$WORKDIR"

# cleanup
RUN bash -c 'rm -f client/src/locale/{xx,zz}*'

ENTRYPOINT [ "/bin/bash" ]


# === STAGE: PROD IMAGE ========================================================================= #
FROM leihs-base-java as prod
ARG WORKDIR=/leihs/procure
WORKDIR "$WORKDIR"

COPY --from=builder /usr/local/lib/ruby/gems/2.7.0/gems/exiftool_vendored-11.44.0 /usr/local/lib/ruby/gems/2.7.0/gems/exiftool_vendored-11.44.0/
ENV PATH=/usr/local/lib/ruby/gems/2.7.0/gems/exiftool_vendored-11.44.0/bin:$PATH
RUN exiftool -ver

# "merge" in nodejs installation from offical image (matching debian version).
COPY --from=leihs-base-nodejs /usr /usr/
# smoke check
RUN node --version && npm --version

WORKDIR "$WORKDIR/server"
COPY --from=builder ${WORKDIR}/server/target/leihs-procure.jar ./target/

# config
ENV HTTP_PORT=3230

# run
EXPOSE ${HTTP_PORT}
ENTRYPOINT [ "java", "-jar", "target/leihs-procure.jar", "run" ]