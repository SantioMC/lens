plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
}

allprojects {
    apply(plugin = "java-library")

    group = "me.santio"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.42")
        annotationProcessor("org.projectlombok:lombok:1.18.42")
        annotationProcessor("com.google.auto.service:auto-service:1.0-rc5")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}

subprojects {
    apply(plugin = "com.gradleup.shadow")

    tasks.build {
        dependsOn(tasks.shadowJar)
    }

    tasks.shadowJar {
        archiveFileName.set("lens-${this@subprojects.name}.jar")
        mergeServiceFiles()
        minimize()
    }
}