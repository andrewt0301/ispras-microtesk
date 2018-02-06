/*
 * Copyright 2016-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.tools.symexec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.solver.engine.smt.Cvc4Solver;
import ru.ispras.fortress.solver.engine.smt.SmtTextBuilder;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.SysUtils;
import ru.ispras.microtesk.tools.Disassembler;
import ru.ispras.microtesk.tools.Disassembler.Output;
import ru.ispras.microtesk.model.IsaPrimitive;
import ru.ispras.microtesk.model.Model;
import ru.ispras.microtesk.model.ProcessingElement;
import ru.ispras.microtesk.model.TemporaryVariables;
import ru.ispras.microtesk.model.memory.Memory;
import ru.ispras.microtesk.options.Options;

public final class SymbolicExecutor {
  private SymbolicExecutor() {}

  public static boolean execute(
      final Options options,
      final String modelName,
      final String fileName) {
    InvariantChecks.checkNotNull(options);
    InvariantChecks.checkNotNull(modelName);
    InvariantChecks.checkNotNull(fileName);

    Logger.message("Analyzing file: %s...", fileName);
    final DisassemblerOutputFactory outputFactory = new DisassemblerOutputFactory();
    if (!Disassembler.disassemble(options, modelName, fileName, outputFactory)) {
      Logger.error("Failed to disassemble " + fileName);
    }

    final DisassemblerOutput output = outputFactory.getOutput();
    InvariantChecks.checkNotNull(output);

    final List<IsaPrimitive> instructions = output.getInstructions();
    InvariantChecks.checkNotNull(instructions);

    final List<Node> ssa = FormulaBuilder.buildFormulas(modelName, instructions);

    final String smtFileName = fileName + ".smt2";
    writeSmt(smtFileName, ssa);
    createMapping(modelName, instructions);

    Logger.message("Created file: %s", smtFileName);
    return true;
  }

  private static void writeSmt(
    final String fileName,
    final Collection<? extends Node> formulas) {
    final Cvc4Solver solver = new Cvc4Solver();

    try {
      SmtTextBuilder.saveToFile(
          fileName,
          Collections.<String>emptyList(),
          formulas,
          solver.getOperations()
          );
    } catch (final java.io.IOException e) {
      Logger.error(e.getMessage());
    }
  }

  private static void createMapping(final String modelName, final List<IsaPrimitive> code) {
    final Model model = loadModel(modelName);
    if (model == null) {
      return;
    }
    final ProcessingElement pe = model.getPeFactory().create();
    final List<String> entries = new ArrayList<>();
    for (final Memory unit : pe.getStorages().values()) {
      if (unit.getKind() == Memory.Kind.REG || unit.getKind() == Memory.Kind.MEM) {
        final String type = unit.getKind().toString().toLowerCase();
        final int card = unit.getLength().intValue();
        final int size = unit.getType().getBitSize();

        final String entry = String.format("%s,%s,%s,%d,%s,%s",
          unit.getName(), type, (card > 1) ? card : "", size, "NYI", "NYI");

        entries.add(entry);
      }
    }
  }

  private static Model loadModel(final String modelName) {
    try {
      return SysUtils.loadModel(modelName);
    } catch (final Exception e) {
      Logger.error("Failed to load the %s model. Reason: %s.", modelName, e.getMessage());
      return null;
    }
  }

  private final static class DisassemblerOutput implements Disassembler.Output {
    private final TemporaryVariables tempVars;
    private final List<IsaPrimitive> instructions;

    private DisassemblerOutput(final TemporaryVariables tempVars) {
      InvariantChecks.checkNotNull(tempVars);

      this.tempVars = tempVars;
      this.instructions = new ArrayList<>();
    }

    @Override
    public void add(final IsaPrimitive primitive) {
      final String text = primitive.text(tempVars);
      Logger.debug(text);
      instructions.add(primitive);
    }

    @Override
    public void close() {
      // Nothing
    }

    public List<IsaPrimitive> getInstructions() {
      return instructions;
    }
  }

  private final static class DisassemblerOutputFactory implements Disassembler.OutputFactory {
    private DisassemblerOutput output = null;

    @Override
    public Output createOutput(final Model model) {
      InvariantChecks.checkNotNull(model);
      final TemporaryVariables tempVars = model.getTempVars();
      output = new DisassemblerOutput(tempVars);
      return output;
    }

    public DisassemblerOutput getOutput() {
      return output;
    }
  }
}
