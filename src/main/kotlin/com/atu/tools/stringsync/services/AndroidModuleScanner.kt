package com.atu.tools.stringsync.services

import com.atu.tools.stringsync.model.ModuleTarget
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class AndroidModuleScanner(private val resourceScanner: ResourceScanner = ResourceScanner()) {

    fun scan(project: Project): List<ModuleTarget> {
        return ModuleManager.getInstance(project).modules.mapNotNull { module ->
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots
            val resDirs = mutableSetOf<Path>()
            for (root in contentRoots) {
                val rootPath = Path.of(root.path)
                val mainRes = rootPath.resolve("src/main/res")
                if (mainRes.exists()) resDirs.add(mainRes)

                val srcPath = rootPath.resolve("src")
                if (srcPath.exists()) {
                    Files.list(srcPath).use { sourceSets ->
                        sourceSets.filter { Files.isDirectory(it) }
                            .forEach { sourceSet ->
                                val resPath = sourceSet.resolve("res")
                                if (resPath.exists()) resDirs.add(resPath)
                            }
                    }
                }
            }

            if (resDirs.isEmpty()) return@mapNotNull null

            val existingLocales = resDirs.flatMap { resourceScanner.detectLocales(it) }.toSet()
            ModuleTarget(
                moduleName = module.name,
                resDirectories = resDirs.map { it.toString() }.sorted(),
                existingLocales = existingLocales
            )
        }.sortedBy { it.moduleName }
    }
}
