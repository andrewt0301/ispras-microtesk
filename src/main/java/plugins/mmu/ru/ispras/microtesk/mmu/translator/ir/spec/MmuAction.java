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

package ru.ispras.microtesk.mmu.translator.ir.spec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.mmu.basis.MemoryAccessStack;

/**
 * {@link MmuAction} describes an action, i.e. a named set of assignments.
 * 
 * @author <a href="mailto:protsenko@ispras.ru">Alexander Protsenko</a>
 */
public final class MmuAction {
  /** Unique name. */
  private final String name;
  /** Buffer used in the action or {@code null}. */
  private final MmuBufferAccess bufferAccess;
  /** Assignments performed by the action. */
  private final Map<IntegerField, MmuBinding> action = new HashMap<>();
  /** Marks associated with the action. */
  private final Set<String> marks = new LinkedHashSet<>();

  public MmuAction(
      final String name,
      final MmuBufferAccess bufferAccess,
      final MmuBinding... assignments) {
    InvariantChecks.checkNotNull(name);
    // The buffer access is allowed to be null.

    this.name = name;
    this.bufferAccess = bufferAccess;

    for (final MmuBinding assignment : assignments) {
      action.put(assignment.getLhs(), assignment);
    }
  }

  public MmuAction(final String name, final MmuBinding... assignments) {
    this(name, null, assignments);
  }

  public MmuAction(final String name,
      final MmuBufferAccess bufferAccess,
      final MmuStruct lhs,
      final MmuStruct rhs) {
    InvariantChecks.checkNotNull(name);
    // The buffer access is allowed to be null.
    InvariantChecks.checkNotNull(lhs);
    InvariantChecks.checkNotNull(rhs);

    this.name = name;
    this.bufferAccess = bufferAccess;

    for (final MmuBinding assignment : lhs.bindings(rhs)) {
      action.put(assignment.getLhs(), assignment);
    }
  }

  public MmuAction(final String name, final MmuStruct lhs, final MmuStruct rhs) {
    this(name, null, lhs, rhs);
  }

  public String getName() {
    return name;
  }

  public MmuBufferAccess getBufferAccess(final MemoryAccessStack stack) {
    InvariantChecks.checkNotNull(stack);

    if (bufferAccess == null || stack.isEmpty()) {
      return bufferAccess;
    }

    return bufferAccess.getInstance(stack);
  }

  public Map<IntegerField, MmuBinding> getAction(final MemoryAccessStack stack) {
    InvariantChecks.checkNotNull(stack);

    if (stack.isEmpty()) {
      return action;
    }

    final Map<IntegerField, MmuBinding> actionInstance = new LinkedHashMap<>();

    for (final Map.Entry<IntegerField, MmuBinding> entry : action.entrySet()) {
      final IntegerField lhs = entry.getKey();
      final MmuBinding rhs = entry.getValue();

      actionInstance.put(stack.getInstance(lhs), rhs.getInstance(stack));
    }

    return actionInstance;
  }

  public void addMark(final String mark) {
    InvariantChecks.checkNotNull(mark);
    marks.add(mark);
  }

  public Set<String> getMarks() {
    return marks;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || !(o instanceof MmuAction)) {
      return false;
    }

    final MmuAction r = (MmuAction) o;
    return name.equals(r.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append(name);

    if (!marks.isEmpty()) {
      builder.append(", ");
      builder.append(String.format("marks: %s", marks));
    }

    if (bufferAccess != null) {
      builder.append(", ");
      builder.append(String.format("buffer: %s", bufferAccess.getBuffer()));
    }

    /*
    if (!action.isEmpty()) {
      builder.append(", ");
      builder.append(String.format("action: %s", action.values()));
    }
    */

    return builder.toString();
  }
}
