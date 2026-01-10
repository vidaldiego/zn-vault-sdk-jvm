// Path: zn-vault-sdk-jvm/zn-vault-coroutines/build.gradle.kts
plugins {
    kotlin("jvm")
}

dependencies {
    // Core SDK
    api(project(":zn-vault-core"))

    // Kotlin Coroutines
    api(libs.coroutines.core)
    implementation(libs.coroutines.jdk8)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.12")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to "ZnVault SDK Coroutines",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "ZincWare"
        )
    }
}
