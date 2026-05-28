#!/usr/bin/env sh
set -eu

exec mvn --global-settings settings.xml \
  quarkus:run \
  -Dquarkus.profile=integration-function \
  -DskipTests
