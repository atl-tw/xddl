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

import com.beust.jcommander.Parameter;
import java.io.File;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerGlideCommand {

  @Parameter(
      names = {"--glide-directory", "-g"},
      description = "The directory where Glide generated the transitional schemas.",
      required = true)
  private File glideDirectory;

  @Parameter(
      names = {"--report-directory", "-o"},
      description = "Directory to which to write run information.")
  private File reportDirectory;

  @Parameter(
      names = {"--batch-size", "-s"},
      description = "The number of records to do in a select->migrated->insert pass. Default 500.")
  @Builder.Default
  int batchSize = 500;

  @Parameter(
      names = {"--active-alias", "-a"},
      description = "The name of the alias that represents the active version.")
  private String activeAlias;

  @Parameter(
      names = {"--switch-active-on-completion", "-switch"},
      description =
          "Should the active alias be pointed to a new version on completion (default: true)")
  @Builder.Default
  private boolean switchActiveOnCompletion = true;

  @Parameter(
      names = {"--use-write-index", "-writeIndex"},
      description = "Should the active alias be a write index (default: false)")
  @Builder.Default
  private boolean writeIndex = false;

  @Parameter(
      names = {"--elasticsearch-url", "-es"},
      description = "The URL to the ElasticSearch instance")
  private String elasticSearchUrl;

  @Parameter(
      names = {"--elasticsearch-auth", "-auth"},
      description =
          "Either a bearer token, or URL encoded basic auth tokens in the form of 'username:password'")
  private String auth;

  @Parameter(
      names = {"--elasticsearch-auth-type", "-auth-type"},
      description = "The type of authentication to use: BEARER or (default) BASIC")
  @Builder.Default
  private AuthType authType = AuthType.BASIC;

  public enum AuthType {
    BASIC,
    BEARER;
  }
}
