# Google Java Format Reference

## Overview

Google Java Format is a program that reformats Java source code to comply with Google Java Style. It is the default formatter used by Spotless for Java code.

## Styles

| Style | Indent | Description |
|-------|--------|-------------|
| `GOOGLE` | 2 spaces | Standard Google Java Style |
| `AOSP` | 4 spaces | Android Open Source Project style |

## IDE Integration

### IntelliJ IDEA

1. Install plugin: `google-java-format`
2. Enable in Settings → Other Settings → google-java-format
3. Configure to format on save (optional)

### VS Code

1. Install extension: `google-java-format`
2. Add to settings.json:
```json
{
    "java.format.enabled": true,
    "java.format.settings.url": ".vscode/java-formatter.xml"
}
```

### Eclipse

Use the Eclipse plugin from the google-java-format releases page.

## Command Line Usage

Format a single file:
```bash
java -jar google-java-format.jar MyClass.java
```

Format multiple files:
```bash
java -jar google-java-format.jar src/**/*.java
```

In-place modification:
```bash
java -jar google-java-format.jar -i MyClass.java
```

Dry run (show changes):
```bash
java -jar google-java-format.jar --dry-run --set-exit-if-changed src/**/*.java
```

## Key Formatting Rules

### Brace Style

```java
// Always opening brace on same line
if (condition) {
    doSomething();
}
```

### Line Length

- Maximum 100 characters
- Long lines are wrapped appropriately

### Import Order (Google Style)

```java
import java.*;      // First
import javax.*;     // Second
import org.*;       // Third
import com.*;       // Fourth
import other.*;     // Last (blank package)
```

### Annotation Formatting

```java
@SingleAnnotation
public class Example {
    @Override
    public void method() {}
}
```

Multiple annotations:
```java
@Annotation1
@Annotation2
public void method() {}
```

### Javadoc

- Reformatted to wrap at 100 characters
- Parameter descriptions aligned

## Spotless Integration

When using Google Java Format through Spotless:

### Gradle
```groovy
spotless {
    java {
        googleJavaFormat('1.25.2')
            .aosp()  // Optional: use AOSP style
    }
}
```

### Maven
```xml
<googleJavaFormat>
    <version>1.25.2</version>
    <style>GOOGLE</style>  <!-- or AOSP -->
</googleJavaFormat>
```

## Version Compatibility

| Google Java Format | Java Version |
|-------------------|--------------|
| 1.25.x | Java 17+ |
| 1.22.x | Java 17+ |
| 1.18.x | Java 11+ |
| 1.7.x to 1.17.x | Java 8+ |

## Common Issues

### Import Optimization

Google Java Format does NOT optimize imports by default. Use Spotless's additional steps:

```groovy
spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        importOrder('java', 'javax', 'org', 'com', '')
    }
}
```

### Skip Specific Code

Use Spotless toggle comments:
```java
// spotless:off
// Code here is not formatted
// spotless:on
```

### AOSP vs Google

- Use `AOSP` for Android projects (4-space indent)
- Use `GOOGLE` for server-side projects (2-space indent)
