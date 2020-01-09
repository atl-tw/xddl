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
import net.kebernet.xddl.powerglide.PowerGlideRunner
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLEncoder
import java.util.Base64

open class XDDLPowerGlideTask : ElasticSearchTask() {

    @Optional
    @OutputDirectory
    var outputDirectory: File = File(project.buildDir, "powerglide")

    @Optional
    @OutputDirectory
    var glideDirectory: File = File(project.buildDir, "glide")

    @Input
    lateinit var activeAlias: String

    @Optional
    @Input
    var batchSize = 500

    @Optional
    @Input
    var switchActiveOnCompletion = false

    @Optional
    @Input
    var writeIndex = false

    @TaskAction
    fun apply() {
        var auth: String? = null
        if (elasticSearchAuthType == PowerGlideCommand.AuthType.BASIC) {
            auth = Base64.getEncoder().encodeToString(
                    "${URLEncoder.encode(username, "UTF-8")}:${URLEncoder.encode(password, "UTF-8")}"
                            .toByteArray(Charsets.UTF_8)
            )
        } else if (elasticSearchAuthType == PowerGlideCommand.AuthType.BEARER) {
            auth = bearerToken
        }
        val command = PowerGlideCommand.builder()
                .activeAlias(activeAlias)
                .authType(elasticSearchAuthType)
                .auth(auth)
                .elasticSearchUrl(elasticSearchUrl.toASCIIString())
                .glideDirectory(glideDirectory)
                .batchSize(batchSize)
                .reportDirectory(outputDirectory)
                .switchActiveOnCompletion(switchActiveOnCompletion)
                .writeIndex(writeIndex)
                .build()

        val result = PowerGlideRunner(command).run()
        logger.lifecycle("Completed migration run:\n\t$result")
    }
}