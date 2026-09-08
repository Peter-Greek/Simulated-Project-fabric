pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "simulated-project-fabric-homestead"

// Keep the first milestone intentionally small: compile Simulated against the
// exact Fabric/Create stack shipped by Homestead before porting Aeronautics
// and Offroad on top of it.
include("simulated:fabric")
