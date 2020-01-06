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
package net.kebernet.xddl.powerglide.metadata;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.SemanticVersion;
import net.kebernet.xddl.java.Resolver;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;

public class GlideMetadataReader {

  public Map<SemanticVersion, PackageMetadata> readGlideFolder(File glideDirectory)
      throws IOException {
    Map<SemanticVersion, PackageMetadata> result = new HashMap<>();
    File[] xddls = glideDirectory.listFiles(f -> f.getName().endsWith(".xddl.json"));
    if (xddls == null) {
      throw new IOException("No xddl files in " + glideDirectory.getAbsolutePath());
    }
    for (File unified : xddls) {
      Specification spec = Loader.builder().main(unified).build().read();
      Context context = new Context(Loader.mapper(), spec);
      String packageName = Resolver.resolvePackageName(context);
      String entryRef = spec.getEntryRef();
      String version = ofNullable(spec.getVersion()).orElse("0");
      result.put(
          new SemanticVersion(version),
          PackageMetadata.builder()
              .entryRef(entryRef)
              .packageName(packageName)
              .baseFilename(context.createBaseFilename())
              .build());
    }
    return result;
  }
}
