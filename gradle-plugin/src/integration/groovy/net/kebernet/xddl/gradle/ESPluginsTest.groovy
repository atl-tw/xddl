/*
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

import net.kebernet.xddl.gradle.CopyDir
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.FileSystems
import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ESPluginsTest extends Specification {

    def setup() {
        def from = FileSystems.default.getPath(new File("src/integration/projects").getAbsolutePath())
        def to = FileSystems.default.getPath(new File("build/integration/projects").getAbsolutePath())
        Files.createDirectories(to)
        Files.walkFileTree(from, new CopyDir(from, to))

    }

    def "1.0 "() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(new File("build/integration/projects/xddl-glide-1.0"))
                .withArguments('loadData', "--stacktrace")
                .withPluginClasspath()
                .withDebug(true)
                .forwardStdOutput(new OutputStreamWriter(System.out))
                .build()
        then:
        result.task(":createIndex").outcome == SUCCESS
        result.task(":loadData").outcome == SUCCESS
    }

    def "1.0.1 "() {
        Thread.sleep(2000)
        when:
        def result = GradleRunner.create()
                .withProjectDir(new File("build/integration/projects/xddl-glide-1.0.1"))
                .withArguments('clean','build', "migrate", "--stacktrace")
                .withPluginClasspath()
                .withDebug(true)
                .forwardStdOutput(new OutputStreamWriter(System.out))
                .build()
        then:
        result.task(":build").outcome == SUCCESS
        result.task(":migrate").outcome == SUCCESS
    }

}