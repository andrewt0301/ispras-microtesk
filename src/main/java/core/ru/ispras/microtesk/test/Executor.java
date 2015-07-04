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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.exception.ConfigurationException;
import ru.ispras.microtesk.model.api.memory.Memory;
import ru.ispras.microtesk.model.api.state.IModelStateObserver;
import ru.ispras.microtesk.model.api.tarmac.LogPrinter;
import ru.ispras.microtesk.model.api.tarmac.Record;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.microtesk.test.template.LabelReference;
import ru.ispras.microtesk.test.template.Output;

/**
 * The role of the Executor class is to execute (simulate) sequences of instruction calls (concrete
 * calls). It executes instruction by instruction, perform control transfers by labels (if needed)
 * and prints information about important events to the simulator log (currently, the console).
 * 
 * @author Andrei Tatarnikov
 */

final class Executor {
  private final IModelStateObserver observer;
  private final int branchExecutionLimit;
  private final LogPrinter logPrinter;

  private Map<String, List<ConcreteCall>> exceptionHandlers;
  private List<LabelReference> labelRefs;

  /**
   * Constructs an Executor object.
   * 
   * @param observer Model state observer to evaluate simulation-time outputs.
   * @param logExecution Specifies whether printing to the simulator log is enabled.
   * 
   * @throws IllegalArgumentException if the {@code observer} parameter is {@code null}.
   */

  public Executor(
      final IModelStateObserver observer,
      final int branchExecutionLimit,
      final LogPrinter logPrinter) {
    checkNotNull(observer);

    this.observer = observer;
    this.branchExecutionLimit = branchExecutionLimit;
    this.logPrinter = logPrinter;

    this.exceptionHandlers = null;
    this.labelRefs = null;
  }

  public void setExceptionHandlers(final Map<String, List<ConcreteCall>> handlers) {
    checkNotNull(handlers);
    this.exceptionHandlers = handlers;
  }

  /**
   * Executes the specified sequence of instruction calls (concrete calls) and prints information
   * about important events to the simulator log.
   * 
   * @param sequence Sequence of executable (concrete) instruction calls.
   * 
   * @throws IllegalArgumentException if the parameter is {@code null}.
   * @throws ConfigurationException if during the interaction with the microprocessor model an error
   *         caused by an invalid format of the request has occurred (typically, it happens when
   *         evaluating an {@link Output} object causes an invalid request to the model state
   *         observer).
   */

  public void executeSequence(
      final TestSequence sequence, final int sequenceIndex) throws ConfigurationException {
    Memory.setUseTempCopies(false);

    final List<ConcreteCall> prologue = sequence.getPrologue();
    if (!prologue.isEmpty()) {
      logText("Initialization:\r\n");
      executeSequence(sequence.getPrologue(), Label.NO_SEQUENCE_INDEX);
      logText("\r\nMain Code:\r\n");
    }

    executeSequence(sequence.getBody(), sequenceIndex);
  }

  private void executeSequence(
      final List<ConcreteCall> sequence, final int sequenceIndex) throws ConfigurationException {
    checkNotNull(sequence);

    // Remembers all labels defined by the sequence and their positions.
    final LabelManager labelManager = new LabelManager();
    // Call address is mapped to its index in the sequence.
    final Map<Long, Integer> addressMap = new HashMap<>(sequence.size());

    for (int index = 0; index < sequence.size(); ++index) {
      final ConcreteCall call = sequence.get(index);
      labelManager.addAllLabels(call.getLabels(), index, sequenceIndex);
      addressMap.put(call.getAddress(), index);
    }

    // Resolves all label references and patches the instruction call text accordingly.
    for (int index = 0; index < sequence.size(); ++index) {
      final ConcreteCall call = sequence.get(index);

      for (LabelReference labelRef : call.getLabelReferences()) {
        labelRef.resetTarget();

        final Label source = labelRef.getReference();
        final LabelManager.Target target = labelManager.resolve(source);

        final String uniqueName;
        final String searchPattern;

        if (null != target) {
          // For code labels
          uniqueName = target.getLabel().getUniqueName();
          labelRef.setTarget(target.getLabel(), target.getPosition());

          final long addr = sequence.get(target.getPosition()).getAddress();
          labelRef.getPatcher().setValue(BigInteger.valueOf(addr));

          searchPattern = String.format("<label>%d", addr);
        } else {
          // For data labels 
          uniqueName = source.getName();
          searchPattern = String.format("<label>%d", labelRef.getArgumentValue());
        }

        final String patchedText =  call.getText().replace(searchPattern, uniqueName);
        call.setText(patchedText);
      }

      // Kill all unused "<label>" markers.
      if (null != call.getText()) {
        call.setText(call.getText().replace("<label>", ""));
      }
    }

    int currentPos = 0;
    final int endPos = sequence.size();

    while (currentPos < endPos) {
      final ConcreteCall call = sequence.get(currentPos);

      if (branchExecutionLimit > 0 && call.getExecutionCount() >= branchExecutionLimit) {
        throw new GenerationAbortedException(String.format(
            "Instruction %s reached its limit on execution count (%d). " +
            "Probably, the program entered an endless loop. Generation was aborted.",
            call.getText(), branchExecutionLimit));
      }

      currentPos = executeCall(call, currentPos, addressMap);
    }
  }

