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
package net.kebernet.xddl.util;

import static net.kebernet.xddl.model.Utils.neverNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Histogram<K> {

  private final ConcurrentHashMap<K, AtomicInteger> values = new ConcurrentHashMap<>();

  public Histogram<K> add(K value) {
    values.computeIfAbsent(value, (k) -> new AtomicInteger(0)).incrementAndGet();
    return this;
  }

  public Set<Map.Entry<K, Integer>> entrySet() {
    return values.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()))
        .entrySet();
  }

  @SuppressWarnings("unchecked")
  public <T extends Histogram<K>> T addAll(Set<Map.Entry<K, Integer>> set) {
    neverNull(set)
        .forEach(
            e -> {
              for (int i = 0; i < e.getValue(); i++) {
                add(e.getKey());
              }
            });
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends Histogram<K>> T addAll(T histogram) {
    this.addAll(histogram.entrySet());
    return (T) this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Histogram: {\n");
    this.entrySet()
        .forEach(
            e -> sb.append("\t").append(e.getKey()).append(": ").append(e.getValue()).append('\n'));
    return sb.append('}').toString();
  }
}
