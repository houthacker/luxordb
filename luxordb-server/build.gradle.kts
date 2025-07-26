plugins {
    id("java-library")
    id("pmd")
    id("checkstyle")
    id("jacoco")
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
}

group = "nl.hh"
version = project.version

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations")

    implementation("org.slf4j:slf4j-api")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.slf4j:slf4j-simple")
}

tasks {

    checkstyle {
        toolVersion = "10.26.1"
    }

    checkstyleTest {
        onlyIf { false }
    }

    jacoco {
        toolVersion = "0.8.13"
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required = false
            csv.required = false
        }
    }

    pmd {
        isConsoleOutput = true
        toolVersion = "7.15.0"
        threads = 1
        ruleSetFiles = rootProject.files("config/pmd/pmd.xml")
        ruleSets = listOf()
        incrementalAnalysis = true
    }

    pmdTest {
        onlyIf { false }
    }

    spotbugsMain {
        reports.create("html") {
            required = true
            outputLocation = layout.buildDirectory.file("reports/spotbugs.html")
            setStylesheet("fancy-hist.xsl")
        }
    }

    spotbugsTest {
        onlyIf { false }
    }

    spotless {
        java {
            googleJavaFormat()
        }
    }

    test {
        useJUnitPlatform()
        maxHeapSize = "1024m"
        finalizedBy(jacocoTestReport)
    }
}
