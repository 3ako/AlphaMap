plugins {
    java
}

allprojects {
    group = "hw.zako"
    version = property("mod_version") as String
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    dependencies {
        val lombok = "org.projectlombok:lombok:${rootProject.property("lombok_version")}"
        "compileOnly"(lombok)
        "annotationProcessor"(lombok)
        "testCompileOnly"(lombok)
        "testAnnotationProcessor"(lombok)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
        options.encoding = "UTF-8"
    }
}
