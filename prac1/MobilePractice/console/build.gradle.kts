plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

val runtime = sourceSets["main"].runtimeClasspath

// ./gradlew :console:task1  (--args="fail" - имитация сбоя)
tasks.register<JavaExec>("task1") {
    group = "practice"
    classpath = runtime
    mainClass.set("Task1Kt")
    workingDir = projectDir
}

// ./gradlew :console:task2 --args="data 5"
tasks.register<JavaExec>("task2") {
    group = "practice"
    classpath = runtime
    mainClass.set("Task2Kt")
    workingDir = projectDir
}
