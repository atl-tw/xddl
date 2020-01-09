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

import net.kebernet.xddl.SemanticVersion
import net.kebernet.xddl.powerglide.PowerGlideRunner
import net.kebernet.xddl.powerglide.metadata.GlideMetadataReader
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Objects
import java.util.stream.Collectors

open class XDDLElasticSearchIndexTask : ElasticSearchTask() {

    @Input
    @Optional
    var onlyVersion: String? = null

    @Input
    lateinit var activeAlias: String

    @Optional
    @Input
    var writeIndex = false

    @Optional
    @InputDirectory
    var glideDirectory: File = File(project.buildDir, "glide")

    @Optional
    @InputDirectory
    var schemasDirectory: File = File(project.buildDir, "xddl")

    @TaskAction
    fun apply() {
        val client = elasticSearchClient(elasticSearchAuthType, username, password, bearerToken, elasticSearchUrl)
        val packageMetadata = GlideMetadataReader().readGlideFolder(glideDirectory)
        val deployed = client.lookupSchemaVersions(activeAlias, writeIndex)
        val current = if (deployed.currentVersion != null) PowerGlideRunner.parseSemVer(deployed.currentVersion) else null
        val deployVersion: SemanticVersion
        if (current != null) {
            val higherVersionsDeployed = PowerGlideRunner.higherThanCurrentVersions(deployed)
            val higherVersionsNotDeployed = packageMetadata.keys.stream()
                    .filter { it.isGreaterThan(current) }
                    .sorted()
                    .collect(Collectors.toList())

            logger.lifecycle("Current active index: $current")
            logger.lifecycle("Higher index versions already created: $higherVersionsDeployed")
            logger.lifecycle("Higher index versions not already created:$higherVersionsNotDeployed")

            if (onlyVersion != null) {
                val only = SemanticVersion(onlyVersion!!)
                if (higherVersionsNotDeployed.contains(only)) {
                    logger.lifecycle("Skipping specific deploy for $only since it already exists.")
                    return
                } else {
                    deployVersion = only
                }
            } else {
                deployVersion = higherVersionsNotDeployed.first()
            }
        } else {
            logger.lifecycle("There isn't a currently active version.")
            val currentVersion = SemanticVersion("0")
            val allDeployed = deployed.deployedVersions.stream()
                    .map { name: String? -> PowerGlideRunner.parseSemVer(name) }
                    .filter { obj: SemanticVersion? -> Objects.nonNull(obj) }
                    .filter { v: SemanticVersion -> v.isGreaterThan(currentVersion) }
                    .sorted()
                    .collect(Collectors.toList())
            val higherVersionsNotDeployed = packageMetadata.keys.stream()
                    .filter { !allDeployed.contains(it) }
                    .sorted()
                    .collect(Collectors.toList())
            logger.lifecycle("Versions not deployed $higherVersionsNotDeployed")
            deployVersion = higherVersionsNotDeployed.last()
            if (currentVersion.isGreaterThan(deployVersion)) {
                logger.lifecycle("Current version $currentVersion is already latest.")
                return
            }
        }

        logger.lifecycle("Creating index for $deployVersion")
        val schemaFile = File(schemasDirectory, "${packageMetadata[deployVersion]!!.baseFilename}.mappings.json")
        logger.lifecycle("Reading schema from ${schemaFile.absolutePath}")

        val allDeployedVersions = deployed.deployedVersions.stream()
                .map { PowerGlideRunner.parseSemVer(it) }
                .sorted()
                .collect(Collectors.toList())

        if (!allDeployedVersions.contains(deployVersion)) {
            client.createIndex("${activeAlias}_$deployVersion", schemaFile.readText(Charsets.UTF_8))
        } else {
            logger.lifecycle("$deployVersion already deployed.")
        }

        if (deployed.currentVersion == null) {
            logger.lifecycle("Since there is no active alias, setting it to $deployVersion")
            client.updateActiveAliasTo(activeAlias, "${activeAlias}_$deployVersion", writeIndex)
        }
    }
}