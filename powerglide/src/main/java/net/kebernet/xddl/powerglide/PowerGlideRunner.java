/*
 * Copyright 2019 Robert Cooper, ThoughtWorks
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
import static net.kebernet.xddl.model.Utils.streamOrEmpty;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Builder;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.SemanticVersion;
import net.kebernet.xddl.migrate.MigrationVisitor;

@Builder
public class PowerGlideRunner {

  private static final Logger LOGGER = Logger.getLogger(PowerGlideRunner.class.getCanonicalName());
  private static final Pattern TRAILING_VERSION_PATTERN =
      Pattern.compile("[A-z0-9- _.][A-z _.]*([\\d.]*)$");
  private final PowerGlideCommand command;
  private final MigrationState state;
  private ElasticSearchClient client;
  private Map<SemanticVersion, String> versionsToMigrationVisitorClassNames;

  @Builder
  public PowerGlideRunner(PowerGlideCommand command, MigrationState state) {
    this.command = command;
    this.state = state;
    this.client =
        new ElasticSearchClient(null, Loader.mapper())
            .initClient(command.getElasticSearchUrl(), command.getAuth(), command.getAuthType());
  }

  @Inject
  public PowerGlideRunner(
      PowerGlideCommand command, MigrationState state, ElasticSearchClient client) {
    this.command = command;
    this.state = state;
    this.client = client;
  }

  private void init() {
    // TODO not done
    if (this.command.getGlideDirectory() != null) {
      streamOrEmpty(command.getGlideDirectory().listFiles())
          .map(f -> Loader.builder().main(f).build().read());
    }
  }

  public MigrationState runSingleBatch() throws IOException {
    ElasticSearchClient.IndexVersions current =
        client.lookupSchemaVersions(command.getActiveAlias(), command.isWriteIndex());
    SemanticVersion nextVersion = resolveNextVersion(current);
    ElasticSearchClient.Batch batch =
        client.readBatch(current.currentVersion, state.getScrollId(), command.batchSize);

    return state;
  }

  private MigrationVisitor visitorFactory(SemanticVersion nextVersion) {
    return null;
  }

  @VisibleForTesting
  static SemanticVersion resolveNextVersion(ElasticSearchClient.IndexVersions current) {
    Matcher currentVersionMatcher = TRAILING_VERSION_PATTERN.matcher(current.currentVersion);
    if (!currentVersionMatcher.matches()) {
      throw new IllegalStateException(
          "Could not determine a current active version from the resolved version information: "
              + current);
    }
    SemanticVersion currentVersion = new SemanticVersion(currentVersionMatcher.group(1));
    List<SemanticVersion> higherVersions =
        current.deployedVersions.stream()
            .map(
                s -> {
                  Matcher versionMatcher = TRAILING_VERSION_PATTERN.matcher(s);
                  if (!versionMatcher.matches()) {
                    LOGGER.info("Could not determine a version number for " + s);
                    return null;
                  }
                  return new SemanticVersion(versionMatcher.group(1));
                })
            .filter(Objects::nonNull)
            .filter(v -> v.isGreaterThan(currentVersion))
            .sorted()
            .collect(Collectors.toList());
    return isNullOrEmpty(higherVersions) ? null : higherVersions.get(0);
  }

  public void run() {}
}
