import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

plugins {
    alias(libs.plugins.fabric.loom)
}

val yarnMappingsBundle by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

fun DependencyHandlerScope.modInclude(dependencyProvider: Provider<out MinimalExternalModuleDependency>) {
    modImplementation(dependencyProvider)
    include(dependencyProvider)
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

dependencies {
    // Fabric + Minecraft
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    add(yarnMappingsBundle.name, variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor
    modImplementation(libs.meteor.client)
    compileOnly(libs.orbit)
    modCompileOnly(libs.baritone)

    // MCP SDK
    modInclude(libs.mcpCore)
    modInclude(libs.mcpJsonJackson2)

    // Explicitly bundled runtime deps for classloader stability in Fabric
    modInclude(libs.reactiveStreams)
    modInclude(libs.reactorCore)
    modInclude(libs.jsonSchemaValidator)
    modInclude(libs.jacksonAnnotations)
    modInclude(libs.jacksonCore)
    modInclude(libs.jacksonDatabind)
    modInclude(libs.jacksonDatatypeJdk8)
    modInclude(libs.jacksonDatatypeJsr310)

    // Embedded servlet container for HttpServlet MCP transport
    modInclude(libs.tomcatEmbedCore)

    // Tests
    testImplementation(libs.junitApi)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get(),
        )

        inputs.properties(propertyMap)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }

        from({ zipTree(yarnMappingsBundle.singleFile) }) {
            include("mappings/mappings.tiny")
            rename { "yarn.tiny" }
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())
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
