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

import net.kebernet.xddl.generate.GenerateRunner
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class XDDLGlideGenerateTask : DefaultTask() {

    @Optional
    @OutputDirectory
    var outputDirectory: File = File(project.buildDir, "xddl")

    @Input
    lateinit var plugin: String

    @Input
    @Optional
    var vals: Map<String, Any> = HashMap()

    @InputFile
    @Optional
    var valsFile: File? = if (project.file("src/main/xddl/vals.json").exists()) project.file("src/main/xddl/vals.json") else null

    @Optional
    @OutputDirectory
    var glideDirectory: File = File(project.buildDir, "glide")

    @TaskAction
    fun apply() {
        val files = glideDirectory.listFiles {
            f -> f.name.endsWith(".xddl.json")
        }
        files.forEach {
            f ->
            logger.lifecycle("Generating " + plugin + " for " + f.absolutePath)
            GenerateRunner
                    .builder()
                    .vals(vals)
                    .valsFile(valsFile)
                    .specificationFile(f)
                    .outputDirectory(outputDirectory)
                    .plugins(listOf(plugin))
                    .build()
                    .run()
        }
    }
}