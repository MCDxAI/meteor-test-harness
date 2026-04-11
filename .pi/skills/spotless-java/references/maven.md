# Spotless Maven Configuration

## Basic Setup

pom.xml:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.diffplug.spotless</groupId>
            <artifactId>spotless-maven-plugin</artifactId>
            <version>2.44.3</version>
            <configuration>
                <java>
                    <googleJavaFormat/>
                </java>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Google Java Format Configuration

```xml
<configuration>
    <java>
        <googleJavaFormat>
            <version>1.25.2</version>
            <style>GOOGLE</style>
            <reflowLongStrings>true</reflowLongStrings>
            <formatJavadoc>true</formatJavadoc>
        </googleJavaFormat>
    </java>
</configuration>
```

### Configuration Options

| Element | Values | Description |
|---------|--------|-------------|
| `version` | e.g., `1.25.2` | Google Java Format version |
| `style` | `GOOGLE` or `AOSP` | Formatting style (2-space vs 4-space indent) |
| `reflowLongStrings` | `true`/`false` | Reflow string literals exceeding line length |
| `formatJavadoc` | `true`/`false` | Format Javadoc comments |

## Complete Java Configuration Example

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.44.3</version>
    <configuration>
        <java>
            <includes>
                <include>src/main/java/**/*.java</include>
                <include>src/test/java/**/*.java</include>
            </includes>

            <!-- Google Java Format -->
            <googleJavaFormat>
                <version>1.25.2</version>
                <style>AOSP</style>
            </googleJavaFormat>

            <!-- Import ordering -->
            <importOrder>
                <order>java,javax,org,com,</order>
            </importOrder>

            <!-- Remove unused imports -->
            <removeUnusedImports/>

            <!-- Format annotations -->
            <formatAnnotations/>

            <!-- License header -->
            <licenseHeader>
                <content>/* (C) $YEAR My Company */</content>
            </licenseHeader>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

## Ratchet Feature (Gradual Enforcement)

Only format changed files:

```xml
<configuration>
    <ratchetFrom>origin/main</ratchetFrom>

    <java>
        <googleJavaFormat/>
    </java>
</configuration>
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

## Available Goals

| Goal | Description |
|------|-------------|
| `spotless:check` | Check formatting (fails if changes needed) |
| `spotless:apply` | Apply formatting to all source files |
| `spotless:help` | Display help information |

## Line Endings and Encoding

```xml
<configuration>
    <lineEndings>UNIX</lineEndings>  <!-- or WINDOWS, PLATFORM_NATIVE -->

    <java>
        <encoding>UTF-8</encoding>
        <googleJavaFormat/>
    </java>
</configuration>
```

## Additional Java Steps

```xml
<java>
    <googleJavaFormat/>

    <!-- Import order -->
    <importOrder>
        <order>java,javax,org,com,</order>
    </importOrder>

    <!-- Remove unused imports -->
    <removeUnusedImports/>

    <!-- Forbid wildcard imports -->
    <forbidWildcardImports/>

    <!-- Format annotations -->
    <formatAnnotations/>

    <!-- Custom license header from file -->
    <licenseHeader>
        <file>${project.basedir}/license-header.txt</file>
    </licenseHeader>
</java>
```
