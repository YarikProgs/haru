plugins {
    id("java")
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

    implementation("com.mojang:datafixerupper:8.0.16")
    implementation("info.picocli:picocli:4.7.7")
}

tasks.test {
    useJUnitPlatform()
}