import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishTask

plugins {
    idea
    java
    kotlin("jvm") version "1.4.30"
    id("org.jetbrains.intellij") version "0.7.2"
}

group = "no.tornado"
version = "1.7.20-1-dev"

val publishUsername: String by rootProject.extra
val publishPassword: String by rootProject.extra

repositories {
    mavenCentral()
}

intellij {
    version = "2020.3.1"
    //updateSinceUntilBuild = false
    setPlugins("java", "properties", "Kotlin")
}

tasks {
    // withType<PatchPluginXmlTask> { }

    withType<PublishTask> {
        username(publishUsername)
        password(publishPassword)
    }

    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

