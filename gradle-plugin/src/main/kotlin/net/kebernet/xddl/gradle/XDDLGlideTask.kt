/**
 * Copyright 2019 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kebernet.xddl.gradle

import net.kebernet.xddl.Loader
import net.kebernet.xddl.SemanticVersion
import net.kebernet.xddl.glide.GlideCommand
import net.kebernet.xddl.glide.GlideRunner
import net.kebernet.xddl.model.Specification
import net.kebernet.xddl.model.Utils.neverNull
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Collections

open class XDDLGlideTask : DefaultTask() {

    @Optional
    @InputFiles
    var includeDirectories: List<File> =
            if (project.file("src/main/xddl/includes").exists())
                Collections.singletonList(project.file("src/main/xddl/includes"))
            else
                Collections.emptyList()

    @Input
    @Optional
    var vals: Map<String, Any> = HashMap()

    @InputFile
    @Optional
    var valsFile: File? = if (project.file("src/main/xddl/vals.json").exists()) project.file("src/main/xddl/vals.json") else null

    @Optional
    @InputFiles
    var patchesDirectory: File = project.file("src/main/xddl/patches")

    @InputFile
    var sourceFile: File = project.file("src/main/xddl/Specification.xddl.json")

    @Optional
    @OutputDirectory
    var outputDirectory: File = File(project.buildDir, "glide")

    @OutputFiles
    var outputFiles: ArrayList<File> = ArrayList()

    @TaskAction
    fun apply() {
        outputDirectory.mkdirs()
        GlideRunner.builder()
                .command(
                        GlideCommand.builder()
                                .inputFile(sourceFile)
                                .includes(includeDirectories)
                                .vals(vals)
                                .valsFile(valsFile)
                                .patches(patchesDirectory)
                                .outputDirectory(outputDirectory)
                                .build()
                )
                .build()
                .run()
        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
        val fileArray = neverNull(outputDirectory.listFiles { f ->
            f.name.endsWith(".xddl.json")
        })
        val sorted = fileArray.sortedBy { f ->
            if (f.name.startsWith("baseline")) SemanticVersion("0")
            else SemanticVersion(f.name.substring(0, f.name.indexOf('.'))
                    .replace('_', '.'))
        }
        outputFiles.addAll(sorted)

        project.version = Loader.mapper()
                .readValue(outputFiles.last(), Specification::class.java)
                .version
    }
}