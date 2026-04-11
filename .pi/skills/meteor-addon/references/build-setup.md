# Build Setup Reference

Complete templates for Meteor addon build files using Gradle Kotlin DSL and version catalogs.

## gradle/libs.versions.toml

This is the single source of truth for all versions and dependency declarations.

```toml
[versions]
mod-version = "0.1.0"

# Fabric (https://fabricmc.net/develop)
minecraft = "1.21.11"
yarn-mappings = "1.21.11+build.3"
fabric-loader = "0.18.2"

# Loom (https://github.com/FabricMC/fabric-loom)
loom = "1.14-SNAPSHOT"

# Meteor (https://github.com/MeteorDevelopment/meteor-client/)
meteor = "1.21.11-SNAPSHOT"

# Testing
junit = "5.10.2"

[libraries]
# Fabric base
minecraft = { module = "com.mojang:minecraft", version.ref = "minecraft" }
yarn = { module = "net.fabricmc:yarn", version.ref = "yarn-mappings" }
fabric-loader = { module = "net.fabricmc:fabric-loader", version.ref = "fabric-loader" }

# Meteor client
meteor-client = { module = "meteordevelopment:meteor-client", version.ref = "meteor" }

# Testing
junitApi = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
junitEngine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit" }
junitPlatformLauncher = { module = "org.junit.platform:junit-platform-launcher" }

[plugins]
fabric-loom = { id = "fabric-loom", version.ref = "loom" }
```

### Adding extra dependencies

Add entries to `[versions]` and `[libraries]`, then reference in `build.gradle.kts`:

```toml
# In [versions]
gson = "2.11.0"
okhttp = "4.12.0"
okio = "3.6.0"
kotlin-stdlib = "1.9.10"

# In [libraries]
gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okio = { module = "com.squareup.okio:okio", version.ref = "okio" }
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin-stdlib" }
```

## build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

fun DependencyHandler.modInclude(
    dependencyProvider: Provider<out MinimalExternalModuleDependency>
) {
    modImplementation(dependencyProvider)
    include(dependencyProvider)
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor
    modImplementation(libs.meteor.client)

    // Add bundled dependencies here with modInclude():
    // modInclude(libs.gson)
    // modInclude(libs.okhttp)
    // include(libs.okio)
    // include(libs.kotlin.stdlib)

    // Testing
    testImplementation(libs.junitApi)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get()
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    test {
        useJUnitPlatform()
    }
}
```

### When bundling many dependencies (modInclude variant for DependencyHandler)

If you're bundling many dependencies, some projects use this alternate form:

```kotlin
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider

fun DependencyHandler.modInclude(dependencyProvider: Provider<out MinimalExternalModuleDependency>) {
    modImplementation(dependencyProvider)
    include(dependencyProvider)
}
```

Or for DependencyHandlerScope (when using `dependencies { }` block):

```kotlin
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.modInclude(
    dependencyProvider: Provider<out MinimalExternalModuleDependency>,
) {
    modImplementation(dependencyProvider)
    include(dependencyProvider)
}
```

## gradle.properties

Only mod identity lives here — versions are in the TOML catalog.

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.configuration-cache=true

# Mod Properties
maven_group=com.example.myaddon
archives_base_name=my-addon
```

## settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

## Build Commands

```bash
# Build the addon JAR
./gradlew build

# Clean build artifacts
./gradlew clean

# Clean + build
./gradlew clean build

# Refresh dependencies
./gradlew clean build --refresh-dependencies
```

Output JAR goes to `build/libs/`. Install by copying to `.minecraft/mods/` alongside Meteor Client and Fabric Loader.
