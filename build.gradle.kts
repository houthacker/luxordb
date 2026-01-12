plugins {
    id("java-platform")
    id("pmd")
    id("checkstyle")
    id("jacoco")
    id("com.github.spotbugs") version("6.4.8") apply(false)
    id("com.diffplug.spotless") version("8.1.0") apply(false)
}

group = "nl.hh"
version = "0.0.1-dev"

repositories {
    mavenCentral()
}