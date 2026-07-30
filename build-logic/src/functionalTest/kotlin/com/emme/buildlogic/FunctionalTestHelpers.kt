package com.emme.buildlogic

import java.nio.file.Path

fun findBuildLogicDir(): Path {
    val cwd = Path.of(System.getProperty("user.dir"))
    var path = cwd
    while (path != null) {
        val candidate = path.resolve("build-logic")
        if (candidate.toFile().isDirectory) {
            return candidate.toAbsolutePath()
        }
        path = path.parent
    }
    return Path.of(System.getProperty("user.dir")).resolve("build-logic").toAbsolutePath()
}

fun findCatalogPath(): Path {
    val cwd = Path.of(System.getProperty("user.dir"))
    var path = cwd
    while (path != null) {
        val candidate = path.resolve("gradle/libs.versions.toml")
        if (candidate.toFile().exists()) {
            return candidate.toAbsolutePath()
        }
        path = path.parent
    }
    return Path.of(System.getProperty("user.dir")).resolve("gradle/libs.versions.toml").toAbsolutePath()
}

fun escapePath(path: Path): String = path.toString().replace("\\", "\\\\")
