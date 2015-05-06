/*
 * Copyright 2012-2015 ISP RAS (http://www.ispras.ru)
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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ru.ispras.microtesk.test.TestProgramGenerator;
import ru.ispras.microtesk.translator.Translator;
import ru.ispras.microtesk.translator.nml.SimnMLAnalyzer;
import ru.ispras.microtesk.utils.FileUtils;

public final class MicroTESK {
  private MicroTESK() {}

  private static class Parameters {
    public static final String INCLUDE = "i";
    public static final String HELP = "h";
    public static final String OUTDIR = "d";
    public static final String GENERATE = "g";
    public static final String TRANSLATE = "t";
    public static final String VERBOSE = "v";
    public static final String RANDOM = "r";
    public static final String SOLVER = "s";

    private static final Options options = newOptions();

    private static Options newOptions() {
      final String TOPT = " [works with -t]";
      final String GOPT = " [works with -g]";

      final Options result = new Options();
      result.addOption(HELP, "help", false, "Shows this message");

      final OptionGroup actions = new OptionGroup(); 
      actions.addOption(new Option(GENERATE, "generate", false, "Generates test programs"));
      actions.addOption(new Option(TRANSLATE, "translate", false, "Translates formal specifications"));
      result.addOptionGroup(actions);

      result.addOption(INCLUDE, "include", true, "Sets include files directories" + TOPT);
      result.addOption(OUTDIR, "dir", true, "Sets where to place generated files" + TOPT);
      result.addOption(RANDOM, "random", true, "Sets seed for randomizer" + GOPT);
      result.addOption(SOLVER, "solver", true, "Sets constraint solver engine to be used" + GOPT);

      result.addOption(VERBOSE, "verbose", false, "Enables printing diagnostic messages");
      return result;
    }

    private Parameters() {}

    public static CommandLine parse(String[] args) throws ParseException {
      final CommandLineParser parser = new GnuParser();
      return parser.parse(options, args);
    }

    public static void help() {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(80, "[options] Files to be processed", "", options, "");
    }
  }

  public static void main(String[] args) {
    final CommandLine params;

    try {
      params = Parameters.parse(args);
    } catch (ParseException e) {
      Logger.error("Wrong command line: " + e.getMessage());
      Parameters.help();
      return;
    }

    if (params.hasOption(Parameters.HELP)) {
      Parameters.help();
      return;
    }

    if (params.hasOption(Parameters.VERBOSE)) {
      Logger.setDebug(true);
      return;
    }

    try {
      if (params.hasOption(Parameters.GENERATE)) {
        generate(params);
      } else {
        translate(params);
      }
    } catch (Throwable e) {
      Logger.exception(e);
    }
  }

  private static void translate(CommandLine params) throws RecognitionException {
    // System.out.println(load("ru.ispras.microtesk.translator.nml.SimnMLAnalyzer"));
    // System.out.println(load("ru.ispras.microtesk.translator.mmu.MmuTranslator"));

    final Translator<?> translator = new SimnMLAnalyzer();

    if (params.hasOption(Parameters.INCLUDE)) {
      translator.addPath(params.getOptionValue(Parameters.INCLUDE));
    }

    if (params.hasOption(Parameters.OUTDIR)) {
      translator.setOutDir(params.getOptionValue(Parameters.OUTDIR));
    }

    translator.start(params.getArgs());
  }

  private static void generate(CommandLine params) {
    final TestProgramGenerator generator = new TestProgramGenerator();

    if (params.hasOption(Parameters.RANDOM)) {
      final String random = params.getOptionValue(Parameters.RANDOM);
      try {
        final int seed = Integer.parseInt(random);
        generator.setRandomSeed(seed);
      } catch (NumberFormatException e) {
        Logger.warning("Failed to parse the value of the -r parameter.");
      }
    }

    if (params.hasOption(Parameters.SOLVER)) {
      generator.setSolver(params.getOptionValue(Parameters.SOLVER));
    }

    final String[] args = params.getArgs();
    if (args.length < 2) {
      Logger.error("Wrong number of generator arguments. At least two are required.");
      Logger.message("Argument format: <model name>, <template files>[, <output file>]");
      return;
    }

    final String modelName = args[0];
    generator.setModelName(modelName);

    final List<String> templateFiles = new ArrayList<>();
    for (int index = 1; index < args.length; ++index) {
      final String fileName = args[index];
      if (".rb".equals(FileUtils.getFileExtension(fileName))) {
        templateFiles.add(fileName);
      } else if (index == args.length - 1){
        generator.setFileName(fileName);
      }
    }

    generator.generate(templateFiles);
  }
}
