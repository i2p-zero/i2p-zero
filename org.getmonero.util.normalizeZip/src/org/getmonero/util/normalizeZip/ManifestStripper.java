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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Strips non-reproducible data from MANIFEST files.
 * This stripper removes the following lines from the manifest:
 * - Built-By
 * - Created-By
 * - Build-Jdk
 * - Build-Date / Build-Time
 * - Bnd-LastModified
 * It also ensures that the MANIFEST entries are in a reproducible order
 * (workaround for MSHARED-511 that was fixed in maven-archiver-3.0.1).
 */
public final class ManifestStripper implements Stripper {
  private static final String[] DEFAULT_ATTRIBUTES =
    {"Built-By", "Created-By", "Build-Jdk", "Build-Date", "Build-Time",
      "Bnd-LastModified", "OpenIDE-Module-Build-Version"};
  private final List<String> manifestAttributes;

  /**
   * Creates a stripper that will remove a default list of manifest attributes.
   */
  public ManifestStripper() {
    this.manifestAttributes = new ArrayList<String>(Arrays.asList(DEFAULT_ATTRIBUTES));
  }

  /**
   * Creates a stripper that will remove a default list of manifest attributes
   * plus additional user-specified attributes.
   *
   * @param manifestAttributes additional attributes to strip.
   */
  public ManifestStripper(List<String> manifestAttributes) {
    this();
    if (manifestAttributes != null) {
      this.manifestAttributes.addAll(manifestAttributes);
    }
  }

  @Override
  public void strip(File in, File out) throws IOException {
    final TextFileStripper s1 = new TextFileStripper();
    manifestAttributes.forEach(att -> s1.addPredicate(s -> s.startsWith(att + ":")));
    final SortManifestFileStripper s2 = new SortManifestFileStripper();
    new CompoundStripper(s1, s2).strip(in, out);
  }
}