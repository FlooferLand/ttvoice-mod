import org.gradle.kotlin.dsl.property

plugins {
    alias(libs.plugins.fabricLoom)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinJvm)
}

val modVersion = "${project.property("mod_version")}+${libs.versions.minecraft.get()}"

version = modVersion
group = project.property("maven_group") as String
val javaVersionInt = libs.versions.java.get().toInt()

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()

    // Simple Voice Chat
    maven { url = uri("https://maven.maxhenkel.de/releases") }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }

    // Mod Menu
    maven { url = uri("https://maven.shedaniel.me/") }
    maven { url = uri("https://maven.terraformersmc.com/releases/") }

    // Figura
    maven { url = uri("https://maven.quiltmc.org/repository/release/") }
    maven {
        name = "Figura Maven Release"
        url = uri("https://maven.figuramc.org/releases")
    }
    maven {
        name = "Figura Maven Snapshots"
        url = uri("https://maven.figuramc.org/snapshots")
    }
    maven { url = uri("https://jitpack.io") }

    // Dev Auth
    maven { url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("ttvoice") {
            sourceSet(sourceSets["client"])
        }
    }
}

// I have to do this because Java and the jar format are so ancient they can't even list the files a jar contains
val nativesIndexDir = file("$buildDir/generated/resources")
val nativesIndexFile = nativesIndexDir.resolve("_natives_index.txt")
tasks.register("generateNativesIndex") {
    val resourceDir = file("src/client/resources/native")
    inputs.dir(resourceDir)
    outputs.file(nativesIndexFile)

    doLast {
        nativesIndexFile.parentFile.mkdirs()
        val files = fileTree(resourceDir).matching { include("**/*") }.files
        nativesIndexFile.writeText(files.joinToString("\n") {
            resourceDir.toPath().relativize(it.toPath()).toString().replace('\\', '/')
        })
    }
}

tasks.processResources {
    dependsOn("generateNativesIndex")
    from(nativesIndexDir) {
        into("")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    mappings("net.fabricmc:yarn:${libs.versions.yarn.get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${libs.versions.loader.get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${libs.versions.fabric.get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${libs.versions.fabricKotlin.get()}+kotlin.${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")

    // Mod dependencies
    modImplementation("de.maxhenkel.voicechat:voicechat-api:${libs.versions.voicechatApi.get()}")
    modImplementation("maven.modrinth:simple-voice-chat:fabric-${libs.versions.minecraft.get()}-${libs.versions.voicechatMod.get()}")

    // Config
    implementation("com.moandjiezana.toml:toml4j:${libs.versions.toml4j.get()}")
    include("com.moandjiezana.toml:toml4j:${libs.versions.toml4j.get()}")
    modApi("com.terraformersmc:modmenu:${libs.versions.modMenu.get()}")

    // Figura integration
    compileOnly("com.github.FiguraMC.luaj:luaj-core:${libs.versions.luaj.get()}-figura")
    compileOnly("com.github.FiguraMC.luaj:luaj-jse:${libs.versions.luaj.get()}-figura")
    compileOnly("com.neovisionaries:nv-websocket-client:${libs.versions.nvWebsocket.get()}")
    compileOnly("org.figuramc:figura-fabric:${libs.versions.figura.get()}+${libs.versions.minecraft.get()}")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:${libs.versions.mixinExtras.get()}")
    include("io.github.llamalad7:mixinextras-fabric:${libs.versions.mixinExtras.get()}")

    // Outside dependencies
    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${libs.versions.devAuth.get()}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.java.dev.jna:jna-platform:${libs.versions.jna.get()}")

    // Testing
    testImplementation("net.fabricmc:fabric-loader-junit:${libs.versions.loader.get()}")
    testImplementation("io.kotest:kotest-runner-junit5:${libs.versions.junit.get()}")
}

tasks.named<ProcessResources>("processClientResources") {
    exclude("**/*.lnk")

    val properties = mapOf(
        "version" to version,
        "minecraft_version" to libs.versions.minecraft.get(),
        "loader_version" to libs.versions.loader.get(),
        "fabric_version" to libs.versions.fabric.get(),
        "java_version" to javaVersionInt.toString(),
        "kotlin_version" to libs.versions.kotlin.get(),

        // Mod dependencies
        "mod_menu_version" to libs.versions.modMenu.get(),
        "voicechat_mod_version" to libs.versions.voicechatMod.get(),
        "fabric_kotlin_version" to libs.versions.fabricKotlin.get(),
        "figura_version" to libs.versions.figura.get()
    )
    properties.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
    filesMatching("${base.archivesName.get()}.client.mixins.json") {
        expand(
            mapOf(
                "archivesName" to base.archivesName.get(),
                "archivesBaseName" to base.archivesName.get()
            )
        )
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersionInt)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = javaVersionInt.toString()
}

java {
    withSourcesJar()
}

tasks.jar {
    inputs.property("archivesName", base.archivesName.get())

    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
    from("LICENSE-EX") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

tasks.test {
    useJUnitPlatform()
}
