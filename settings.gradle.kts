// Path: zn-vault-sdk-jvm/settings.gradle.kts
rootProject.name = "zn-vault-sdk-jvm"

include("zn-vault-core")
include("zn-vault-coroutines")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.22")
            version("okhttp", "4.12.0")
            version("jackson", "2.17.0")
            version("coroutines", "1.8.0")
            version("slf4j", "2.0.12")

            library("okhttp", "com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            library("okhttp-logging", "com.squareup.okhttp3", "logging-interceptor").versionRef("okhttp")
            library("jackson-core", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-jsr310", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")
            library("coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("coroutines-jdk8", "org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8").versionRef("coroutines")
            library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")

            // Test dependencies
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:5.10.2")
            library("junit-platform-launcher", "org.junit.platform:junit-platform-launcher:1.10.2")
            library("mockk", "io.mockk:mockk:1.13.10")
            library("okhttp-mockwebserver", "com.squareup.okhttp3", "mockwebserver").versionRef("okhttp")
            library("coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines")
        }
    }
}
