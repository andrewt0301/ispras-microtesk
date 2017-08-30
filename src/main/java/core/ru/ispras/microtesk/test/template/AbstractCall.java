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

package ru.ispras.microtesk.test.template;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.utils.SharedObject;

public final class AbstractCall {
  private final Where where;
  private final String text;
  private final Primitive rootOperation;
  private final Map<String, Object> attributes;

  private final List<Label> labels;
  private final List<LabelReference> labelRefs;
  private final List<Output> outputs;

  private final boolean relativeOrigin;
  private final BigInteger origin;
  private final BigInteger alignment;
  private final BigInteger alignmentInBytes;

  private final PreparatorReference preparatorReference;
  private final DataSection data;
  private final List<AbstractCall> atomicSequence;
  private final Primitive modeToFree;
  private final boolean freeAllModes;

  public static AbstractCall newData(final DataSection data) {
    InvariantChecks.checkNotNull(data);

    return new AbstractCall(
        null,
        null,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.<Output>emptyList(),
        false,
        null,
        null,
        null,
        null,
        data,
        null,
        null,
        false
        );
  }

  // TODO:
  public static AbstractCall newText(final String text) {
    InvariantChecks.checkNotNull(text);

    return new AbstractCall(
        null,
        text,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.<Output>emptyList(),
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false
        );
  }

  public static AbstractCall newLine() {
    return newText("");
  }

  public static AbstractCall newComment(final String comment) {
    InvariantChecks.checkNotNull(comment);

    return new AbstractCall(
        null,
        null,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.singletonList(new Output(Output.Kind.COMMENT, comment)),
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false
        );
  }

  public static AbstractCall newOrigin(final BigInteger origin, final boolean isRelative) {
    InvariantChecks.checkNotNull(origin);

    return new AbstractCall(
        null,
        null,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.<Output>emptyList(),
        isRelative,
        origin,
        null,
        null,
        null,
        null,
        null,
        null,
        false
        );
  }

  public static AbstractCall newAtomicSequence(final List<AbstractCall> sequence) {
    InvariantChecks.checkNotNull(sequence);

    return new AbstractCall(
        null,
        null,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.<Output>emptyList(),
        false,
        null,
        null,
        null,
        null,
        null,
        expandAtomic(sequence),
        null,
        false
        );
  }

  public static List<AbstractCall> expandAtomic(final List<AbstractCall> sequence) {
    InvariantChecks.checkNotNull(sequence);

    final List<AbstractCall> result = new ArrayList<>();
    for (final AbstractCall call : sequence) {
      if (call.isAtomicSequence()) {
        result.addAll(call.getAtomicSequence());
      } else {
        result.add(call);
      }
    }

    return result;
  }

  public static AbstractCall newFreeAllocatedMode(final Primitive mode, final boolean freeAll) {
    InvariantChecks.checkNotNull(mode);
    InvariantChecks.checkTrue(mode.getKind() == Primitive.Kind.MODE);

    return new AbstractCall(
        null,
        null,
        null,
        Collections.<Label>emptyList(),
        Collections.<LabelReference>emptyList(),
        Collections.<Output>emptyList(),
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        mode,
        freeAll
        );
  }

  public AbstractCall(
      final Where where,
      final String text,
      final Primitive rootOperation,
      final List<Label> labels,
      final List<LabelReference> labelRefs,
      final List<Output> outputs,
      final boolean relativeOrigin,
      final BigInteger origin,
      final BigInteger alignment,
      final BigInteger alignmentInBytes,
      final PreparatorReference preparatorReference,
      final DataSection data,
      final List<AbstractCall> atomicSequence,
      final Primitive modeToFree,
      final boolean freeAllModes) {
    InvariantChecks.checkNotNull(labels);
    InvariantChecks.checkNotNull(labelRefs);
    InvariantChecks.checkNotNull(outputs);

    // Both either null or not null
    InvariantChecks.checkTrue((null == alignment) == (null == alignmentInBytes));

    // Both cannot be not null. A call cannot be both an instruction and a preparator invocation.
    InvariantChecks.checkTrue((null == rootOperation) || (null == preparatorReference));

    this.where = where;
    this.text = text;
    this.rootOperation = rootOperation;
    this.attributes = new LinkedHashMap<>();
    this.labels = labels; // Modifiable to allow adding labels.
    this.labelRefs = Collections.unmodifiableList(labelRefs);
    this.outputs = Collections.unmodifiableList(outputs);

    this.relativeOrigin = relativeOrigin;
    this.origin = origin;
    this.alignment = alignment;
    this.alignmentInBytes = alignmentInBytes;

    this.preparatorReference = preparatorReference;
    this.data = data;
    this.atomicSequence = atomicSequence;
    this.modeToFree = modeToFree;
    this.freeAllModes = freeAllModes;
  }

