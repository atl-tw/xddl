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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.SemanticVersion;
import net.kebernet.xddl.migrate.MigrationVisitor;
import net.kebernet.xddl.powerglide.metadata.GlideMetadataReader;
import net.kebernet.xddl.powerglide.metadata.PackageMetadata;

public class PowerGlideRunner {

  private static final Logger LOGGER = Logger.getLogger(PowerGlideRunner.class.getCanonicalName());
  private static final Pattern TRAILING_VERSION_PATTERN =
      Pattern.compile("[A-z0-9- _.][A-z _.]*([\\d.]*)$");
  private final ClassLoader loader;
  private MigrationState state;
  private ElasticSearchClient client;

  public PowerGlideRunner(@Nonnull PowerGlideCommand command, ClassLoader loader)
      throws IOException {
    this.client =
        new ElasticSearchClient(null, Loader.mapper())
            .initClient(command.getElasticSearchUrl(), command.getAuth(), command.getAuthType());
    Map<SemanticVersion, PackageMetadata> packageMetadata =
        new GlideMetadataReader().readGlideFolder(command.getGlideDirectory());

    ElasticSearchClient.IndexVersions current =
        client.lookupSchemaVersions(command.getActiveAlias(), command.isWriteIndex());

    SemanticVersion nextVersion = resolveNextVersion(current);
    if (nextVersion == null || nextVersion.getName() == null) {
      throw new IllegalStateException("Couldn't determine the next version from " + current);
    }
    state =
        MigrationState.builder()
            .itemName(packageMetadata.get(nextVersion).getBaseFilename())
            .currentIndex(current.currentVersion)
            .nextIndex(nextVersion.getName())
            .visitorClassName(packageMetadata.get(nextVersion).migrationVisitor())
            .batchSize(command.getBatchSize())
            .switchActiveOnCompletion(command.isSwitchActiveOnCompletion())
            .activeAlias(command.getActiveAlias())
            .build();
    this.loader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
  }

  public PowerGlideRunner(@Nonnull ElasticSearchClient client, @Nonnull MigrationState state) {
    checkNotNull(client, "You must provide an ElasticSearchClient.");
    checkNotNull(state, "You must provide an initial MigrationState.");
    this.client = client;
    this.state = state;
    this.loader = Thread.currentThread().getContextClassLoader();
  }

  public MigrationState runSingleBatch() throws IOException {

    ElasticSearchClient.Batch batch =
        client.readBatch(
            state.getCurrentIndex(),
            ofNullable(state).map(MigrationState::getScrollId).orElse(null),
            state.getBatchSize());

    if (!isNullOrEmpty(batch.errors)) {
      LOGGER.warning("There were " + batch.errors.size() + " errors reading the batch from ES.");
    }

    MigrationVisitor visitor = visitorFactory(state.getVisitorClassName());

    batch.documents.values().forEach(v -> visitor.apply((ObjectNode) v, v));

    List<ElasticSearchClient.ErrorResult> results =
        client.insertBatch(state.getNextIndex(), state.getItemName(), batch);

    if (batch.nextScrollId == null && state.isSwitchActiveOnCompletion()) {
      LOGGER.info("Updating alias " + state.getActiveAlias() + " to " + state.getNextIndex());
      client.updateActiveAliasTo(state.getActiveAlias(), state.getNextIndex(), false);
    }

    return MigrationState.builder()
        .scrollId(batch.nextScrollId)
        .visitorClassName(state.getVisitorClassName())
        .itemName(state.getItemName())
        .nextIndex(state.getNextIndex())
        .currentIndex(state.getCurrentIndex())
        .switchActiveOnCompletion(state.isSwitchActiveOnCompletion())
        .activeAlias(state.getActiveAlias())
        .successfulRecords(
            state.getSuccessfulRecords()
                + (batch.documents.size() - batch.errors.size() - results.size()))
        .failedRecords(state.getFailedRecords() + batch.errors.size() + results.size())
        .exceptions(
            state
                .getExceptions()
                .addAll(new Exceptions().from(batch.errors).addAll(new Exceptions().from(results))))
        .build();
  }

  private MigrationVisitor visitorFactory(String className) {
    try {
      return (MigrationVisitor) loader.loadClass(className).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      throw new CriticalPowerglideException(
          "Unable to create migration visitor for " + className, e);
    }
  }

  public static SemanticVersion resolveNextVersion(ElasticSearchClient.IndexVersions current) {
    Matcher currentVersionMatcher = TRAILING_VERSION_PATTERN.matcher(current.currentVersion);
    if (!currentVersionMatcher.matches()) {
      throw new IllegalStateException(
          "Could not determine a current active version from the resolved version information: "
              + current);
    }
    List<SemanticVersion> higherVersions = higherThanCurrentVersions(current);
    return isNullOrEmpty(higherVersions) ? null : higherVersions.get(0);
  }

  public static List<SemanticVersion> higherThanCurrentVersions(
      ElasticSearchClient.IndexVersions current) {
    SemanticVersion currentVersion = parseSemVer(current.currentVersion);
    return current.deployedVersions.stream()
        .map(PowerGlideRunner::parseSemVer)
        .filter(Objects::nonNull)
        .filter(v -> v.isGreaterThan(currentVersion))
        .sorted()
        .collect(Collectors.toList());
  }

  public static SemanticVersion parseSemVer(String name) {
    Matcher versionMatcher = TRAILING_VERSION_PATTERN.matcher(name);
    if (!versionMatcher.matches()) {
      LOGGER.info("Could not determine a version number for " + name);
      return null;
    }
    return new SemanticVersion(versionMatcher.group(1), name);
  }

  public MigrationState run() throws IOException {
    for (state = this.runSingleBatch();
        state.getScrollId() != null;
        state = this.runSingleBatch()) {
      LOGGER.info("Executed batch: " + state);
    }
    return state;
  }
}
