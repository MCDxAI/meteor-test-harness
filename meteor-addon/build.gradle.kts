plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    mavenCentral()
}

val modInclude: Configuration by configurations.creating

configurations {
    implementation.configure { extendsFrom(modInclude) }
    include.configure { extendsFrom(modInclude) }
}

dependencies {
    // Fabric + Minecraft
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    // Meteor
    implementation(libs.meteor.client)
    compileOnly(libs.orbit)
    compileOnly(libs.baritone)

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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
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
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    test {
        useJUnitPlatform()
    }
}