  public AbstractCall(final AbstractCall other) {
    InvariantChecks.checkNotNull(other);

    this.where = other.where;
    this.text = other.text;
    this.rootOperation = null != other.rootOperation ? other.rootOperation.newCopy() : null;
    this.attributes = new LinkedHashMap<>(other.attributes);

    this.labels = Label.copyAll(other.labels);
    this.labelRefs = LabelReference.copyAll(other.labelRefs);
    this.outputs = Output.copyAll(other.outputs);

    this.relativeOrigin = other.relativeOrigin;
    this.origin = other.origin;
    this.alignment = other.alignment;
    this.alignmentInBytes = other.alignmentInBytes;

    this.preparatorReference = null != other.preparatorReference ?
        new PreparatorReference(other.preparatorReference) : null;

    this.data = null != other.data ? new DataSection(other.data) : null;

    this.atomicSequence = null != other.atomicSequence ?
        copyAll(other.atomicSequence) : null;

    this.modeToFree = null != other.modeToFree ?
       (Primitive)((SharedObject<?>) other.modeToFree).getCopy() : null;

    this.freeAllModes = other.freeAllModes;
  }

  public static List<AbstractCall> copyAll(final List<AbstractCall> calls) {
    InvariantChecks.checkNotNull(calls);

    if (calls.isEmpty()) {
      return Collections.emptyList();
    }

    final List<AbstractCall> result = new ArrayList<>(calls.size());
    for (final AbstractCall call : calls) {
      result.add(new AbstractCall(call));
    }

    SharedObject.freeSharedCopies();
    return result;
  }

  public boolean isExecutable() {
    return null != rootOperation;
  }

  public boolean isPreparatorCall() {
    return null != preparatorReference;
  }

  public boolean isEmpty() {
    return null == text        &&
           !isExecutable()     &&
           !isPreparatorCall() &&
           !hasData()          &&
           !isAtomicSequence() &&
           !isModeToFree()     &&
           labels.isEmpty()    &&
           outputs.isEmpty()   &&
           null == origin      &&
           null == alignment;
  }

  public Where getWhere() {
    return where;
  }

  public String getText() {
    return text;
  }

  public Primitive getRootOperation() {
    return rootOperation;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public List<Primitive> getCommands() {
    return getCommands(rootOperation);
  }

  private static List<Primitive> getCommands(final Primitive primitive) {
    if (null == primitive) {
      return Collections.emptyList();
    }

    boolean isCommand = true;
    final List<Primitive> commands = new ArrayList<>();

    for (final Argument argument : primitive.getArguments().values()) {
      if (argument.getKind() == Argument.Kind.OP) {
        final Primitive argumentPrimitive = (Primitive) argument.getValue();
        commands.addAll(getCommands(argumentPrimitive));
        isCommand = false;
      }
    }

    return isCommand ? Collections.singletonList(primitive) : commands;
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

  public Label getTargetLabel() {
    if (labelRefs.isEmpty()) {
      return null;
    }

    final LabelReference reference = labelRefs.get(0);
    if (null == reference) {
      return null;
    }

    return reference.getReference();
  }

  public boolean isRelativeOrigin() {
    return relativeOrigin;
  }

  public BigInteger getOrigin() {
    return origin;
  }

  public BigInteger getAlignment() {
    return alignment;
  }

  public BigInteger getAlignmentInBytes() {
    return alignmentInBytes;
  }

  public PreparatorReference getPreparatorReference() {
    return preparatorReference;
  }

  public boolean hasData() {
    return null != data;
  }

  public DataSection getData() {
    return data;
  }

  public boolean isAtomicSequence() {
    return null != atomicSequence;
  }

  public List<AbstractCall> getAtomicSequence() {
    return atomicSequence;
  }

  public boolean isModeToFree() {
    return null != modeToFree;
  }

  public boolean isFreeAllModes() {
    return freeAllModes;
  }

  public Primitive getModeToFree() {
    return modeToFree;
  }

  @Override
  public String toString() {
    return String.format(
        "instruction call %s" + 
        "(root: %s, " +
        "preparator: %s, data: %b, atomic: %b, modeToFree: %s)",
        null != text ? text : "", 
        isExecutable() ? rootOperation.getName() : "null",
        isPreparatorCall() ? preparatorReference : "null",
        hasData(),
        isAtomicSequence(),
        isModeToFree() ? modeToFree.getName() : "null"
        );
  }
}
