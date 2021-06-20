import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.5.0"
  `java-library-distribution`
}

version = "0.0.18"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.ktor:ktor-server-netty:1.6.0")
  implementation("io.ktor:ktor-jackson:1.6.0")

  implementation("com.orbitz.consul:consul-client:1.5.1")

  implementation("org.slf4j:slf4j-api:1.7.30")
  runtimeOnly("ch.qos.logback:logback-classic:1.2.3")
}

tasks.jar {
  manifest {
    attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { "lib/" + it.name }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = listOf(
      "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      "-Xopt-in=kotlinx.coroutines.ObsoleteCoroutinesApi"
    )
  }
}
