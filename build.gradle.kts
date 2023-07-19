import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.text.Regex

/* the name of this project, default is the template version but you are free to change these */
group = "org.openrndr.template"
version = "0.3.13"

val applicationMainClass = "TemplateProgramKt"

/*  Which additional (ORX) libraries should be added to this project. */
val orxFeatures = setOf(
//  "orx-boofcv",
//  "orx-camera",
//  "orx-chataigne",
  "orx-color",
  "orx-compositor",
//  "orx-dnk3",
//  "orx-easing",
//  "orx-file-watcher",
//  "orx-parameters",
  "orx-filter-extension",
  "orx-fx",
//  "orx-glslify",
//  "orx-gradient-descent",
  "orx-gui",
  "orx-image-fit",
//  "orx-integral-image",
//  "orx-interval-tree",
//  "orx-jumpflood",
//  "orx-kdtree",
//  "orx-mesh-generators",
//  "orx-midi",
  "orx-no-clear",
  "orx-noise",
//  "orx-obj-loader",
  "orx-olive",
//  "orx-osc",
//  "orx-palette",
//  "orx-poisson-fill",
//  "orx-rabbit-control,
//  "orx-runway",
  "orx-shade-styles",
//  "orx-shader-phrases",
//  "orx-shapes",
//  "orx-syphon",
//  "orx-temporal-blur",
//  "orx-time-operators",
//  "orx-kinect-v1",

  "orx-panel"
)

/* Which OPENRNDR libraries should be added to this project? */
val openrndrFeatures = setOf(
  "video"
)

/*  Which version of OPENRNDR and ORX should be used? */
val openrndrUseSnapshot = false
val openrndrVersion = if (openrndrUseSnapshot) "0.4.0-SNAPSHOT" else "0.3.47"

val orxUseSnapshot = false
val orxVersion = if (orxUseSnapshot) "0.4.0-SNAPSHOT" else "0.3.57"

// <editor-fold desc="This is code for OPENRNDR, no need to edit this .. most of the times">
val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")

val openrndrOs = if (project.hasProperty("targetPlatform")) {
  val platform: String = project.property("targetPlatform") as String
  if (platform !in supportedPlatforms) {
    throw IllegalArgumentException("target platform not supported: $platform")
  } else {
    platform
  }
} else when (OperatingSystem.current()) {
  OperatingSystem.WINDOWS -> "windows"
  OperatingSystem.MAC_OS -> "macos"
  OperatingSystem.LINUX -> when (val h = DefaultNativePlatform("current").architecture.name) {
    "x86-64" -> "linux-x64"
    "aarch64" -> "linux-arm64"
    else -> throw IllegalArgumentException("architecture not supported: $h")
  }
  else -> throw IllegalArgumentException("os not supported")
}
// </editor-fold>

enum class Logging {
  NONE,
  SIMPLE,
  FULL
}

/*  What type of logging should this project use? */
val applicationLogging = Logging.FULL

val kotlinVersion = "1.4.20"

plugins {
  java
  kotlin("jvm") version("1.4.20")
  id("com.github.johnrengelman.shadow") version ("6.0.0")
  id("org.beryx.runtime") version ("1.9.1")
  /* Linting and auto-formatting! 🙌 */
  id("org.jlleitschuh.gradle.ktlint") version ("9.3.0")
}

/* * * * * * * * * * * * * * * * * * * *
 Required for my unusual project structure, nothing to do with the openrndr project
 * * * * * * * * * * * * * * * * * * * */
kotlin {
  sourceSets["main"].apply {
    kotlin.srcDir("openrndr/src/main/kotlin")
  }
}

repositories {
  mavenCentral()
  if (openrndrUseSnapshot || orxUseSnapshot) {
    mavenLocal()
  }
  maven(url = "https://dl.bintray.com/openrndr/openrndr")
}

fun DependencyHandler.orx(module: String): Any {
  return "org.openrndr.extra:$module:$orxVersion"
}

fun DependencyHandler.openrndr(module: String): Any {
  return "org.openrndr:openrndr-$module:$openrndrVersion"
}

fun DependencyHandler.openrndrNatives(module: String): Any {
  return "org.openrndr:openrndr-$module-natives-$openrndrOs:$openrndrVersion"
}

fun DependencyHandler.orxNatives(module: String): Any {
  return "org.openrndr.extra:$module-natives-$openrndrOs:$orxVersion"
}

