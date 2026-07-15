# Test Coverage

This module aggregates JaCoCo coverage reports from all modules in the monorepo and sends them to SonarCloud under a **single project key** (`pagopa_selfcare`).

## Architecture

The coverage aggregation follows a **clean separation of concerns**:

1. **Build & Test**: All modules build and run tests independently
   - Each module generates its own `jacoco.exec` and `jacoco.xml` reports
   - Failures in tests cause the build to fail immediately

2. **Coverage Aggregation**: After all modules succeed, `test-coverage` collects all reports
   - Uses JaCoCo's `report-aggregate` goal
   - Combines all `jacoco.xml` files into `test-coverage/target/site/jacoco-aggregate/jacoco.xml`

3. **SonarCloud Upload**: Coverage data is sent with a **single organization key and project key**
   - All modules contribute to the same SonarCloud project
   - Pull request analysis works across the entire monorepo

## Local Usage

### Test a specific module's coverage
```bash
# Build and test a single module with coverage report
mvn --projects :test-coverage --also-make verify -Pdelegation-cdc,report -DskipITs

# Generates: test-coverage/target/site/jacoco-aggregate/jacoco.xml
```

### Full monorepo coverage aggregation
```bash
# Build all modules
mvn clean verify -DskipITs

# Then aggregate coverage reports
mvn --projects :test-coverage --also-make package -Preport -DskipTests

# Generates complete: test-coverage/target/site/jacoco-aggregate/jacoco.xml
```

## CI/CD Pipeline

The GitHub Actions workflow (`pr_sonar_analysis.yml`) follows this pattern:

```bash
# Step 1: Build all modules and run tests
mvn clean verify -DskipITs

# Step 2: Aggregate JaCoCo reports from all modules
mvn jacoco:report-aggregate -DskipTests \
  --projects :test-coverage --also-make -Preport

# Step 3: Collect all jacoco.xml paths and upload to SonarCloud
# Dynamically finds all jacoco.xml files and sends them to SonarCloud
mvn sonar:sonar --projects :root --also-make \
  -Dsonar.coverage.jacoco.xmlReportPaths="<all jacoco.xml files>" \
  -Dsonar.organization=pagopa \
  -Dsonar.projectKey=pagopa_selfcare \
  -Dsonar.token=$SONAR_TOKEN
```

## Key Points

- ✅ **Single SonarCloud key**: All modules report to `pagopa_selfcare` 
- ✅ **Fail-fast on test failures**: No `-fae` flag means the build stops on first test failure
- ✅ **Scalable**: Adding new modules automatically included in coverage reporting
- ✅ **Clean separation**: Build, aggregation, and analysis are separate concerns

## Troubleshooting

### No coverage reports generated
- Ensure all modules have the JaCoCo plugin configured (inherited from `selfcare-sdk-pom`)
- Check that `jacoco.exec` files exist in each module's `target/` directory

### SonarCloud showing 0% coverage
- Verify `test-coverage/target/site/jacoco-aggregate/jacoco.xml` exists and contains data
- Check that the `-Dsonar.coverage.jacoco.xmlReportPaths` in CI workflow points to correct locations

### Test failures not stopping the build
- Remove `-fae` flag from Maven command
- Ensure no `skipTests` or `skipITs` flags that bypass coverage
