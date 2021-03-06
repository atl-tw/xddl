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

import org.gradle.api.Plugin
import org.gradle.api.Project

open class XDDLPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            project.extensions.extraProperties.set("XDDLGenerate", XDDLGenerateTask::class.java)
            project.extensions.extraProperties.set("XDDLGlide", XDDLGlideTask::class.java)
            project.extensions.extraProperties.set("XDDLGlideGenerate", XDDLGlideGenerateTask::class.java)
            project.extensions.extraProperties.set("XDDLUnify", XDDLUnifyTask::class.java)
            project.extensions.extraProperties.set("XDDLPowerGlide", XDDLPowerGlideTask::class.java)
            project.extensions.extraProperties.set("XDDLElasticSearchIndex", XDDLElasticSearchIndexTask::class.java)
            project.extensions.extraProperties.set("XDDLElasticSearchLoad", XDDLElasticSearchLoadTask::class.java)
        }
    }
}
