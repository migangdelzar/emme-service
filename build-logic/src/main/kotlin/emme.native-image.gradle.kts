plugins {
  id("org.graalvm.buildtools.native")
}

graalvmNative {
  binaries.configureEach {
    fallback.set(false)
  }
}

tasks.matching { it.name.startsWith("native") }.configureEach {
  group = "native-image"
}
