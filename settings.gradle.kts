pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "alphamap"

include("protocol")
include("fabric-1.21.11")
include("fabric-26.1.2")
include("fabric-26.2")
