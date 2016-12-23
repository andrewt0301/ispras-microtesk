/*
 * Copyright 2012-2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.ispras.microtesk.decoder.Disassembler;
import ru.ispras.microtesk.options.Option;
import ru.ispras.microtesk.options.OptionReader;
import ru.ispras.microtesk.options.Options;
import ru.ispras.microtesk.settings.GeneratorSettings;
import ru.ispras.microtesk.settings.SettingsParser;
import ru.ispras.microtesk.symexec.SymbolicExecutor;
import ru.ispras.microtesk.test.Statistics;
import ru.ispras.microtesk.test.TestEngine;
import ru.ispras.microtesk.test.sequence.GeneratorConfig;
import ru.ispras.microtesk.test.sequence.engine.Adapter;
import ru.ispras.microtesk.test.sequence.engine.Engine;
import ru.ispras.microtesk.translator.Translator;
import ru.ispras.microtesk.translator.TranslatorContext;
import ru.ispras.microtesk.utils.FileUtils;
import ru.ispras.testbase.TestBaseRegistry;
import ru.ispras.testbase.generator.DataGenerator;
import ru.ispras.testbase.stub.TestBase;

public final class MicroTESK {
  private MicroTESK() {}

  private static final List<Translator<?>> translators = new ArrayList<>();

  public static void main(final String[] args) {
    try {
      final OptionReader optionReader = new OptionReader(Config.loadSettings(), args);

      try {
        optionReader.read();
      } catch (final Exception e) {
        Logger.error("Incorrect command line: " + e.getMessage());
        Logger.message(optionReader.getHelpText());
        exitWithError();
      }

      final Options options = optionReader.getOptions();
      final String[] arguments = optionReader.getArguments();

      if (options.getValueAsBoolean(Option.HELP)) {
        Logger.message(optionReader.getHelpText());
        return;
      }

      Logger.setDebug(options.getValueAsBoolean(Option.VERBOSE));

      final List<Plugin> plugins = Config.loadPlugins();
      registerPlugins(plugins);

      if (!runTask(options, arguments, plugins)) {
        exitWithError();
      }
    } catch (final Throwable e) {
      Logger.exception(e);
      exitWithError();
    }
  }

  private static void exitWithError() {
    System.exit(-1);
  }

  private static void registerPlugins(final List<Plugin> plugins) {
    for (final Plugin plugin : plugins) {
      // Register the translator.
      final Translator<?> translator = plugin.getTranslator();

      if (translator != null) {
        translators.add(translator);
      }

      // Register the engines.
      final GeneratorConfig<?> generatorConfig = GeneratorConfig.get();
      final Map<String, Engine<?>> engines = plugin.getEngines();

      for (final Map.Entry<String, Engine<?>> entry : engines.entrySet()) {
        generatorConfig.registerEngine(entry.getKey(), entry.getValue());
      }

      // Register the adapters.
      final Map<String, Adapter<?>> adapters = plugin.getAdapters();

      for (final Map.Entry<String, Adapter<?>> entry : adapters.entrySet()) {
        generatorConfig.registerAdapter(entry.getKey(), entry.getValue());
      }

      // Register the data generators.
      final TestBaseRegistry testBaseRegistry = TestBase.get().getRegistry();
      final Map<String, DataGenerator> dataGenerators = plugin.getDataGenerators();

      if (dataGenerators != null) {
        for (final Map.Entry<String, DataGenerator> entry : dataGenerators.entrySet()) {
          testBaseRegistry.registerGenerator(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static boolean runTask(
      final Options options,
      final String[] arguments,
      final List<Plugin> plugins) throws Throwable {
    if (options.getValueAsBoolean(Option.GENERATE)) {
      return generate(options, arguments, plugins);
    } else if (options.getValueAsBoolean(Option.DISASSEMBLE)) {
      return disassemble(options, arguments);
    } else if (options.getValueAsBoolean(Option.SYMBOLIC_EXECUTE)) {
      return symbolicExecute(options, arguments);
    } else {
      return translate(options, arguments);
    }
  }

  private static boolean translate(final Options options, final String[] arguments) {
    final TranslatorContext context = new TranslatorContext();
    for (final Translator<?> translator : translators) {
      if (!translator.translate(options, context, arguments)) {
        Logger.message("Translation was aborted.");
        return false;
      }
    }

    // Copies user-defined Java code to the output folder.
    if(!copyExtensions(options)) {
      Logger.error("Failed to copy extensions.");
      return false;
    }

    return true;
  }

  private static boolean copyExtensions(final Options options) {
    if (options.hasValue(Option.EXTDIR)) {
      final String extensionDir = options.getValueAsString(Option.EXTDIR);
      final File extensionDirFile = new File(extensionDir);

      if (!extensionDirFile.exists() || !extensionDirFile.isDirectory()) {
        Logger.error("The extension folder %s does not exists or is not a folder.", extensionDir);
        return false;
      }

      final String outDir = options.getValueAsString(Option.OUTDIR) + "/src/java";
      final File outDirFile = new File(outDir);

      try {
        FileUtils.copyDirectory(extensionDirFile, outDirFile);
        Logger.message("Copied %s to %s", extensionDir, outDir);
      } catch (final IOException e) {
        Logger.error("Failed to copy %s to %s", extensionDir, outDir);
        return false;
      }
    }

    return true;
  }

  private static boolean disassemble(final Options options, final String[] arguments) {
    if (!checkTwoArguments(arguments)) {
      return false;
    }

    final String modelName = arguments[0];
    final String inputFile = arguments[1];

    final boolean result = Disassembler.disassemble(options, modelName, inputFile);
    if (!result) {
      Logger.message("Disassembling was aborted.");
    }

    return result;
  }

  private static boolean symbolicExecute(final Options options, final String[] arguments) {
    if (!checkTwoArguments(arguments)) {
      return false;
    }

    final String modelName = arguments[0];
    final String inputFile = arguments[1];

    final boolean result = SymbolicExecutor.execute(options, modelName, inputFile);
    if (!result) {
      Logger.message("Symbolic execution was aborted.");
    }

    return result;
  }

  private static boolean generate(
      final Options options,
      final String[] arguments,
      final List<Plugin> plugins) throws Throwable {

    if (!checkTwoArguments(arguments)) {
      return false;
    }

    final String modelName = arguments[0];
    final String templateFile = arguments[1];

    GeneratorSettings settings = null;
    if (options.hasValue(Option.ARCH_DIRS)) {
      final String archDirs = options.getValueAsString(Option.ARCH_DIRS);
      final String[] archDirsArray = archDirs.split(":");

      boolean isFound = false;
      for (final String archDir : archDirsArray) {
        final String[] archDirArray = archDir.split("=");

        if (archDirArray != null && archDirArray.length > 1 && modelName.equals(archDirArray[0])) {
          final File archFile = new File(archDirArray[1]);

          final String archPath = archFile.isAbsolute() ? archDirArray[1] : String.format("%s%s%s",
              SysUtils.getHomeDir(), File.separator, archDirArray[1]); 

          settings = SettingsParser.parse(archPath);

          isFound = true;
          break;
        }
      }

      if (!isFound) {
        Logger.error("Failed to start generation. " +
                     "The --%s option does not contain path to settings for %s.",
                     Option.ARCH_DIRS.getName(), modelName);
        return false;
      }
    } else {
      Logger.error("Failed to start generation. The --%s option is not specified.",
                    Option.ARCH_DIRS.getName());
      return false;
    }

    final Statistics statistics =
        TestEngine.generate(options, settings, modelName, templateFile, plugins);

    if (null == statistics) {
      return false;
    }

    final long totalTime = statistics.getTotalTime();
    final long genTime = totalTime - statistics.getTimeMetric(Statistics.Activity.INITIALIZING);
    final long genRate = (1000 * statistics.getInstructions()) / genTime;

    Logger.message("Generation Statistics");
    Logger.message("Generation time: %s", Statistics.timeToString(genTime));
    Logger.message("Generation rate: %d instructions/second", genRate);

    Logger.message("Programs/stimuli/instructions: %d/%d/%d",
        statistics.getPrograms(), statistics.getSequences(), statistics.getInstructions());

    if (options.getValueAsBoolean(Option.TIME_STATISTICS)) {
      Logger.message(System.lineSeparator() + "Time Statistics");

      Logger.message("Total time: %s", Statistics.timeToString(totalTime));
      Logger.message(statistics.getTimeMetricText(Statistics.Activity.INITIALIZING));

      final long genPercentage = (genTime * 10000) / totalTime;
      Logger.message("Generation time: %s (%d.%d%%)",
          Statistics.timeToString(genTime), genPercentage / 100, genPercentage % 100);

      for (final Statistics.Activity activity : Statistics.Activity.values()) {
        if (activity != Statistics.Activity.INITIALIZING) {
          Logger.message("  " + statistics.getTimeMetricText(activity));
        }
      }
    }

    final long rateLimit = options.getValueAsInteger(Option.RATE_LIMIT);
    if (genRate < rateLimit && statistics.getInstructions() >= 1000) { 
      // Makes sense only for sequences of significant length (>= 1000)
      Logger.error("Generation rate is too slow. At least %d is expected.", rateLimit);
      return false;
    }

    return true;
  }

  private static boolean checkTwoArguments(final String[] arguments) {
    if (arguments.length == 2) {
      return true;
    }

    Logger.error("Wrong number of command-line arguments. Two are required.");
    Logger.message("Argument format: <model name>, <input file>");

    return false;
  }
}
