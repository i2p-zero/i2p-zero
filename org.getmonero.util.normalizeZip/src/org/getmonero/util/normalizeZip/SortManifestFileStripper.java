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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sorts a MANIFEST file by attribute.
 */
final class SortManifestFileStripper implements Stripper {
  private static final Comparator<String> MANIFEST_ENTRY_COMPARATOR = new Comparator<String>() {
    // CHECKSTYLE IGNORE LINE: ReturnCount
    @Override
    public int compare(String o1, String o2) {
      if (o1.startsWith("Manifest-Version:")) {
        return -1;
      } else if (o2.startsWith("Manifest-Version:")) {
        return 1;
      }
      // From the "JAR File Specification":
      // Each section must start with an attribute with the name as "Name"
      else if (o1.startsWith("Name:") && !o2.startsWith("Name:")) {
        return -1;
      } else if (o2.startsWith("Name:") && !o1.startsWith("Name:")) {
        return 1;
      } else {
        return o1.compareTo(o2);
      }
    }
  };

  @Override
  public void strip(File in, File out) throws IOException {
    final List<String> lines = Files.readAllLines(in.toPath(), StandardCharsets.UTF_8);

    try (final BufferedWriter writer = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
      final String sortedManifest = sortManifestSections(lines).stream()
        .collect(Collectors.joining("\r\n"));
      try {
        writer.write(sortedManifest + "\r\n");
      } catch (IOException e) {
      }
    }
  }

  private List<String> sortManifestSections(List<String> lines) {
    final List<List<String>> sections = new ArrayList<>();
    List<String> currentSection = new ArrayList<>();
    for (String line : lines) {
      // New section?
      if (line.isEmpty()) {
        if (!currentSection.isEmpty()) {
          sections.add(currentSection);
          currentSection = new ArrayList<>();
        }
      } else {
        currentSection.add(line);
      }
    }
    if (!currentSection.isEmpty()) {
      sections.add(currentSection);
    }

    return sections.stream()
      .map(list -> sortAttributes(list))
      .map(list -> String.join("", list))
      .sorted(MANIFEST_ENTRY_COMPARATOR)
      .collect(Collectors.toList());
  }

  private List<String> sortAttributes(List<String> lines) {
    final List<String> attributes = new ArrayList<>();
    String currentAttribute = "";
    for (String line : lines) {
      if (line.startsWith(" ")) {
        currentAttribute += line + "\r\n";
      } else {
        if (!currentAttribute.isEmpty()) {
          attributes.add(currentAttribute);
          currentAttribute = "";
        }
        currentAttribute = line + "\r\n";
      }
    }
    if (!currentAttribute.isEmpty()) {
      attributes.add(currentAttribute);
    }
    attributes.sort(MANIFEST_ENTRY_COMPARATOR);
    return attributes;
  }
}