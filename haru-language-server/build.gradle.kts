plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0"
}

group = "net.aros"
version = "1.0.0"

repositories {
    mavenCentral()

    maven {
        name = "Minecraft Libraries"
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
    implementation(rootProject)
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "net.aros.haru.ls.Main"
    }
}

tasks.test {
    useJUnitPlatform()
}

val extServerDir = rootProject.layout.projectDirectory.dir("vscode-extension/server")
val copyToExt by tasks.registering(Copy::class) {
    group = "vscode"
    description = "Copies shadow jar into vscode extension"
    from(tasks.shadowJar)
    into(extServerDir)
}

tasks.assemble {
    dependsOn(copyToExt)
}

tasks.shadowJar {
    archiveBaseName = "haru-language-server"
    archiveClassifier.set("shadow")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "net.aros.haru.ls.Main"
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