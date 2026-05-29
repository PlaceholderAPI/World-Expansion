plugins {
    java
}

group = "at.helpch"
version = "1.0.3"

repositories {
    mavenCentral()

    maven("https://maven.hytale.com/release/")
    maven("https://repo.helpch.at/releases")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:0.5.2")
    compileOnly("at.helpch:placeholderapi-hytale:1.0.8")
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