dependencies {
  /*  This is where you add additional (third-party) dependencies */

//    implementation("org.jsoup:jsoup:1.12.2")
//    implementation("com.google.code.gson:gson:2.8.6")

  runtimeOnly(openrndr("gl3"))
  runtimeOnly(openrndrNatives("gl3"))
  implementation(openrndr("openal"))
  runtimeOnly(openrndrNatives("openal"))
  implementation(openrndr("core"))
  implementation(openrndr("svg"))
  implementation(openrndr("animatable"))
  implementation(openrndr("extensions"))
  implementation(openrndr("filter"))

  implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.7")
  implementation("io.github.microutils", "kotlin-logging", "1.7.10")

  when (applicationLogging) {
    Logging.NONE -> {
      runtimeOnly("org.slf4j", "slf4j-nop", "1.7.30")
    }
    Logging.SIMPLE -> {
      runtimeOnly("org.slf4j", "slf4j-simple", "1.7.30")
    }
    Logging.FULL -> {
      runtimeOnly("org.slf4j", "slf4j-simple", "1.7.30") // started throwing an error so I guess I need it?
      runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.3")
      runtimeOnly("com.fasterxml.jackson.core", "jackson-databind", "2.11.1")
      runtimeOnly("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.11.1")
    }
  }

  if ("video" in openrndrFeatures) {
    implementation(openrndr("ffmpeg"))
    runtimeOnly(openrndrNatives("ffmpeg"))
  }

  for (feature in orxFeatures) {
    implementation(orx(feature))
  }

  if ("orx-kinect-v1" in orxFeatures) {
    runtimeOnly(orxNatives("orx-kinect-v1"))
  }

  if ("orx-olive" in orxFeatures) {
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
  }

  implementation(kotlin("stdlib-jdk8"))
  testImplementation("junit", "junit", "4.12")
}

// --------------------------------------------------------------------------------------------------------------------

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

project.setProperty("mainClassName", applicationMainClass)
tasks {
  named<ShadowJar>("shadowJar") {
    manifest {
      attributes["Main-Class"] = applicationMainClass
    }
    minimize {
      exclude(dependency("org.openrndr:openrndr-gl3:.*"))
      exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
    }
  }
  named<org.beryx.runtime.JPackageTask>("jpackage") {
    doLast {
      when (OperatingSystem.current()) {
        OperatingSystem.WINDOWS, OperatingSystem.LINUX -> {
          copy {
            from("data") {
              include("**/*")
            }
            into("build/jpackage/openrndr-application/data")
          }
        }
        OperatingSystem.MAC_OS -> {
          copy {
            from("data") {
              include("**/*")
            }
            into("build/jpackage/openrndr-application.app/data")
          }
        }
      }
    }
  }
}

tasks.register<Zip>("jpackageZip") {
  archiveFileName.set("openrndr-application-$openrndrOs.zip")
  from("$buildDir/jpackage") {
    include("**/*")
  }
}
tasks.findByName("jpackageZip")?.dependsOn("jpackage")

runtime {
  jpackage {
    imageName = "openrndr-application"
    skipInstaller = true
    if (OperatingSystem.current() == OperatingSystem.MAC_OS) {
      jvmArgs.add("-XstartOnFirstThread")
    }
  }
  options.empty()
  options.add("--strip-debug")
  options.add("--compress")
  options.add("1")
  options.add("--no-header-files")
  options.add("--no-man-pages")
  modules.empty()
  modules.add("jdk.unsupported")
  modules.add("java.management")
}

/* Woo! Run a Kotlin program from command line! 🙌
* Run with:
*   ./gradlew sketch -Ptitle=MySketch
* */
val title: String by project
task("sketch", JavaExec::class) {
  classpath = sourceSets["main"].runtimeClasspath
  if (project.hasProperty("title")) {
    var programMain = title
    // this is to make the simple script in run.json work
    programMain = programMain.replace(Regex(".kt$"), "")
    programMain = programMain.split(Regex("/")).last()
    println("programMain = $programMain")

    if (!Regex("Kt$").containsMatchIn(title)) {
      programMain = programMain + "Kt"
    }
    main = "sketch." + programMain
  } else {
    println(
      "Please pass title parameter like\n" +
        "./gradlew sketch -Ptitle=MySketch"
    )
  }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  languageVersion = "1.4"
}
