plugins {
    java
}

group = "at.helpch"
version = "1.0.0"

repositories {
    mavenCentral()

    maven("https://repo.codemc.io/repository/hytale/")
    maven("https://repo.helpch.at/releases")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:2026.01.17-4b0f30090")
    compileOnly("at.helpch:placeholderapi-hytale:1.0.2")
}

tasks {
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        withJavadocJar()
        withSourcesJar()
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}