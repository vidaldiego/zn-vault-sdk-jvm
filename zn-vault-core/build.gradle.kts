// Path: zn-vault-sdk-jvm/zn-vault-core/build.gradle.kts
plugins {
    kotlin("jvm")
}

dependencies {
    // HTTP client
    api(libs.okhttp)
    implementation(libs.okhttp.logging)

    // JSON serialization
    api(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)

    // Logging
    api(libs.slf4j.api)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.12")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to "ZnVault SDK Core",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "ZincWare"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
