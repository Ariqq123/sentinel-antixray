plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.azreyzaako"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("io.netty:netty-transport:4.2.7.Final")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("SentinelAntiXray")
    }

    reobfJar {
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
}
