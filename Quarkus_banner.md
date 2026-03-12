# Custom Selfcare banner

This guide explains how to automatically inject the Maven project version into your Quarkus application configuration, allowing it to be used in the banner or other properties.

## 1. Update `pom.xml`

Enable resource filtering in your module's `pom.xml` (e.g., `apps/webhook/pom.xml`). This tells Maven to process files in `src/main/resources` and replace placeholders.

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
        </resource>
    </resources>
    <!-- ... other plugins ... -->
</build>
```

## 2. Use in `src/main/resources/banner.txt`
```shell
  ____       _  __  ____               
 / ___|  ___| |/ _|/ ___|__ _ _ __ ___ 
 \___ \ / _ \ | |_| |   / _` | '__/ _ \
  ___) |  __/ |  _| |__| (_| | | |  __/
 |____/ \___|_|_|  \____\__,_|_|  \___|
                                       
µs-name Service v${project.version}
```

## 3. Add in `src/main/resources/application.properties`
```shell
quarkus.banner.path=banner.txt
```

## 3. Build the Project
Run a clean build to ensure resources are processed:
```shell
mvn clean package
```

The application.properties file in the target directory will now contain the actual version number instead of the placeholder.

