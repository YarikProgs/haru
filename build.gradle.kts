plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0"
    `maven-publish`
}

group = "net.aros"
version = "1.0.0-beta.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("info.picocli:picocli:4.7.7")
    implementation("com.google.code.gson:gson:2.10.1")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
java.withSourcesJar()

tasks.shadowJar {
    archiveBaseName = "haru"
    archiveClassifier.set("cli")
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
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}