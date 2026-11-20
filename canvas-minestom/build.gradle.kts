plugins {
    `java-library`
    id("net.ltgt.errorprone")
    `maven-publish`
    id("com.gradleup.shadow")
}

dependencies {
    "api"(project(":canvas-pack"))
    "api"(project(":canvas-elements"))
    "compileOnly"("net.minestom:minestom:2026.10.07-26.2")
    "compileOnly"("org.slf4j:slf4j-api:2.0.16")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("canvas-minestom")
    archiveClassifier.set("")
    configurations.set(listOf(project.configurations.runtimeClasspath.get()))
    // Minestom already provides Adventure, Gson, and SLF4J
    dependencies {
        exclude(dependency("net.kyori:.*"))
        exclude(dependency("com.google.code.gson:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.jspecify:jspecify"))
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}

shadow {
    addShadowVariantIntoJavaComponent.set(false)
}
