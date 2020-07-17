/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.mock.data;


import eu.cessda.pasc.oci.helpers.FileHandler;
import lombok.experimental.UtilityClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * mock data for Record headers.
 *
 * @author moses AT doraventures DOT com
 */
@UtilityClass
public class CMMStudyTestData {

  public static String getContent(String filePath) throws IOException {
    FileHandler fileHandler = new FileHandler();
      return fileHandler.getFileAsString(filePath);
  }

  public static InputStream getContentAsStream(String filePath) throws FileNotFoundException {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileAsStream(filePath);
  }
}