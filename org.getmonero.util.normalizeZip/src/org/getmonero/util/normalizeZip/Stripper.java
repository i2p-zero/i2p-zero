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

/**
 * Generic interface for stripping non-reproducible data.
 */
public interface Stripper
{
    /**
     * Strips non-reproducible data.
     * @param in the input file.
     * @param out the stripped output file.
     * @throws IOException if an I/O error occurs.
     */
    void strip(File in, File out) throws IOException;
}