# selfcare
Monorepo for selfcare platform

| Action                                           |  in working directory  | with Maven                                                                               |
|:-------------------------------------------------|:----------------------:|:-----------------------------------------------------------------------------------------|
| Build `selfcare-cucumber-sdk` and its dependents |          `.`           | `mvn --projects :selfcare-cucumber-sdk --also-make-dependents clean package -DskipTests` |
| Change version  of `selfcare-cucumber-sdk`       |          `.`           | `mvn versions:set -DnewVersion=0.0.3 --projects :selfcare-cucumber-sdk  `                |
| Persist version  of `selfcare-cucumber-sdk`      |          `.`           | `mvn versions:commit   `                                                                 |
