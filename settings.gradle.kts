import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}

val localProperties by lazy {
    val localProperties = Properties()
    val localPropertiesFile = File("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    localProperties
}

fun MavenArtifactRepository.eamMavenProperties() {
    credentials {
        username = System.getenv("GITHUB_USER") ?: localProperties.getProperty("GITHUB_USER")
        password = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("GITHUB_TOKEN")
    }

    content {
        includeGroup("com.tarkalabs")
//        includeGroup("com.eam360")
        excludeModule("com.tarkalabs", "jobqueue")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven(url = "https://maven.pkg.github.com/tarkalabs/eam360-common-library-android") {
            eamMavenProperties()
        }
        maven(url = "https://maven.pkg.github.com/tarkalabs/eam360-ui-android") {
            eamMavenProperties()
        }
    }
}

rootProject.name = "Zebra Printer - KCH"
include(":app")
 