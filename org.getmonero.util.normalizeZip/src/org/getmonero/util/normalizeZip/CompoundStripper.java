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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A stripper that runs several strippers one after the others,
 * where the input of one stripper is the output of the previous one.
 * This class implements the Design Pattern "Decorator".
 */
final class CompoundStripper implements Stripper {
  private final Stripper[] strippers;

  /**
   * Constructs a compound stripper from a list of strippers.
   *
   * @param strippers the list of strippers.
   */
  public CompoundStripper(Stripper... strippers) {
    this.strippers = strippers;
  }

  @Override
  public void strip(File in, File out) throws IOException {
    final List<File> tmpFiles = new ArrayList<>();
    File currentIn = in;
    try {
      for (Stripper stripper : strippers) {
        final File tmp = Files.createTempFile(null, null).toFile();
        tmp.deleteOnExit();
        tmpFiles.add(tmp);
        stripper.strip(currentIn, tmp);
        currentIn = tmp;
      }
      Files.copy(currentIn.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } finally {
      for (File file : tmpFiles) {
        Files.delete(file.toPath());
      }
    }
  }

}