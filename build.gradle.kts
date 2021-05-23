plugins {
  kotlin("jvm") version "1.5.0"
  `java-library-distribution`
}

version = "0.0.2"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("io.ktor:ktor-server-netty:1.5.4")
  runtimeOnly("ch.qos.logback:logback-classic:1.2.3")
}

tasks.jar {
  manifest {
    attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { "lib/" + it.name }
  }
}
