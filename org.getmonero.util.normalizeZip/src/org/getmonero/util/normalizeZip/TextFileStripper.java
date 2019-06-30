package org.getmonero.util.normalizeZip;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Generic text file stripper.
 */
class TextFileStripper implements Stripper {
  private final List<Predicate<String>> predicates = new ArrayList<>();

  /**
   * Adds a predicate to filter the text file.
   *
   * @param predicate the predicate.
   * @return this.
   */
  public TextFileStripper addPredicate(Predicate<String> predicate) {
    predicates.add(predicate.negate());
    return this;
  }

  @Override
  public void strip(File in, File out) throws IOException {
    try (final BufferedWriter writer = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));
         final BufferedReader reader = new BufferedReader(
           new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8))) {
      reader.lines().filter(s -> predicates.stream().allMatch(p -> p.test(s)))
        .forEach(s ->
        {
          try {
            writer.write(s);
            writer.write("\r\n");
          } catch (IOException e) {
          }
        });
    }
  }
}