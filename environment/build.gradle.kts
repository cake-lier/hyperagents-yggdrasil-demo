import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

group = "org.hyperagents.demo"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("libs/yggdrasil.jar"))
}

application {
    mainClass = "io.vertx.core.Launcher"
}

val mainVerticleName = "org.hyperagents.yggdrasil.MainVerticle"

tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Verticle" to mainVerticleName))
        }
        mergeServiceFiles {
            include("META-INF/services/io.vertx.core.spi.VerticleFactory")
        }
        archiveFileName = "app.jar"
    }

    named<JavaExec>("run") {
        args = mutableListOf("run", mainVerticleName, "--launcher-class=${application.mainClass.get()}")
    }

    compileJava {
        options.compilerArgs.addAll(listOf("-parameters"))
    }
}
