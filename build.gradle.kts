/*
 * 🪁 ai: Simple CLI parser for Kotlin that won't make your head spin. 。.:☆*:･'(*⌒―⌒*)))
 * Copyright (c) 2022 Noelware
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import dev.floofy.utils.gradle.Version
import dev.floofy.utils.gradle.by
import dev.floofy.utils.gradle.noel
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.util.Properties
import java.io.StringReader

buildscript {
    repositories {
        maven("https://maven.floofy.dev/repo/releases")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.6.1")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.6.21")
        classpath(kotlin("gradle-plugin", version = "1.6.21"))
        classpath(kotlin("serialization", version = "1.6.21"))
        classpath("dev.floofy.commons:gradle:2.1.0.1")
    }
}

plugins {
    `maven-publish`
    `java-library`

    id("com.diffplug.spotless") version "6.6.1"
    id("org.jetbrains.dokka") version "1.6.21"
    kotlin("jvm") version "1.6.21"
}

val VERSION = Version(1, 0, 0, 0, dev.floofy.utils.gradle.ReleaseType.Beta)
val DOKKA_OUTPUT = "${rootProject.projectDir}/docs"
val JAVA_VERSION = JavaVersion.VERSION_17

group = "org.noelware.ai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    noel()
}

dependencies {
    testImplementation(kotlin("test"))
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }
}

java {
    targetCompatibility = JAVA_VERSION
    sourceCompatibility = JAVA_VERSION
}

tasks {
    wrapper {
        version = "7.4.2"
        distributionType = ALL
    }

    clean {
        delete(DOKKA_OUTPUT)
    }

    test {
        useJUnitPlatform()
    }

    dokkaHtml {
        dokkaSourceSets {
            configureEach {
                platform by org.jetbrains.dokka.Platform.jvm
                jdkVersion by 17

                sourceLink {
                    remoteLineSuffix by "#L"
                    localDirectory by file("src/main/kotlin")
                    remoteUrl by uri("https://github.com/Noelware/ai/tree/master/src/main/kotlin").toURL()
                }
            }
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Version" to "$VERSION",
                    "Implementation-Vendor" to "Noelware, Inc. <team@noelware.org>"
                )
            )
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        kotlinOptions.javaParameters = true
    }
}

// Get the `publishing.properties` file from the `gradle/` directory
// in the root project.
val publishingPropsFile = file("${rootProject.projectDir}/gradle/publishing.properties")
val publishingProps = Properties()

// If the file exists, let's get the input stream
// and load it.
if (publishingPropsFile.exists()) {
    publishingProps.load(publishingPropsFile.inputStream())
} else {
    // Check if we do in environment variables
    val accessKey = System.getenv("NOELWARE_PUBLISHING_ACCESS_KEY") ?: ""
    val secretKey = System.getenv("NOELWARE_PUBLISHING_SECRET_KEY") ?: ""

    if (accessKey.isNotEmpty() && secretKey.isNotEmpty()) {
        val data = """
        |s3.accessKey=$accessKey
        |s3.secretKey=$secretKey
        """.trimMargin()

        publishingProps.load(StringReader(data))
    }
}

// Check if we have the `NOELWARE_PUBLISHING_ACCESS_KEY` and `NOELWARE_PUBLISHING_SECRET_KEY` environment
// variables, and if we do, set it in the publishing.properties loader.
val snapshotRelease: Boolean = run {
    val env = System.getenv("NOELWARE_PUBLISHING_IS_SNAPSHOT") ?: "false"
    env == "true"
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble Kotlin documentation with Dokka"

    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
}

publishing {
    publications {
        create<MavenPublication>("ai") {
            from(components["kotlin"])

            artifactId = "ai"
            groupId = "org.noelware.ai"
            version = "$VERSION"

            artifact(sourcesJar.get())
            artifact(dokkaJar.get())

            pom {
                description by "Simple CLI parser for Kotlin that won't make your head spin. 。.:☆*:･'(*⌒―⌒*)))"
                name by "ai"
                url by "https://docs.noelware.org/libs/ai"

                organization {
                    name by "Noelware"
                    url by "https://noelware.org"
                }

                developers {
                    developer {
                        name by "Noel"
                        email by "cutie@floofy.dev"
                        url by "https://floofy.dev"
                    }

                    developer {
                        name by "Noelware Team"
                        email by "team@noelware.org"
                        url by "https://noelware.org"
                    }
                }

                issueManagement {
                    system by "GitHub"
                    url by "https://github.com/Noelware/ai/issues"
                }

                licenses {
                    license {
                        name by "Apache-2.0"
                        url by "http://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                scm {
                    connection by "scm:git:ssh://github.com/Noelware/remi.git"
                    developerConnection by "scm:git:ssh://git@github.com:Noelware/remi.git"
                    url by "https://github.com/Noelware/ai"
                }
            }
        }
    }

    repositories {
        val url = if (snapshotRelease) "s3://maven.noelware.org/snapshots" else "s3://maven.noelware.org"
        maven(url) {
            credentials(AwsCredentials::class.java) {
                accessKey = publishingProps.getProperty("s3.accessKey") ?: System.getenv("NOELWARE_PUBLISHING_ACCESS_KEY") ?: ""
                secretKey = publishingProps.getProperty("s3.secretKey") ?: System.getenv("NOELWARE_PUBLISHING_SECRET_KEY") ?: ""
            }
        }
    }
}
