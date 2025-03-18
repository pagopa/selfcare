# selfcare
Monorepo for selfcare platform

| Action                                                                                             |  in working directory  | with Maven                                                                               |
|:---------------------------------------------------------------------------------------------------|:----------------------:|:-----------------------------------------------------------------------------------------|
| Build `onboarding-sdk` and its dependents (aka. reverse dependencies or *rdeps* in Bazel parlance) |          `.`           | `mvn --projects :selfcare-sdk-cucumber --also-make-dependents clean package -DskipTests` |
| Change version  of `selfcare-sdk`                                                                  |          `.`           | `mvn versions:set -DnewVersion=0.0.1 --projects :selfcare-sdk-cucumber  `                |
| Persist version  of `selfcare-sdk`                                                                 |          `.`           | `mvn versions:commit   `                                                                 |
