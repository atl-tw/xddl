/**
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
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

import net.kebernet.xddl.generate.GenerateRunner
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Collections

open class XDDLGenerateTask : DefaultTask() {

    @Optional
    @InputFiles
    var includeDirectories: List<File> =
            if (project.file("src/main/xddl/includes").exists())
            Collections.singletonList(project.file("src/main/xddl/includes"))
            else
            Collections.emptyList()

    @InputFile
    var sourceFile: File = project.file("src/main/xddl/Specification.xddl.json")

    @Input
    @Optional
    var vals: Map<String, Any> = HashMap()

    @InputFile
    @Optional
    var valsFile: File? = if (project.file("src/main/xddl/vals.json").exists()) project.file("src/main/xddl/vals.json") else null

    @Optional
    @OutputDirectory
    var outputDirectory: File = File(project.buildDir, "xddl")

    @Input
    lateinit var plugin: String

    @TaskAction
    fun apply() {
        GenerateRunner
                .builder()
                .vals(vals)
                .valsFile(valsFile)
                .specificationFile(sourceFile)
                .includes(includeDirectories)
                .outputDirectory(outputDirectory)
                .plugins(Collections.singletonList(plugin))
                .build()
                .run()
    }
}