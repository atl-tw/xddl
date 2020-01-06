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

import com.google.common.io.CharStreams;
import net.kebernet.xddl.migrate.ObjectMapperFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class ElasticSearchClientTest {

  private static RestHighLevelClient client;
  private ElasticSearchClient instance;

  @BeforeClass
  public static void setup() throws IOException {
    client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    client.indices().create(new CreateIndexRequest("es_client_test_1.0")
        .source(CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.mappings.json")), XContentType.JSON),
        RequestOptions.DEFAULT);
    client
        .indices()
        .create(
            new CreateIndexRequest("es_client_test_1.0.1").alias(new Alias("es_client_test"))
                .source(CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.1.mappings.json")), XContentType.JSON),
            RequestOptions.DEFAULT);
    client.indices().create(new CreateIndexRequest("es_client_test_1.0.2")
        .source(CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.2.mappings.json")), XContentType.JSON)
        , RequestOptions.DEFAULT);
  }

  @Before
  public void before() {
    this.instance =
        new ElasticSearchClient(null, ObjectMapperFactory.create())
            .initClient("http://localhost:9200", null, null);
    ;
  }

  @AfterClass
  public static void tearDown() throws IOException {
    client.indices().delete(new DeleteIndexRequest("es_client_test_1.0"), RequestOptions.DEFAULT);
    client.indices().delete(new DeleteIndexRequest("es_client_test_1.0.1"), RequestOptions.DEFAULT);
    client.indices().delete(new DeleteIndexRequest("es_client_test_1.0.2"), RequestOptions.DEFAULT);
  }

  @Test
  public void testLocalHostListVersions() throws IOException {
    ElasticSearchClient.IndexVersions results =
        instance.lookupSchemaVersions("es_client_test", false);

    assertThat(results.currentVersion).isEqualTo("es_client_test_1.0.1");
  }

  @Test
  public void testInsert() {

  }
}
