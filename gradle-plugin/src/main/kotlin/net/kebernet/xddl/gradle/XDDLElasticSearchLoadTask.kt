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

import com.fasterxml.jackson.databind.JsonNode
import net.kebernet.xddl.migrate.ObjectMapperFactory
import net.kebernet.xddl.powerglide.ElasticSearchClient
import net.kebernet.xddl.powerglide.PowerGlideCommand
import net.kebernet.xddl.powerglide.PowerGlideRunner
import net.kebernet.xddl.powerglide.metadata.GlideMetadataReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

open class XDDLElasticSearchLoadTask : DefaultTask() {

    @Input
    @Optional
    val mapper = ObjectMapperFactory.create()

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

    @Optional
    @InputDirectory
    var dataDirectory: File = File(project.projectDir, "src/elasticsearch/load")

    @Input
    lateinit var activeAlias: String

    @Optional
    @Input
    var writeIndex = false

    @Optional
    @InputDirectory
    var glideDirectory: File = File(project.buildDir, "glide")

    @TaskAction
    fun apply() {
        val client = elasticSearchClient(elasticSearchAuthType, username, password, bearerToken, elasticSearchUrl)
        val packageMetadata = GlideMetadataReader().readGlideFolder(glideDirectory)
        val deployed = client.lookupSchemaVersions(activeAlias, writeIndex)
        val files = dataDirectory.listFiles { _, name -> name.endsWith(".json") } ?: return
        val batch = HashMap<String, JsonNode>()
        val version = PowerGlideRunner.parseSemVer(deployed.currentVersion)

        var total = 0
        while (total < files.size) {
            batch.clear()
            for (i in 0..499) {
                if (total >= files.size) {
                    break
                }
                val node = mapper.readTree(files[total])
                batch.put(files[total].name.dropLast(5), node)
                total++
            }
            val errors = client.insertBatch("${activeAlias}_$version", packageMetadata[version]!!.baseFilename,
                    ElasticSearchClient.Batch(null, batch, ArrayList()))
            if (!errors.isEmpty()) {
                logger.lifecycle(errors.toString())
            }
        }
        logger.lifecycle("Inserted $total records to $activeAlias")
    }
}