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
package net.kebernet.xddl.powerglide;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.SemanticVersion;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;

public class PowerGlideRunnerTest {

  @Test
  public void testResolveNextVersion() {
    ElasticSearchClient.IndexVersions versions =
        new ElasticSearchClient.IndexVersions("Foo_v1.0", Arrays.asList("Foo_v1.1", "Foo_v1.2"));

    assertThat(PowerGlideRunner.resolveNextVersion(versions)).isEqualTo(new SemanticVersion("1.1"));

  }
}
