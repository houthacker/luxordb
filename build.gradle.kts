plugins {
    id("java-library")
    id("pmd")
    id("checkstyle")
    id("jacoco")
    id("io.spring.dependency-management") version("1.1.7")
    id("com.github.spotbugs") version("6.2.2") apply(false)
    id("com.diffplug.spotless") version("7.2.1") apply(false)
}

group = "nl.hh"
version = "0.0.1-dev"

repositories {
    mavenCentral()
}

dependencyManagement {
    dependencies {
        dependency("com.github.spotbugs:spotbugs-annotations:4.9.3")
        //dependency("org.junit:junit-bom:5.10.0")
        dependency("org.slf4j:slf4j-api:2.0.17")
        dependency("org.slf4j:slf4j-simple:2.0.17")
    }
}