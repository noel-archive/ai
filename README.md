# 🍋 愛 (ai)
> *Simple CLI parser for Kotlin that won't make your head spin. 。.:☆*:･'(*⌒―⌒*)))*
>
> [:scroll: **Documentation**](https://ai.noelware.org)

## WHY
**ai** (case sensitive) was built out of frustration from the Kotlin CLI parsers out there like [Clikt](https://github.com/ajalt), [kotlinx.cli](https://github.com/Kotlin/kotlinx.cli), just to name a few.

With **Clikt**, I really had a hard time to build a proper CLI tool without running into errors when mixing flags and arguments and much more and I don't like the design for **kotlinx-cli**, while be minimal, just not for us.

**ai** was built to be really simple to build a CLI with a builder DSL or with OOP in mind. With **Clikt**, if you wanted to execute subcommands
from the main [sub]command, you will need to do this:

```kotlin
object SomeCommand: CliktCommand(name = "name") {
  override fun run() {
    if (currentContext.invokedSubcommand == null) {
      // do code here for executing `./name`
    }
  }
}
```

With **ai**, the `run` body is only called if the command's context is referring to `name`, not with the parent of the command:

```kt
object SomeCommand: AiCOmmand(
  "name', "owo", "uwu",
  help = "This is a command!"
) {
  private val user by argument(
    "user",
    help = "The user's name to register.",
    envVar = "SOME_APP_USERNAME"
  )
  
  // Enables the use of stdin, where the command is executed as:
  // echo "username" | ./owo
  //
  // The `user` argument will be populated if `echo "username" | ./owo` was
  // used, or the argument will be required.
  private val useStdin by stdin(for = "user")
  
  override fun run(ctx: AiContext) {
    println("user to register: $user (used stdin = $useStdin)")
  }
}
```

## Installation
#### Kotlin DSL
```kotlin
repositories {
    // If you're using the Noel Gradle Utils package, you can use the
    // `noelware` extension
    maven {
        url = uri("https://maven.noelware.org")
    }
}

dependencies {
    // If you're using the Noel Gradle Utils package, you can use
    // the `noelware` extension to automatically prefix `org.noelware.<module>`
    // in the dependency declaration
    implementation("org.noelware.ai:ai-<module_name>:<version>")
}
```

### Groovy DSL
```groovy
repositories {
    maven {
        url "https://maven.noelware.org"
    }
}

dependencies {
    implementation "org.noelware.ai:ai:<version>"
}
```

### Maven
Declare the **Noelware** Maven repository under the `<repositories>` chain:

```xml
<repositories>
    <repository>
        <id>noelware-maven</id>
        <url>https://maven.noelware.org</url>
    </repository>
</repositories>
```

Now declare the dependency you want under the `<dependencies>` chain:

```xml
<dependencies>
    <dependency>
        <groupId>org.noelware.cli</groupId>
        <artifactId>ai</artifactId>
        <version>{{VERSION}}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

## License
**ai (愛)** is released under the [**MIT License**](./LICENSE) with love 💜 by **Noelware** (´∀｀)♡
