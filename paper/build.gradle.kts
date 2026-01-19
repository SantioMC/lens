repositories {
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.viaversion.com")
}

dependencies {
    implementation(project(":api"))
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.1")
    compileOnly("com.viaversion:viaversion-api:5.7.1")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT") {
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }
}