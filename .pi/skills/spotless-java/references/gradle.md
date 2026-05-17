# Spotless Gradle Configuration

## Basic Setup

build.gradle (Groovy DSL):
```groovy
plugins {
    id 'com.diffplug.spotless' version '7.0.2'
}

spotless {
    java {
        target 'src/**/*.java'
        googleJavaFormat()
    }
}
```

build.gradle.kts (Kotlin DSL):
```kotlin
plugins {
    id("com.diffplug.spotless") version "7.0.2"
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
    }
}
```

## Google Java Format Configuration

```groovy
spotless {
    java {
        googleJavaFormat('1.25.2')
            .aosp()  // Use AOSP style instead of Google
            .reflowLongStrings()
            .skipJavadocFormatting()
            .reorderImports(false)
            .groupArtifact('com.google.googlejavaformat:google-java-format')
    }
}
```

### Configuration Options

| Method | Description |
|--------|-------------|
| `aosp()` | Use AOSP style (4-space indent) instead of Google style (2-space) |
| `reflowLongStrings()` | Reflow string literals that exceed line length |
| `skipJavadocFormatting()` | Disable Javadoc formatting |
| `reorderImports(boolean)` | Control import reordering (default: true) |
| `groupArtifact(String)` | Override default Maven coordinates |

## Complete Java Configuration Example

```groovy
spotless {
    java {
        target 'src/main/java/**/*.java'

        // Use Google Java Format
        googleJavaFormat('1.25.2').aosp()

        // Import ordering
        importOrder('java', 'javax', 'org', 'com', '')

        // Remove unused imports
        removeUnusedImports()

        // Format annotations
        formatAnnotations()

        // License header
        licenseHeader '/* (C) $YEAR My Company */'
    }
}
```

## Ratchet Feature (Gradual Enforcement)

Only format changed files since a base branch:

```groovy
spotless {
    ratchetFrom 'origin/main'

    java {
        googleJavaFormat()
    }
}
```

## Toggle Off/On

Exclude sections from formatting:

```java
// spotless:off
public class LegacyCode {
    // This won't be formatted
}
// spotless:on
```

## Available Tasks

| Task | Description |
|------|-------------|
| `spotlessApply` | Apply formatting to all source files |
| `spotlessCheck` | Check formatting (fails if changes needed) |
| `spotlessDiagnose` | Show which files need formatting |

## Line Endings and Encoding

```groovy
spotless {
    lineEndings 'UNIX'  // or 'WINDOWS', 'PLATFORM_NATIVE'

    java {
        encoding 'UTF-8'
        googleJavaFormat()
    }
}
```

## Additional Java Steps

```groovy
spotless {
    java {
        googleJavaFormat()

        // Import order
        importOrder('java', 'javax', 'org', 'com', '')

        // Remove unused imports
        removeUnusedImports()

        // Forbid wildcard imports
        forbidWildcardImports()

        // Format annotations (TYPE_USE, etc.)
        formatAnnotations()

        // Clean obsolete code patterns
        cleanthat()

        // Custom license header
        licenseHeader '/* Copyright $YEAR */'
    }
}
```
