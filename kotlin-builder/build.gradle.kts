import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)
    `java-library`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {
    implementation(JacksonDatatype.moduleKotlin)
    implementation(JacksonDatatype.datatypeJsr310)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JunitJupiter.api)
    testImplementation(JunitJupiter.engine)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
