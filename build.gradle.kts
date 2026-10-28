plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0"
}

group = "net.aros"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "Minecraft Libraries"
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("info.picocli:picocli:4.7.7")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    archiveBaseName = "haru"
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "net.aros.haru.HaruCli"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        project.configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.test {
    useJUnitPlatform()
}