  /**
   * Executes the specified instruction call (concrete call) and returns the position of the next
   * instruction call to be executed. Also, it prints the textual representation of the call and
   * debugging outputs linked to the call to the simulator log (if logging is enabled). If the
   * method fails to deal with a control transfer in a proper way it prints a warning message and
   * returns the position of the instruction call that immediately follows the current one.
   * 
   * @param call Instruction call to be executed.
   * @param currentPos Position of the current call.
   * @param addressMap Map of addresses that stores correspondences
   *        between addresses and instruction indexes in the sequence.
   * @return Position of the next instruction call to be executed.
   * 
   * @throws ConfigurationException if failed to evaluate an Output object associated with the
   *         instruction call.
   */

  private int executeCall(
      final ConcreteCall call,
      final int currentPos,
      final Map<Long, Integer> addressMap) throws ConfigurationException {

    logOutputs(call.getOutputs());
    logLabels(call.getLabels());

    // If the call is not executable (contains only attributes like
    // labels or outputs, but no "body"), continue to the next instruction.
    if (!call.isExecutable()) {
      return currentPos + 1;
    }

    logText(call.getText());
    final String exception = call.execute();

    TestEngine.STATISTICS.instructionExecutedCount++;
    if (logPrinter != null) {
      logPrinter.addRecord(Record.newInstruction(call));
    }

    if (null != exception) {
      if (exceptionHandlers == null) {
        Logger.error("No exception handlers are defined. " + MSG_HAVE_TO_CONTINUE);
        return currentPos + 1;
      }

      final List<ConcreteCall> handlerSequence = exceptionHandlers.get(exception);
      if (handlerSequence == null) {
        Logger.error("Exception handler for %s is not found. " + 
            MSG_HAVE_TO_CONTINUE, exception);
        return currentPos + 1;
      }

      executeSequence(handlerSequence, Label.NO_SEQUENCE_INDEX);
      return currentPos + 1;
    }

    // TODO: Use the address map to determine the jump target.

    // Saves labels to jump in case there is a branch delay slot.
    if (!call.getLabelReferences().isEmpty()) {
      labelRefs = call.getLabelReferences();
    }

    // TODO: Support instructions with 2+ labels (needs API)
    final int transferStatus = observer.getControlTransferStatus();

    // If there are no transfers, continue to the next instruction.
    if (0 == transferStatus) {
      return currentPos + 1;
    }

    if ((null == labelRefs) || labelRefs.isEmpty()) {
      logText(MSG_NO_LABEL_LINKED);
      return currentPos + 1;
    }

    final LabelReference reference = labelRefs.get(0);
    final LabelReference.Target target = reference.getTarget();

    // Resets labels to jump (they are no longer needed after being used).
    labelRefs = null;

    if (null == target) {
      logText(String.format(MSG_NO_LABEL_DEFINED, reference.getReference().getName()));
      return currentPos + 1;
    }

    logText("Jump to label: " + target.getLabel().getUniqueName());
    return target.getPosition();
  }

  /**
   * Evaluates and prints the collection of {@link Output} objects.
   * 
   * @param o List of {@link Output} objects.
   * 
   * @throws IllegalArgumentException if the parameter is {@code null}.
   * @throws ConfigurationException if failed to evaluate the information in an Output
   *         object due to an incorrect request to the model state observer.
   */

  private void logOutputs(
      final List<Output> outputs) throws ConfigurationException {
    checkNotNull(outputs);

    for (final Output output : outputs) {
      if (output.isRuntime()) {
        logText(output.evaluate(observer));
      }
    }
  }
  
  private void logLabels(final List<Label> labels) {
    checkNotNull(labels);
    for (final Label label : labels) {
      logText(label.getUniqueName() + ":");
    }
  }

  /**
   * Prints the text to the simulator log if logging is enabled.
   * 
   * @param text Text to be printed.
   */

  private void logText(final String text) {
    if (text != null) {
      Logger.debug(text);
    }
  }

  private static final String MSG_HAVE_TO_CONTINUE =
    "Have to continue to the next instruction.";

  private static final String MSG_NO_LABEL_LINKED =
    "Warning: No label to jump is linked to the current instruction. " + MSG_HAVE_TO_CONTINUE;

  private static final String MSG_NO_LABEL_DEFINED =
    "Warning: No label called %s is defined in the current sequence. " + MSG_HAVE_TO_CONTINUE;
}
