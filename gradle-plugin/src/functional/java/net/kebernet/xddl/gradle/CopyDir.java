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
package net.kebernet.xddl.gradle;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CopyDir extends SimpleFileVisitor<Path> {
  private Path sourceDir;
  private Path targetDir;

  public CopyDir(Path sourceDir, Path targetDir) {
    this.sourceDir = sourceDir;
    this.targetDir = targetDir;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {

    try {
      Path targetFile = targetDir.resolve(sourceDir.relativize(file));
      if (Files.exists(targetFile)) Files.delete(targetFile);
      Files.copy(file, targetFile);
    } catch (IOException ex) {
      System.err.println(ex);
    }

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
    try {
      Path newDir = targetDir.resolve(sourceDir.relativize(dir));
      if (!Files.exists(newDir)) Files.createDirectory(newDir);
    } catch (IOException ex) {
      System.err.println(ex);
    }

    return FileVisitResult.CONTINUE;
  }
}
