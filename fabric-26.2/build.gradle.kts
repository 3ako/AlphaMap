plugins {
    // С 26.1 игра не обфусцирована и маппингов Mojang для неё Mojang не выкладывает: берём вариант
    // плагина без ремапа. Отсюда же отсутствие mappings() и обычные зависимости вместо mod*.
    id("net.fabricmc.fabric-loom") version "1.17.20"
}

// Всё, что привязано к версии игры, стоит здесь и больше нигде. Модуль под новую версию — это этот
// файл с другими числами плюс горстка классов compat-слоя рядом (Canvas, Compat, *Hud, WaypointScreen).
val minecraftVersion = "26.2"
val fabricApiVersion = "0.160.0+26.2"
val javaVersion = 25
val modMenuVersion = "20.0.1"

base {
    archivesName.set("alphamap-$minecraftVersion")
}

// Общий код и ресурсы лежат по разу и подключаются модулями версий:
//   common    — то, что одинаково везде;
//   common-26 — рисование через GuiGraphicsExtractor и регистрации fabric-api, с 26.1.
// Своё здесь только Vanilla: с 26.2 экран и худ живут в Minecraft.gui, а не в самом Minecraft.
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

    // Только ради экрана настроек. Мод работает и без ModMenu — точка входа просто не позовётся.
    compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    // В dev-клиенте ModMenu стоит, чтобы экран настроек можно было открыть и проверить.
    localRuntime("com.terraformersmc:modmenu:$modMenuVersion")

    implementation(project(":protocol"))
    // Протокол не мод, а обычная библиотека, поэтому едет внутрь джарника вложенным архивом.
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
