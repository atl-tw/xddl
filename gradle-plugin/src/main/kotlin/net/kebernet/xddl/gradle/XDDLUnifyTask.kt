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

import net.kebernet.xddl.unify.UnifyCommand
import net.kebernet.xddl.unify.UnifyRunner
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Collections

open class XDDLUnifyTask : DefaultTask() {
    @Optional
    @Input
    var includeDirectories: List<File> =
            if (project.file("src/main/xddl/includes").exists())
                Collections.singletonList(project.file("src/main/xddl/includes"))
            else
                Collections.emptyList()
    @Optional
    @Input
    lateinit var patchDirectory: File

    @InputFile
    var sourceFile: File = project.file("src/main/xddl/Specification.xddl.json")

    @Optional
    @OutputFile
    var outputFile: File = File(project.buildDir, "xddl/Unified.xddl.json")

    @Optional
    @Input
    lateinit var newVersion: String

    @TaskAction
    fun apply() {
        outputFile.parentFile.mkdirs()
        UnifyRunner.builder()
                .command(
                        UnifyCommand.builder()
                                .inputFile(sourceFile)
                                .includes(includeDirectories)
                                .patches(listOf(patchDirectory))
                                .outputFile(outputFile)
                                .newVersion(newVersion)
                                .evaluateOgnl(true)
                                .build()
                )
                .build()
                .run()
    }
}