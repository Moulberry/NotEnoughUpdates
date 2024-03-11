	/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */


import neubs.NEUBuildFlags
import neubs.applyPublishingInformation
import neubs.setVersionFromEnvironment

plugins {
		idea
		java
		id("gg.essential.loom") version "0.10.0.+"
		id("dev.architectury.architectury-pack200") version "0.1.3"
		id("com.github.johnrengelman.shadow") version "7.1.2"
		id("io.github.juuxel.loom-quiltflower") version "1.7.3"
		`maven-publish`
}


apply<NEUBuildFlags>()

// Build metadata

group = "io.github.moulberry"

setVersionFromEnvironment("2.1")

// Minecraft configuration:
loom {
		launchConfigs {
				"client" {
						property("mixin.debug", "true")
						property("asmhelper.verbose", "true")
						arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
						arg("--mixin", "mixins.notenoughupdates.json")
				}
		}
		runConfigs {
				"server" {
						isIdeConfigGenerated = false
				}
		}
		forge {
				pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
				mixinConfig("mixins.notenoughupdates.json")
		}
		mixin {
				defaultRefmapName.set("mixins.notenoughupdates.refmap.json")
		}
}


// Dependencies:
repositories {
		mavenCentral()
		mavenLocal()
		maven("https://repo.spongepowered.org/maven/")
		maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
		maven("https://jitpack.io")
}

val shadowImplementation by configurations.creating {
		configurations.implementation.get().extendsFrom(this)
}

val shadowApi by configurations.creating {
		configurations.implementation.get().extendsFrom(this)
}

val devEnv by configurations.creating {
		configurations.runtimeClasspath.get().extendsFrom(this)
		isCanBeResolved = false
		isCanBeConsumed = false
		isVisible = false
}

dependencies {
		minecraft("com.mojang:minecraft:1.8.9")
		mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
		forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

		shadowImplementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
				isTransitive = false // Dependencies of mixin are already bundled by minecraft
		}
		annotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")
		shadowApi("info.bliki.wiki:bliki-core:3.1.0")
		testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
		testAnnotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")
		//	modImplementation("io.github.notenoughupdates:MoulConfig:0.0.1")

		devEnv("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")
}



java {
		withSourcesJar()
		toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Tasks:

tasks.withType(JavaCompile::class) {
		options.encoding = "UTF-8"
}

tasks.named<Test>("test") {
		useJUnitPlatform()
}

tasks.withType(Jar::class) {
		archiveBaseName.set("NotEnoughUpdates")
		manifest.attributes.run {
				this["Main-Class"] = "NotSkyblockAddonsInstallerFrame"
				this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
				this["MixinConfigs"] = "mixins.notenoughupdates.json"
				this["FMLCorePluginContainsFMLMod"] = "true"
				this["ForceLoadAsMod"] = "true"
				this["Manifest-Version"] = "1.0"
		}
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
		archiveClassifier.set("dep")
		from(tasks.shadowJar)
		input.set(tasks.shadowJar.get().archiveFile)
		doLast {
				println("Jar name: ${archiveFile.get().asFile}")
		}
}

tasks.shadowJar {
		archiveClassifier.set("dep-dev")
		configurations = listOf(shadowImplementation, shadowApi)
		exclude("**/module-info.class", "LICENSE.txt")
		dependencies {
				exclude {
						it.moduleGroup.startsWith("org.apache.") || it.moduleName in
										listOf("logback-classic", "commons-logging", "commons-codec", "logback-core")
				}
		}
		fun relocate(name: String) = relocate(name, "io.github.moulberry.notenoughupdates.deps.$name")
}

tasks.assemble.get().dependsOn(remapJar)

tasks.processResources {
		from(tasks["generateBuildFlags"])
		filesMatching(listOf("mcmod.info", "fabric.mod.json", "META-INF/mods.toml")) {
				expand(
						"version" to project.version, "mcversion" to "1.8.9"
				)
		}
}

sourceSets.main {
		output.setResourcesDir(file("$buildDir/classes/java/main"))
}

applyPublishingInformation(
		"deobf" to tasks.jar,
		"all" to tasks.remapJar,
		"sources" to tasks["sourcesJar"],
)
