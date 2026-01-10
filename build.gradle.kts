// Path: zn-vault-sdk-jvm/build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("com.gradleup.nmcp.aggregation") version "1.3.0"
}

allprojects {
    group = "io.github.vidaldiego"
    version = "1.8.1"

    repositories {
        mavenCentral()
    }
}

// Configure NMCP for Maven Central Portal
nmcpAggregation {
    centralPortal {
        username = findProperty("mavenCentralUsername") as String? ?: System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
        password = findProperty("mavenCentralPassword") as String? ?: System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
        publishingType = "AUTOMATIC"
    }
    publishAllProjectsProbablyBreakingProjectIsolation()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Dokka task for javadoc jar
    tasks.named<Jar>("javadocJar") {
        from(tasks.named("dokkaHtml"))
        archiveClassifier.set("javadoc")
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Java/Kotlin client library for ZnVault secrets management")
                    url.set("https://github.com/vidaldiego/zn-vault-sdk-jvm")

                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("vidaldiego")
                            name.set("Diego Vidal")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/vidaldiego/zn-vault-sdk-jvm.git")
                        developerConnection.set("scm:git:ssh://github.com/vidaldiego/zn-vault-sdk-jvm.git")
                        url.set("https://github.com/vidaldiego/zn-vault-sdk-jvm")
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        val signingKey = findProperty("signingKey") as String? ?: System.getenv("GPG_SIGNING_KEY")
        val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("GPG_SIGNING_PASSWORD")

        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
        }
    }
}
