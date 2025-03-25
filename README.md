# selfcare
Monorepo for selfcare platform

| Action                                  |  in working directory  | with Maven                                                                      |
|:----------------------------------------|:----------------------:|:--------------------------------------------------------------------------------|
| Build `cucumber-sdk` and its dependents |          `.`           | `mvn --projects :cucumber-sdk --also-make-dependents clean package -DskipTests` |
| Change version  of `cucumber-sdk`       |          `.`           | `mvn versions:set -DnewVersion=0.0.3 --projects :cucumber-sdk  `                |
| Persist version  of `cucumber-sdk`      |          `.`           | `mvn versions:commit   `                                                        |
