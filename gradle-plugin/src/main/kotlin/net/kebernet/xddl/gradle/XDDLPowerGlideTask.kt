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

import net.kebernet.xddl.powerglide.PowerGlideCommand
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

open class XDDLPowerGlideTask : DefaultTask() {

    @Optional
    @OutputDirectory
    var outputDirectory: File = File(project.buildDir, "powerglide")

    @Optional
    @OutputDirectory
    var glideDirectory: File = File(project.buildDir, "glide")

    @Input
    lateinit var elasticSearchUrl: URI

    @Input
    @Optional
    var elasticSearchAuthType: PowerGlideCommand.AuthType? = null

    @Input
    @Optional
    var username: String? = null

    @Input
    @Optional
    var password: String? = null

    @Input
    @Optional
    var bearerToken: String? = null

    @TaskAction
    fun apply() {
    }
}