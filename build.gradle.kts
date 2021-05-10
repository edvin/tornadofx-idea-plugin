import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishTask

plugins {
    idea
    java
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.intellij") version "0.7.3"
}

group = "no.tornado"
version = "1.7.20.1"

val publishUsername: String by rootProject.extra
val publishPassword: String by rootProject.extra

repositories {
    mavenCentral()
}

intellij {
    version = "2021.1.1"
    //updateSinceUntilBuild = false
    setPlugins("java", "properties", "Kotlin", "com.intellij.javafx:1.0.3")
}

tasks {
    patchPluginXml {
         version(project.version)
         sinceBuild("203")
         untilBuild("")
     }

     publishPlugin {
        username(publishUsername)
        password(publishPassword)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    runIde {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }

    buildSearchableOptions {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

