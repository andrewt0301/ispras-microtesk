/*
 * Copyright 2014-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.template;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.ExecutionException;
import ru.ispras.microtesk.model.InstructionCall;
import ru.ispras.microtesk.model.ProcessingElement;
import ru.ispras.microtesk.model.memory.LocationAccessor;
import ru.ispras.microtesk.model.memory.Section;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * The {@link ConcreteCall} class describes an instruction call with fixed arguments
 * which can be simulated. It also can hold objects are used by processing logic
 * to do some housekeeping job.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class ConcreteCall {
  private final List<Label> labels;
  private final List<LabelReference> labelRefs;
  private final List<Output> outputs;
  private final InstructionCall executable;
  private final boolean relativeOrigin;
  private final BigInteger origin;
  private final BigInteger alignment;
  private final BigInteger alignmentInBytes;
  private final DataSection data;

  private final List<LocationAccessor> addressRefs;
  private long address = 0;
  private final String text;
  private int executionCount = 0;
  private BigInteger originFromRelative = null;

  // TODO:
  public static ConcreteCall newText(final String text) {
    InvariantChecks.checkNotNull(text);
    return new ConcreteCall(AbstractCall.newText(text));
  }

  public static ConcreteCall newLine() {
    return new ConcreteCall(AbstractCall.newLine());
  }

  public static ConcreteCall newComment(final String comment) {
    InvariantChecks.checkNotNull(comment);
    return new ConcreteCall(AbstractCall.newComment(comment));
  }

  public ConcreteCall(
      final AbstractCall abstractCall,
      final InstructionCall executable,
      final List<LabelReference> labelRefs,
      final List<LocationAccessor> addressRefs) {
    InvariantChecks.checkNotNull(abstractCall);
    InvariantChecks.checkNotNull(executable);
    InvariantChecks.checkNotNull(labelRefs);
    InvariantChecks.checkNotNull(addressRefs);

    this.text = abstractCall.getText();
    this.labels = Label.copyAll(abstractCall.getLabels());
    this.labelRefs = labelRefs;
    this.outputs = abstractCall.getOutputs();
    this.executable = executable;
    this.relativeOrigin = abstractCall.isRelativeOrigin();
    this.origin = abstractCall.getOrigin();
    this.alignment = abstractCall.getAlignment();
    this.alignmentInBytes = abstractCall.getAlignmentInBytes();
    this.data = null;
    this.addressRefs = addressRefs;
  }

  public ConcreteCall(final AbstractCall abstractCall) {
    InvariantChecks.checkNotNull(abstractCall);

    this.text = abstractCall.getText();
    this.labels = Label.copyAll(abstractCall.getLabels());
    this.labelRefs = abstractCall.getLabelReferences();
    this.outputs = abstractCall.getOutputs();
    this.executable = null;
    this.relativeOrigin = abstractCall.isRelativeOrigin();
    this.origin = abstractCall.getOrigin();
    this.alignment = abstractCall.getAlignment();
    this.alignmentInBytes = abstractCall.getAlignmentInBytes();
    this.data = abstractCall.hasData() ? new DataSection(abstractCall.getData()) : null;
    this.addressRefs = Collections.emptyList();
  }

  public ConcreteCall(final InstructionCall executable) {
    InvariantChecks.checkNotNull(executable);

    this.text = null;
    this.labels = Collections.<Label>emptyList();
    this.labelRefs = Collections.<LabelReference>emptyList();
    this.outputs = Collections.<Output>emptyList();
    this.executable = executable;
    this.relativeOrigin = false;
    this.origin = null;
    this.alignment = null;
    this.alignmentInBytes = null;
    this.data = null;
    this.addressRefs = Collections.emptyList();
  }

  public boolean isExecutable() {
    return null != executable;
  }

  public InstructionCall getExecutable() {
    return executable;
  }

  /**
   * Checks whether the instruction call corresponds to a printable instruction
   * (executable instruction or pseudo instruction). This method is used to
   * calculate statistics on instruction number.
   *
   * @return {@code true} if the call corresponds to a printable instruction or
   *         {@code false} if it is used to housekeeping purposes.
   */
  public boolean isInstruction() {
    return isExecutable() || text != null;
  }

  /**
   * Executes the instruction call on the specified processing element.
   *
   * @param processingElement Processing element instance to be used for execution.
   * @return exception name if was interrupted.
   */
  public String execute(final ProcessingElement processingElement) {
    if (!isExecutable()) {
      return null;
    }

    try {
      ++executionCount;
      executable.execute(processingElement);
    } catch (final ExecutionException e) {
      return e.getMessage();
    }

    return null;
  }

  public int getExecutionCount() {
    return executionCount;
  }

  public void resetExecutionCount() {
    executionCount = 0;
  }

  public String getText() {
    return isExecutable() ? executable.getText() : text;
  }

  public String getImage() {
    return null != executable ? executable.getImage() : "";
  }

  public List<Label> getLabels() {
    return labels;
  }

  public List<LabelReference> getLabelReferences() {
    return labelRefs;
  }

  public List<Output> getOutputs() {
    return outputs;
  }

  public int getByteSize() {
    if (!isExecutable()) {
      return 0;
    }

    return executable.getByteSize();
  }

  public long getAddress() {
    return address;
  }

  public long setAddress(final Section section,  final long value) {
    long thisAddress = value;

    if (origin != null) {
      if (relativeOrigin) {
        thisAddress = value + origin.longValue();
        originFromRelative = section.virtualToOrigin(BigInteger.valueOf(thisAddress));
      } else {
        thisAddress = section.virtualFromOrigin(origin).longValue();
      }
    }

    if (alignmentInBytes != null) {
      final long alignmentLength = alignmentInBytes.longValue();
      final long unalignedLength = thisAddress % alignmentLength;
      if (0 != unalignedLength) {
        thisAddress = thisAddress + (alignmentLength - unalignedLength);
      }
    }

    this.address = thisAddress;
    for (final LocationAccessor locationAccessor : addressRefs) {
      locationAccessor.setValue(BigInteger.valueOf(address));
    }

    return thisAddress + getByteSize();
  }

  public BigInteger getOrigin() {
    if (!relativeOrigin) {
      return origin;
    }

    InvariantChecks.checkNotNull(relativeOrigin, "Relative origin is not calculated!");
    return originFromRelative;
  }

  public BigInteger getAlignment() {
    return alignment;
  }

  public DataSection getData() {
    return data;
  }
}
