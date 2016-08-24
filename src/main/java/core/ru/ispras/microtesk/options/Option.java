/*
 * Copyright 2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.options;

import java.util.HashSet;
import java.util.Set;

import ru.ispras.fortress.util.InvariantChecks;

public enum Option {
  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Common flags

  HELP("help", "Shows help message", false),
  VERBOSE("verbose", "Enables printing diagnostic messages", false),
  TRANSLATE("translate", "Translates formal specifications", false, null, "task"),
  GENERATE("generate", "Generates test programs", false, null, "task"),
  OUTDIR("output-dir", "Sets where to place generated files", "./output"),

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Translator Options

  INCLUDE("include", "Sets include files directories", "", TRANSLATE),
  EXTDIR("extension-dir", "Sets directory that stores user-defined Java code", "", TRANSLATE),

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Test Program Generation Options

  RANDOM("random-seed", "Sets seed for randomizer", 0, GENERATE),
  SOLVER("solver", "Sets constraint solver engine to be used", "cvc4"),
  LIMIT("branch-exec-limit", "Sets the limit on jumps to detect endless loops", 100, GENERATE),
  SOLVER_DEBUG("solver-debug", "Enables debug mode for SMT solvers", false, GENERATE),
  TARMAC_LOG("tarmac-log", "Saves simulator log in Tarmac format", false, GENERATE),
  SELF_CHECKS("self-checks", "Inserts self-checking code into test programs", false, GENERATE),
  DEFAULT_TEST_DATA("default-test-data", "Enables generation of default test data", false, GENERATE),
  ARCH_DIRS("arch-dirs", "Home directories for tested architectures", "", GENERATE),
  RATE_LIMIT("rate-limit", "Generation rate limit, causes error when exceeded", 0, GENERATE),
  NO_SIMULATION("no-simulation", "Disables simulation of generated code", false, GENERATE),
  TIME_STATISTICS("time-statistics", "Enables printing time statistics", false, GENERATE),

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Test Program Generation Options (File Creation)

  CODE_EXT("code-file-extension", "The output file extension", "asm", GENERATE),
  CODE_PRE("code-file-prefix", "The output file prefix (file name format is prefix_xxxx.ext)",
      "test", GENERATE),
  DATA_EXT("data-file-extension", "The data file extension", "dat", GENERATE),
  DATA_PRE("data-file-prefix", "The data file prefix", "asm", GENERATE),
  EXCEPT_PRE("exception-file-prefix", "The exception handler file prefix", "test_except", GENERATE),
  CODE_LIMIT("program-length-limit", "The maximum number of instructions in output programs",
      1000, GENERATE),
  TRACE_LIMIT("trace-length-limit", "The maximum length of execution traces of output programs",
      1000, GENERATE),
  COMMENTS_ENABLED("comments-enabled", "Enables printing comments; if not specified no comments " +
      "are printed", false, GENERATE),
  COMMENTS_DEBUG("comments-debug", "Enables printing detailed comments, must be used together " + 
      "with --" + COMMENTS_ENABLED.getName(), false, GENERATE);

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private static class Static {
    private static final Set<String> NAMES = new HashSet<>();
    private static final Set<String> SHORT_NAMES = new HashSet<>();
  }

  private final String name;
  private final String shortName;
  private final String description;
  private final Object defaultValue;
  private final Option dependency;
  private final String groupName;

  private Option(
      final String name,
      final String description,
      final Object defaultValue) {
    this(name, description, defaultValue, null);
  }

  private Option(
      final String name,
      final String description,
      final Object defaultValue,
      final Option dependency) {
    this(name, description, defaultValue, dependency, null);
  }

  private Option(
      final String name,
      final String description,
      final Object defaultValue,
      final Option dependency,
      final String groupName) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(description);

    if (Static.NAMES.contains(name)) {
      throw new IllegalArgumentException(String.format("--%s is already used!", name));
    }

    this.name = name;
    this.shortName = makeUniqueShortName(name);

    this.description = description;
    this.defaultValue = defaultValue;
    this.dependency = dependency;
    this.groupName = groupName; 

    Static.SHORT_NAMES.add(shortName);
    Static.NAMES.add(name);
  }

  private static String makeUniqueShortName(final String name) {
    final String[] nameTokens = name.split("-");

    final StringBuilder sb = new StringBuilder();
    for (final String token : nameTokens) {
      sb.append(token.charAt(0));
    }

    while (Static.SHORT_NAMES.contains(sb.toString())) {
      final String lastToken = nameTokens[nameTokens.length - 1];
      sb.append(lastToken.charAt(0));
    }

    return sb.toString();
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public String getDescription() {
    final StringBuilder sb = new StringBuilder(description);

    if (null != dependency) {
      sb.append(String.format(" [works with --%s]", dependency.getShortName()));
    }

    sb.append(", default=");
    sb.append(defaultValue instanceof String ? "\"" + defaultValue + "\"" : defaultValue);

    return sb.toString();
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public Class<?> getValueClass() {
    return defaultValue.getClass();
  }

  public Option getDependency() {
    return dependency;
  }

  public String getGroupName() {
    return groupName;
  }

  public boolean isFlag() {
    return defaultValue instanceof Boolean;
  }
}