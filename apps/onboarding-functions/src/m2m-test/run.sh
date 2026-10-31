#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$root"

# Build the branch artifacts before starting the isolated Quarkus test processes.
mvn -B --projects :selfcare-sdk-m2m --also-make install
mvn -B --projects :onboarding-functions --also-make -Pm2m-integration \
  install -DskipTests -Dquarkus.build.skip=true
mvn -B -f apps/auth/pom.xml -Pm2m-integration test -Dtest=MachineTokenFlowIT
