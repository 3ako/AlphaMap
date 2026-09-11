plugins {
    id("net.fabricmc.fabric-loom") version "1.17.20"
}

val minecraftVersion = "26.2"
val fabricApiVersion = "0.160.0+26.2"
val javaVersion = 25
val modMenuVersion = "20.0.1"

base {
    archivesName.set("alphamap-$minecraftVersion")
}

sourceSets.main {
    java.srcDir(rootProject.file("common/src/main/java"))
    java.srcDir(rootProject.file("common-26/src/main/java"))
    resources.srcDir(rootProject.file("common/src/main/resources"))
}

repositories {
    maven("https://maven.terraformersmc.com/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    localRuntime("com.terraformersmc:modmenu:$modMenuVersion")

    implementation(project(":protocol"))
    include(project(":protocol"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("java_version", javaVersion)
    filesMatching(listOf("fabric.mod.json", "alphamap.mixins.json")) {
        expand(
            "version" to version,
            "minecraft_version" to minecraftVersion,
            "java_version" to javaVersion,
        )
    }
}
