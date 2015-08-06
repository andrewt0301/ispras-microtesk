/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.exception.ConfigurationException;
import ru.ispras.microtesk.model.api.state.IModelStateObserver;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.microtesk.test.template.Output;

/**
 * The Printer class is responsible for printing generated symbolic test programs (sequences of
 * concrete calls to a file and to the screen).
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */

public final class Printer {
  private final static int LINE_WIDTH = 100;

  private final IModelStateObserver observer;
  private final String separator;
  private int codeFileCount;

  private static String lastFileName = null;
  private PrintWriter fileWritter;

  /**
   * Constructs a printer object.
   * 
   * @param fileName Test program file name (if null or empty no file is generated),
   * @param observer Model state observer to evaluate outputs.
   * @param indentToken Token for indents.
   * @param commentToken Token for comments (used to generate the header).
   * @param separatorToken Token for separator comments.
   * @param printToScreen Specifies whether the test program is to be printed to the screen.
   * 
   * @throws IllegalArgumentException if the observer or commentToken parameter is null.
   * @throws IOException if failed to open the specified file for writing.
   */

  public Printer(final IModelStateObserver observer) {
    checkNotNull(observer);

    final String commentToken = TestSettings.getCommentToken();
    final String separatorToken = TestSettings.getSeparatorToken();

    this.observer = observer;
    this.separator = commentToken + newSeparator(LINE_WIDTH - commentToken.length(), separatorToken);

    this.codeFileCount = 0;
  }

  public String createNewFile() throws IOException {
    close();

    final String fileName = String.format(
        "%s_%04d.%s",
        TestSettings.getCodeFilePrefix(),
        codeFileCount,
        TestSettings.getCodeFileExtension()
        );

    fileWritter = newFileWriter(fileName);
    ++codeFileCount;

    lastFileName = fileName;
    return fileName;
  }

  public PrintWriter newFileWriter(final String fileName) throws IOException {
    checkNotNull(fileName);

    final File file = new File(TestSettings.getOutDir(), fileName);
    final File fileParent = file.getParentFile();
    if (null != fileParent) {
      fileParent.mkdirs();
    }

    final String fullFileName = file.getAbsolutePath();
    final PrintWriter writer = new PrintWriter(new FileWriter(fullFileName));
    if (TestSettings.isCommentsEnabled()) {
      // Prints MicroTESK information to the file (as the top file header).
      final String commentToken = TestSettings.getCommentToken();
      printToFile(writer, separator);
      printToFile(writer, String.format("%s", commentToken));
      printToFile(writer, String.format("%s This test program was automatically generated by the MicroTESK tool", commentToken));
      printToFile(writer, String.format("%s Generation started: %s", commentToken, new Date()));
      printToFile(writer, String.format("%s", commentToken));
      printToFile(writer, String.format("%s Institute for System Programming of the Russian Academy of Sciences (ISP RAS)", commentToken));
      printToFile(writer, String.format("%s 25 Alexander Solzhenitsyn st., Moscow, 109004, Russia", commentToken));
      printToFile(writer, String.format("%s http://forge.ispras.ru/projects/microtesk", commentToken));
      printToFile(writer, String.format("%s", commentToken));
      printToFile(writer, separator);
    }

    return writer;
  }

  private static String newSeparator(final int length, final String token) {
    final StringBuilder sb = new StringBuilder();
    while (sb.length() < length / token.length()) {
      sb.append(token);
    }
    return sb.toString();
  }

  /**
   * Prints the specified instruction call sequence.
   * 
   * @param sequence Instruction call sequence.
   * @throws NullPointerException if the parameter is null.
   * @throws ConfigurationException if failed to evaluate one of the output objects associated with
   *         an instruction call in the sequence.
   */

  public void printSequence(final TestSequence sequence) throws ConfigurationException {
    checkNotNull(sequence);

    final List<ConcreteCall> prologue = sequence.getPrologue();
    if (!prologue.isEmpty()) {
      printText("");
      printNote("Preparation");
      printCalls(prologue);

      printText("");
      printNote("Stimulus");
    }

    printCalls(sequence.getBody());
  }

  public void printSequence(
      final PrintWriter writer, final TestSequence sequence) throws ConfigurationException {
    final PrintWriter tempWriter = fileWritter;
    fileWritter = writer;

    try {
      printSequence(sequence);
    } finally {
      fileWritter = tempWriter;
    }
  }

  /**
   * Prints the specified list of calls (all attributes applicable at generation time).
   * 
   * @param calls List of calls.
   * @throws ConfigurationException if failed to evaluate one of the output objects
   *         associated with an instruction call.
   */

  public void printCalls(final List<ConcreteCall> calls) throws ConfigurationException {
    for (final ConcreteCall call : calls) {
      if (call.getOrigin() != null) {
        printText(String.format(TestSettings.getOriginFormat(), call.getOrigin()));
      }

      if (call.getAlignment() != null) {
        printText(String.format(TestSettings.getAlignFormat(), call.getAlignment()));
      }

      printOutputs(call.getOutputs());
      printLabels(call.getLabels());

      printText(call.getText());
    }
  }

