package org.getmonero.util.normalizeZip;/*
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
 *
 * Based on: https://github.com/Zlika/reproducible-build-maven-plugin/blob/master/src/main/java/io/github/zlika/reproducible/ZipStripper.java
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Strips non-reproducible data from a ZIP file.
 * It rebuilds the ZIP file with a predictable order for the zip entries and sets zip entry dates to a fixed value.
 */
public final class NormalizeZip {

  public static void main(String[] args) throws Exception {
    if(args.length!=2) {
      System.out.println("Arguments: timestampSource zip");
      System.exit(1);
    }
    File timestampSourceFile = Path.of(args[0]).toFile();
    File sourceZipFile = Path.of(args[1]).toFile();
    File destZipFile = Path.of(args[1]+".tmp").toFile();

    if(!timestampSourceFile.exists()) {
      System.out.println("Timestamp source file does not exist");
      System.exit(1);
    }
    if(!sourceZipFile.exists()) {
      System.out.println("Source zip file does not exist");
      System.exit(1);
    }

    System.out.println("Normalizing zip " + sourceZipFile.getCanonicalPath() + " to timestamp: " + new Date(timestampSourceFile.lastModified()));

    NormalizeZip nz = new NormalizeZip(timestampSourceFile.lastModified(), true).addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper());
    nz.strip(sourceZipFile, destZipFile);
    if(!sourceZipFile.delete()) System.out.println("Cannot delete source file");
    if(!destZipFile.renameTo(sourceZipFile)) System.out.println("Cannot rename temporary zip file");
    destZipFile = sourceZipFile;
    if(!destZipFile.setLastModified(timestampSourceFile.lastModified())) System.out.println("Cannot update zip last modified date");
  }

  /**
   * Comparator used to sort the files in the ZIP file.
   * This is mostly an alphabetical order comparator, with the exception that
   * META-INF/MANIFEST.MF and META-INF/ must be the 2 first entries (if they exist)
   * because this is required by some tools
   * (cf. https://github.com/Zlika/reproducible-build-maven-plugin/issues/16).
   */
  private static final Comparator<String> MANIFEST_FILE_SORT_COMPARATOR = new Comparator<String>() {
    // CHECKSTYLE IGNORE LINE: ReturnCount
    @Override
    public int compare(String o1, String o2) {
      if ("META-INF/MANIFEST.MF".equals(o1)) {
        return -1;
      }
      if ("META-INF/MANIFEST.MF".equals(o2)) {
        return 1;
      }
      if ("META-INF/".equals(o1)) {
        return -1;
      }
      if ("META-INF/".equals(o2)) {
        return 1;
      }
      return o1.compareTo(o2);
    }
  };

  private final long zipTimestamp;
  private final boolean fixZipExternalFileAttributes;


  public NormalizeZip(long zipTimestamp, boolean fixZipExternalFileAttributes) {
    this.zipTimestamp = zipTimestamp;
    this.fixZipExternalFileAttributes = fixZipExternalFileAttributes;
  }

  private final Map<String, Stripper> subFilters = new HashMap<>();
  private Stripper getSubFilter(String name) {
    for (Map.Entry<String, Stripper> filter : subFilters.entrySet()) {
      if (name.matches(filter.getKey())) {
          return filter.getValue();
      }
    }
    return null;
  }
  /**
   * Adds a stripper for a given file in the Zip.
   * @param filename the name of the file in the Zip (regular expression).
   * @param stripper the stripper to apply on the file.
   * @return this object (for method chaining).
   */
  public NormalizeZip addFileStripper(String filename, Stripper stripper) {
    subFilters.put(filename, stripper);
    return this;
  }


  public void strip(File in, File out) throws IOException {
    try (final ZipFile zip = new ZipFile(in);
         final ZipArchiveOutputStream zout = new ZipArchiveOutputStream(out)) {
      final List<String> sortedNames = sortEntriesByName(zip.getEntries());
      for (String name : sortedNames) {
        final ZipArchiveEntry entry = zip.getEntry(name);
        // Strip Zip entry
        final ZipArchiveEntry strippedEntry = filterZipEntry(entry);
        // Fix external file attributes if required
        if (in.getName().endsWith(".jar") || in.getName().endsWith(".war")) {
          fixAttributes(strippedEntry);
        }

        // Strip file if required
        final Stripper stripper = getSubFilter(name);
        if (stripper != null)
        {
          // Unzip entry to temp file
          final File tmp = File.createTempFile("tmp", null);
          tmp.deleteOnExit();
          Files.copy(zip.getInputStream(entry), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
          final File tmp2 = File.createTempFile("tmp", null);
          tmp2.deleteOnExit();
          stripper.strip(tmp, tmp2);
          final byte[] fileContent = Files.readAllBytes(tmp2.toPath());
          strippedEntry.setSize(fileContent.length);
          zout.putArchiveEntry(strippedEntry);
          zout.write(fileContent);
          zout.closeArchiveEntry();
        }
        else {
          // Copy the Zip entry as-is
          zout.addRawArchiveEntry(strippedEntry, zip.getRawInputStream(entry));
        }
      }
    }
  }

  private void fixAttributes(ZipArchiveEntry entry) {
    if (fixZipExternalFileAttributes) {
            /* ZIP external file attributes:
               TTTTsstrwxrwxrwx0000000000ADVSHR
               ^^^^____________________________ file type
                                                (file: 1000 , dir: 0100)
                   ^^^_________________________ setuid, setgid, sticky
                      ^^^^^^^^^________________ Unix permissions
                                         ^^^^^^ DOS attributes
               The argument of setUnixMode() only takes the 2 upper bytes. */
      if (entry.isDirectory()) {
        entry.setUnixMode((0b0100 << 12) + 0755);
      } else {
        entry.setUnixMode((0b1000 << 12) + 0644);
      }
    }
  }

  private List<String> sortEntriesByName(Enumeration<ZipArchiveEntry> entries) {
    return Collections.list(entries).stream()
      .map(e -> e.getName())
      .sorted(MANIFEST_FILE_SORT_COMPARATOR)
      .collect(Collectors.toList());
  }

  private ZipArchiveEntry filterZipEntry(ZipArchiveEntry entry) {
    // Set times
    entry.setCreationTime(FileTime.fromMillis(zipTimestamp));
    entry.setLastAccessTime(FileTime.fromMillis(zipTimestamp));
    entry.setLastModifiedTime(FileTime.fromMillis(zipTimestamp));
    entry.setTime(zipTimestamp);
    // Remove extended timestamps
    for (ZipExtraField field : entry.getExtraFields()) {
      if (field instanceof X5455_ExtendedTimestamp) {
        entry.removeExtraField(field.getHeaderId());
      }
    }
    return entry;
  }
}