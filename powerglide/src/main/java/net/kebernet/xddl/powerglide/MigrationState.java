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

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MigrationState {

  private String scrollId;
  private int successfulRecords;
  private int failedRecords;
  @Builder.Default private Exceptions exceptions = new Exceptions();
  private String visitorClassName;
  private String itemName;
  private String currentIndex;
  private String nextIndex;
  private int batchSize;
  private boolean switchActiveOnCompletion;
  private String activeAlias;
}