  /**
   * Closes the generated file.
   */

  public void close() {
    if (null != fileWritter) {
      fileWritter.close();
    }
  }

  private void printOutputs(final List<Output> outputs) throws ConfigurationException {
    checkNotNull(outputs);

    for (final Output output : outputs) {
      if (output.isRuntime()) {
        continue;
      }

      final boolean printComment =
          TestSettings.isCommentsEnabled() && TestSettings.isCommentsDebug();

      final String text = output.evaluate(observer);

      if (output.isComment() && !printComment) {
        printToScreen(text);
      } else {
        printToScreen(text);
        printToFile(text);
      }
    }
  }

  private void printLabels(final List<Label> labels) {
    checkNotNull(labels);

    for (final Label label : labels) {
      printTextNoIndent(label.getUniqueName() + ":");
    }
  }

  /**
   * Prints the specified text to the screen (as is) and to the file (a comment).
   * The text is followed by an empty line. Note specify parts of code that need
   * a comment on their purpose. 
   * 
   * @param text Text to be printed.
   */

  public void printNote(final String text) {
    printToScreen(text);
    if (TestSettings.isCommentsEnabled()) {
      printCommentToFile(text);
    }
  }

  /**
   * Prints text both to the file and to the screen (if corresponding options are enabled).
   * @param text Text to be printed.
   */

  public void printText(final String text) {
    if (text != null) {
      printToScreen(text);
      printToFile(text);
    }
  }

  /**
   * Prints text with no indent both to the file and to the screen (if corresponding options are
   * enabled).
   * 
   * @param text Text to be printed.
   */

  public void printTextNoIndent(final String text) {
    if (text != null) {
      printToScreen(text);
      printToFileNoIndent(text);
    }
  }

  /**
   * Prints a special header comment that specifies the start of a code section
   * (sections include: data definitions, initialization, finalization and main code). 
   * 
   * @param text Text of the header.
   */

  public void printHeaderToFile(String text) {
    if (TestSettings.isCommentsEnabled()) {
      printSeparatorToFile();
      printCommentToFile(text);
      printSeparatorToFile();
    }
  }

  /**
   * Prints a special header comment that specifies the start of a logically separate
   * part of code. 
   * 
   * @param text Text of the header.
   */

  public void printSubheaderToFile(String text) {
    if (TestSettings.isCommentsEnabled()) {
      printSeparatorToFile();
      printCommentToFile(text);
    }
  }

  /**
   * Prints a comment to the file.
   * 
   * @param text Text of the comment to be printed.
   */

  public void printCommentToFile(String text) {
    if (text != null) {
      final String commentToken = TestSettings.getCommentToken();
      printToFile(
          String.format("%s%s%s", commentToken, commentToken.endsWith(" ") ? "" : " ", text));
    }
  }

  public void printCommentToFile(PrintWriter writer, String text) {
    if (TestSettings.isCommentsEnabled() && text != null) {
      final String commentToken = TestSettings.getCommentToken();
      printToFile(
          writer,
          String.format("%s%s%s", commentToken, commentToken.endsWith(" ") ? "" : " ", text));
    }
  }

  /**
   * Prints a special comment (a line of '*' characters) to the file to
   * separate different parts of the code.
   */

  public void printSeparatorToFile() {
    if (TestSettings.isCommentsEnabled()) {
      printToFile(separator);
    }
  }

  /**
   * Prints a special comment to the file to separate different parts of the code.
   * 
   * @param text Text of the separator.
   */

  public void printSeparatorToFile(String text) {
    if (!TestSettings.isCommentsEnabled()) {
      return;
    }

    final int prefixLength = (LINE_WIDTH - text.length()) / 2;
    final int postfixLength = LINE_WIDTH - prefixLength - text.length();
    final StringBuilder sb = new StringBuilder();

    final String commentToken = TestSettings.getCommentToken();
    final String separatorToken = TestSettings.getSeparatorToken();

    sb.append(commentToken);
    sb.append(newSeparator(prefixLength - commentToken.length() - 1, separatorToken));
    sb.append(' ');
    sb.append(text);
    sb.append(' ');
    sb.append(newSeparator(postfixLength - 1, separatorToken));

    printToFile(sb.toString());
  }

  public void printToScreen(String text) {
    Logger.debug(text);
  }

  public void printToFile(String text) {
    if (null != fileWritter) {
      fileWritter.println(String.format("%s%s", TestSettings.getIndentToken(), text));
    }
  }

  public void printToFile(PrintWriter writer, String text) {
    if (null != writer) {
      writer.println(String.format("%s%s", TestSettings.getIndentToken(), text));
    }
  }

  public void printToFileNoIndent(String text) {
    if (null != fileWritter) {
      fileWritter.println(text);
    }
  }

  public static String getLastFileName() {
    return lastFileName;
  }
}
