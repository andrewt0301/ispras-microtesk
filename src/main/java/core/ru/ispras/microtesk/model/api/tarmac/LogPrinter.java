/*
 * Copyright 2015 ISP RAS (http://www.ispras.ru)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.model.api.tarmac;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import ru.ispras.fortress.util.InvariantChecks;

public final class LogPrinter {
  private static final String FILE_EXTENSION = "log";

  private final String filePrefix;
  private final String fileExtension;
  private int fileCount;
  private PrintWriter fileWritter;

  public LogPrinter(final String filePrefix) {
    InvariantChecks.checkNotNull(filePrefix);

    this.filePrefix = filePrefix;
    this.fileExtension = FILE_EXTENSION;
    this.fileCount = 0;
    this.fileWritter = null;
  }

  public String createNewFile() throws IOException {
    close();
    Record.resetInstructionCount();

    final String fileName = String.format(
        "%s_%04d.%s", filePrefix, fileCount++, fileExtension);

    fileWritter = new PrintWriter(new FileWriter(fileName));
    return fileName;
  }

  public void close() {
    if (null != fileWritter) {
      fileWritter.close();
    }
  }

  public void addRecord(final Record record) {
    fileWritter.println(record.toString());
  }
}
