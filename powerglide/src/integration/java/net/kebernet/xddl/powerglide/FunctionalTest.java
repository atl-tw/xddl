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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;
import com.my.project.model.v1_0_1.Name;
import com.my.project.model.v1_0_1.Team;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

public class FunctionalTest {

  private static RestHighLevelClient client;
  private ElasticSearchClient instance;

  @BeforeClass
  public static void setup() throws IOException {
    client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    client
        .indices()
        .create(
            new CreateIndexRequest("es_client_test_1.0")
                .source(
                    CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.mappings.json")),
                    XContentType.JSON),
            RequestOptions.DEFAULT);
    client
        .indices()
        .create(
            new CreateIndexRequest("es_client_test_1.0.1")
                .alias(new Alias("es_client_test"))
                .source(
                    CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.1.mappings.json")),
                    XContentType.JSON),
            RequestOptions.DEFAULT);
    client
        .indices()
        .create(
            new CreateIndexRequest("es_client_test_1.0.2")
                .source(
                    CharStreams.toString(new FileReader("./build/xddl/xddl_1.0.2.mappings.json")),
                    XContentType.JSON),
            RequestOptions.DEFAULT);
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
  public void testEndToEnd() throws IOException, InterruptedException {
    ElasticSearchClient.IndexVersions results =
        instance.lookupSchemaVersions("es_client_test", false);

    assertThat(results.currentVersion).isEqualTo("es_client_test_1.0.1");
    testInsert();
    testMigrate();
  }

  public void testInsert() throws IOException, InterruptedException {
    Map<String, JsonNode> inserts = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      Team team = new Team();
      team.members(
          Arrays.asList(
              new Name().firstName("Robert" + i).lastName("Cooper"),
              new Name().firstName("Jonathan" + i).lastName("Cooper"),
              new Name().firstName("Leslie" + i).lastName("Cooper")));
      inserts.put(UUID.randomUUID().toString(), ObjectMapperFactory.create().valueToTree(team));
    }
    ElasticSearchClient.Batch batch = new ElasticSearchClient.Batch(null, inserts, null);
    ElasticSearchClient.IndexVersions results =
        instance.lookupSchemaVersions("es_client_test", false);

    instance.insertBatch(results.currentVersion, "xddl_1.0.1", batch);
    Thread.sleep(1000);
  }

  public void testMigrate() throws IOException {
    PowerGlideCommand command =
        PowerGlideCommand.builder()
            .batchSize(500)
            .activeAlias ("es_client_test")
            .glideDirectory(new File("build/glide"))
            .elasticSearchUrl("http://localhost:9200")
            .switchActiveOnCompletion(false)
            .build();
    PowerGlideRunner runner = new PowerGlideRunner(command, null);
    MigrationState result = runner.run();
    assertThat(result.getSuccessfulRecords()).isEqualTo(1000);
  }
}
