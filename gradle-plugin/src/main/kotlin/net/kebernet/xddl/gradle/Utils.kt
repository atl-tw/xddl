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

import net.kebernet.xddl.Loader
import net.kebernet.xddl.powerglide.ElasticSearchClient
import net.kebernet.xddl.powerglide.PowerGlideCommand
import java.net.URI
import java.net.URLEncoder
import java.util.Base64

fun elasticSearchClient(
    elasticSearchAuthType: PowerGlideCommand.AuthType?,
    username: String?,
    password: String?,
    bearerToken: String?,
    elasticSearchUrl: URI
): ElasticSearchClient {
    var auth: String? = null
    if (elasticSearchAuthType == PowerGlideCommand.AuthType.BASIC) {
        auth = Base64.getEncoder().encodeToString(
                "${URLEncoder.encode(username, "UTF-8")}:${URLEncoder.encode(password, "UTF-8")}"
                        .toByteArray(Charsets.UTF_8)
        )
    } else if (elasticSearchAuthType == PowerGlideCommand.AuthType.BEARER) {
        auth = bearerToken
    }

    return ElasticSearchClient(Loader.mapper()).initClient(
            elasticSearchUrl.toASCIIString(),
            auth, elasticSearchAuthType
    )
}