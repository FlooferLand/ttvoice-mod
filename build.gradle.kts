val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5"))
    JavaVersion.VERSION_21 else JavaVersion.VERSION_17
val kotlinVersion = "2.2.20"
val loader = "fabric"

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-2.0.2"
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.11-SNAPSHOT"
}

val minecraft = stonecutter.current.version
group = "com.flooferland"
version = "${property("mod.version")}"
base {
    archivesName.set("${property("mod.id")}-$minecraft")
}

evaluationDependsOnChildren()

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
    runConfigs.all {
        ideConfigGenerated(true) // Run configurations are not created for subprojects by default
        runDir = "../../run" // Shared run folder between versions
    }
}

// I have to do this because Java and the jar format are so ancient they can't even list the files a jar contains
val nativesIndexDir = file("${layout.buildDirectory}/generated/resources")
val nativesIndexFile = nativesIndexDir.resolve("_natives_index.txt")
tasks.register("generateNativesIndex") {
    val resourceDir = file("../../src/client/resources/native")
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

fun dep(name: String) = property("deps.${name}")
dependencies {
    mappings("net.fabricmc:yarn:${dep("yarn_mappings")}:v2")
    minecraft("com.mojang:minecraft:${minecraft}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    if (loader == "fabric") {
        if (dep("fabric_language_kotlin").toString().split("+")[1] != "kotlin.$kotlinVersion") {
            error("Fabric Language Kotlin and Kotlin version do not match up")
        }
        modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")
        modImplementation("net.fabricmc:fabric-language-kotlin:${dep("fabric_language_kotlin")}")
    }

    // Config
    implementation("com.moandjiezana.toml:toml4j:${dep("toml4j")}")
    include("com.moandjiezana.toml:toml4j:${dep("toml4j")}")
    modApi("com.terraformersmc:modmenu:${dep("mod_menu")}")

    // Simple voice chat
    modImplementation("de.maxhenkel.voicechat:voicechat-api:${dep("simple_voice_chat_api")}")
    modImplementation("maven.modrinth:simple-voice-chat:$loader-${minecraft}-${dep("simple_voice_chat")}")
            
    // Figura integration
    compileOnly("org.figuramc:figura-fabric:${dep("figura")}+${minecraft}")
    compileOnly("com.github.FiguraMC.luaj:luaj-core:${dep("luaj")}")
    compileOnly("com.github.FiguraMC.luaj:luaj-jse:${dep("luaj")}")
    compileOnly("com.neovisionaries:nv-websocket-client:${dep("nv_websocket")}")
    annotationProcessor("io.github.llamalad7:mixinextras-$loader:${dep("mixin_extras")}")
    include("io.github.llamalad7:mixinextras-$loader:${dep("mixin_extras")}")

    // Outside dependencies
    modRuntimeOnly("me.djtheredstoner:DevAuth-$loader:${dep("dev_auth")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.java.dev.jna:jna-platform:${dep("jna")}")

    // Testing
    testImplementation("net.fabricmc:fabric-loader-junit:${dep("fabric_loader")}")
    testImplementation("io.kotest:kotest-runner-junit5:${dep("junit")}")
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.WARN

    val fabricLanguageKotlin = "${dep("fabric_language_kotlin")}+kotlin.$kotlinVersion"
    val voiceChatMod = "${minecraft}-${dep("simple_voice_chat")}"
    val properties = mapOf(
        "minecraft" to minecraft,
        "version" to version as String,
        "java" to java.toString(),
        "kotlin" to kotlinVersion,
        "fabric_loader" to dep("fabric_loader") as String,
        "fabric_language_kotlin" to fabricLanguageKotlin,
        "voicechat_mod" to voiceChatMod,
        "mod_menu" to dep("mod_menu") as String,
        "figura" to dep("figura") as String,
        "archivesName" to base.archivesName.get(),
        "archivesBaseName" to base.archivesName.get()
    )
    properties.forEach() { (k, v) ->
        inputs.property(k, v)
    }

    exclude("**/*.lnk")
    filesMatching("fabric.mod.json") {
        expand(properties)
    }
    filesMatching("${base.archivesName.get()}.client.mixins.json") {
        expand(properties)
    }
}

java {
    withSourcesJar()
    targetCompatibility = java
    sourceCompatibility = java
}

kotlin {
    jvmToolchain(java.ordinal + 1)
}

tasks.jar {
    inputs.property("archivesName", base.archivesName.get())

    from("LICENSE") {
        rename { "${it}_${base.archivesName}" }
    }
    from("LICENSE-EX") {
        rename { "${it}_${base.archivesName}" }
    }
}

tasks.test {
    useJUnitPlatform()
}
