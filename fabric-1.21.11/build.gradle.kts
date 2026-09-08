plugins {
    id("fabric-loom") version "1.17.20"
}

// Всё, что привязано к версии игры, стоит здесь и больше нигде. Модуль под новую версию — это копия
// каталога с другими числами в этих двух строках плюс правка того, что в ней переименовали.
val minecraftVersion = "1.21.11"
val fabricApiVersion = "0.141.6+1.21.11"
val modMenuVersion = "17.0.1-beta.1"

base {
    archivesName.set("alphamap-$minecraftVersion")
}

// Ассеты, не зависящие от версии игры, лежат один раз в корне: модулю под новую версию они
// достанутся этой же строкой, а не копией каталога.
sourceSets.main {
    resources.srcDir(rootProject.file("assets"))
}

repositories {
    maven("https://maven.terraformersmc.com/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    // Мапинги Mojang, а не yarn: имена тогда совпадают с серверной стороной (Paper/Leaf), и
    // контракт канала читается одинаково по обе стороны.
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Только ради экрана настроек. Мод работает и без ModMenu — точка входа просто не позовётся.
    modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    // В dev-клиенте ModMenu стоит, чтобы экран настроек можно было открыть и проверить.
    modLocalRuntime("com.terraformersmc:modmenu:$modMenuVersion")

    implementation(project(":protocol"))
    // Протокол не мод, а обычная библиотека, поэтому едет внутрь джарника вложенным архивом.
    include(project(":protocol"))
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("minecraft_version", minecraftVersion)
    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "minecraft_version" to minecraftVersion,
        )
    }
}
