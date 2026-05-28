mvn --global-settings settings.xml \
  quarkus:run \
  -Dquarkus.profile=integration-function \
  -DskipTests \
  -DrepositoryId="${REPO_SELFCARE:-selfcare}" \
  -DrepoLogin="${REPO_USERNAME:-}" \
  -DrepoPwd="${REPO_PASSWORD:-}"
