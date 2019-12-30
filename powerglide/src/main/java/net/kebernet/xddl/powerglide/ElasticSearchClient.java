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

import static net.kebernet.xddl.model.Utils.isNullOrEmpty;
import static net.kebernet.xddl.model.Utils.stackTraceAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.kebernet.xddl.model.Utils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ElasticSearchClient {
  private static final Logger LOGGER =
      Logger.getLogger(ElasticSearchClient.class.getCanonicalName());
  private RestHighLevelClient client;
  private final ObjectMapper objectMapper;
  private final RequestOptions options = RequestOptions.DEFAULT;

  @Inject
  public ElasticSearchClient(
      @Nullable RestHighLevelClient client, @Nonnull ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
  }

  public Batch readBatch(String indexName, String lastScrollId, int pageSize) throws IOException {

    SearchRequest searchRequest = new SearchRequest(indexName);
    searchRequest.scroll(lastScrollId);
    SearchSourceBuilder searchSourceBuilder =
        new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).size(pageSize);
    searchRequest.source(searchSourceBuilder);
    SearchHit[] searchHits;
    String scrollId;

    if (lastScrollId == null) {
      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      scrollId = searchResponse.getScrollId();
      searchHits = searchResponse.getHits().getHits();
    } else {
      SearchScrollRequest scrollRequest = new SearchScrollRequest(lastScrollId);
      scrollRequest.scroll(lastScrollId);
      SearchResponse searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
      scrollId = searchResponse.getScrollId();
      searchHits = searchResponse.getHits().getHits();
    }

    if (searchHits == null || searchHits.length == 0) {
      ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
      clearScrollRequest.addScrollId(scrollId);
      ClearScrollResponse clearScrollResponse =
          client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
      if (!clearScrollResponse.isSucceeded()) {
        LOGGER.warning(
            "CLEAR SCROLL ON MIGRATION WAS UNSUCCESSFUL. The scroll on "
                + indexName
                + " could not be cleaned: "
                + scrollId);
      }
      scrollId = null;
    }
    Stream<SearchHit> stream = searchHits != null ? Arrays.stream(searchHits) : Stream.empty();
    ArrayList<ErrorResult> errors = new ArrayList<>();
    return new Batch(
        scrollId,
        stream
            .map(
                h -> {
                  byte[] value = h.getSourceRef().toBytesRef().bytes;
                  try {
                    return new AbstractMap.SimpleEntry<>(h.getId(), objectMapper.readTree(value));
                  } catch (IOException e) {
                    ErrorResult result =
                        new ErrorResult(
                            h.getId(), "Failed to parse " + indexName + "/" + h.getId(), e, null);
                    errors.add(result);
                    LOGGER.log(Level.SEVERE, result.error, e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(
                Collectors.toMap(
                    AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)),
        errors);
  }

  public @Nonnull List<ErrorResult> insertBatch(String indexName, String itemType, Batch batch) {
    try {
      BulkRequest request = new BulkRequest();
      if (isNullOrEmpty(batch.documents)) {
        return Collections.emptyList();
      }
      for (Map.Entry<String, JsonNode> node : batch.documents.entrySet()) {
        request.add(
            new IndexRequest(indexName)
                .type(itemType)
                .id(node.getKey())
                .source(node.getValue().toString(), XContentType.JSON));
      }
      BulkResponse result = client.bulk(request, RequestOptions.DEFAULT);
      return Arrays.stream(result.getItems())
          .filter(BulkItemResponse::isFailed)
          .map(
              r ->
                  new ErrorResult(
                      r.getId(),
                      r.getFailureMessage(),
                      r.getFailure().getCause(),
                      batch.documents.get(r.getId())))
          .collect(Collectors.toList());
    } catch (Exception e) {
      return batch.documents.entrySet().stream()
          .map(
              doc ->
                  new ErrorResult(
                      doc.getKey(), "Exception thrown in batch operation.", e, doc.getValue()))
          .collect(Collectors.toList());
    }
  }

  public ElasticSearchClient initClient(
      String url, String auth, PowerGlideCommand.AuthType authType) {
    URI uri = URI.create(url);
    if (auth == null) {
      this.client =
          new RestHighLevelClient(
              RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())));
    }

    switch (authType) {
      case BASIC:
        String[] urlEncodedCreds = auth.split(":");
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        try {
          credentialsProvider.setCredentials(
              AuthScope.ANY,
              new UsernamePasswordCredentials(
                  URLDecoder.decode(urlEncodedCreds[0], "UTF-8"),
                  URLDecoder.decode(urlEncodedCreds[1], "UTF-8")));
          this.client =
              new RestHighLevelClient(
                  RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()))
                      .setHttpClientConfigCallback(
                          clientBuilder ->
                              clientBuilder.setDefaultCredentialsProvider(credentialsProvider)));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException("Failed to decode basic auth credentials", e);
        }
      case BEARER:
        this.client =
            new RestHighLevelClient(
                RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())));
        this.options.toBuilder().addHeader("Authorization", "Bearer " + auth);
    }
    return this;
  }

  public IndexVersions lookupSchemaVersions(String aliasName, boolean useWriteIndex)
      throws IOException {
    GetAliasesRequest request = new GetAliasesRequest();
    GetAliasesResponse response = client.indices().getAlias(request, RequestOptions.DEFAULT);
    Map<String, Set<AliasMetaData>> aliases = response.getAliases();
    String current = null;
    List<String> deployed = new ArrayList<>(aliases.size());
    for (Map.Entry<String, Set<AliasMetaData>> entry : aliases.entrySet()) {
      deployed.add(entry.getKey());
      if (Utils.neverNull(entry.getValue()).stream()
          .anyMatch(
              aliasMetaData ->
                  (!useWriteIndex || Boolean.TRUE.equals(aliasMetaData.writeIndex()))
                      && aliasName.equals(aliasMetaData.getAlias()))) {
        current = entry.getKey();
      }
    }
    return new IndexVersions(current, deployed);
  }

  public static class IndexVersions {
    public String currentVersion;
    public final List<String> deployedVersions;

    public IndexVersions(String currentVersion, List<String> deployedVersions) {
      this.currentVersion = currentVersion;
      this.deployedVersions = deployedVersions;
    }

    @Override
    public String toString() {
      return "IndexVersions{"
          + "currentVersion='"
          + currentVersion
          + '\''
          + ", deployedVersions="
          + deployedVersions
          + '}';
    }
  }

  public static class ErrorResult {
    final String documentId;
    final String error;
    final String stackTrace;
    final JsonNode source;

    public ErrorResult(String documentId, String error, Throwable exception, JsonNode source) {
      this.documentId = documentId;
      this.error = error;
      this.stackTrace = stackTraceAsString(exception);
      this.source = source;
    }

    @Override
    public String toString() {
      return "ErrorResult{"
          + "documentId='"
          + documentId
          + '\''
          + ", error='"
          + error
          + '\''
          + ", stackTrace="
          + stackTrace
          + ", source="
          + source
          + '}';
    }
  }

  public static class Batch {
    final String nextScrollId;
    final Map<String, JsonNode> documents;
    final List<ErrorResult> errors;

    public Batch(String nextScrollId, Map<String, JsonNode> documents, List<ErrorResult> errors) {
      this.nextScrollId = nextScrollId;
      this.documents = documents;
      this.errors = errors;
    }
  }
}
