/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.translator.ir;

import ru.ispras.fortress.util.InvariantChecks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Ir {
  private final String modelName;
  private final Map<String, Constant> constants;
  private final Map<String, Var> externs;
  private final Map<String, Address> addresses;
  private final Map<String, Segment> segments;
  private final Map<String, Buffer> buffers;
  private final Map<String, Memory> memories;
  private final Map<String, Type> types;
  private final Map<String, Callable> functions;
  private final Map<String, Operation> operations;

  public Ir(final String modelName) {
    InvariantChecks.checkNotNull(modelName);
    this.modelName = modelName;

    this.constants = new LinkedHashMap<>();
    this.externs = new LinkedHashMap<>();
    this.addresses = new LinkedHashMap<>();
    this.segments = new LinkedHashMap<>();
    this.buffers = new LinkedHashMap<>();
    this.memories = new LinkedHashMap<>();
    this.types = new LinkedHashMap<>();
    this.functions = new LinkedHashMap<>();
    this.operations = new LinkedHashMap<>();
  }

  public String getModelName() {
    return modelName;
  }

  public Map<String, Constant> getConstants() {
    return Collections.unmodifiableMap(constants);
  }

  public Map<String, Var> getExterns() {
    return Collections.unmodifiableMap(externs);
  }

  public Map<String, Address> getAddresses() {
    return Collections.unmodifiableMap(addresses);
  }

  public Map<String, Segment> getSegments() {
    return Collections.unmodifiableMap(segments);
  }

  public Map<String, Buffer> getBuffers() {
    return Collections.unmodifiableMap(buffers);
  }

  public Map<String, Memory> getMemories() {
    return Collections.unmodifiableMap(memories);
  }

  public Map<String, Type> getTypes() {
    return Collections.unmodifiableMap(types);
  }

  public Map<String, Callable> getFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  public Map<String, Operation> getOperations() {
    return Collections.unmodifiableMap(operations);
  }

  public void addConstant(final Constant constant) {
    InvariantChecks.checkNotNull(constant);
    constants.put(constant.getId(), constant);
  }

  public void addExtern(final Var variable) {
    InvariantChecks.checkNotNull(variable);
    externs.put(variable.getName(), variable);
  }

  public void addAddress(final Address address) {
    InvariantChecks.checkNotNull(address);
    addresses.put(address.getId(), address);
  }

  public void addSegment(final Segment segment) {
    InvariantChecks.checkNotNull(segment);
    segments.put(segment.getId(), segment);
  }

  public void addBuffer(final Buffer buffer) {
    InvariantChecks.checkNotNull(buffer);
    buffers.put(buffer.getId(), buffer);
  }

  public void addMemory(final Memory memory) {
    InvariantChecks.checkNotNull(memory);
    memories.put(memory.getId(), memory);
  }

  public void addType(final Type type, final String name) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(name);
    types.put(name, type);
  }

  public void addType(final Type type) {
    InvariantChecks.checkNotNull(type);
    addType(type, type.getId());
  }

  public void addFunction(final Callable f) {
    InvariantChecks.checkNotNull(f);
    functions.put(f.getName(), f);
  }

  public void addOperation(final Operation operation) {
    InvariantChecks.checkNotNull(operation);
    operations.put(operation.getId(), operation);
  }

  @Override
  public String toString() {
    return String.format(
        "Mmu Ir(%s):%n constants=%s%n externals=%s%n addresses=%s%n segments=%s"
            + "%n buffers=%s%n memories=%s%n types=%s%n functions=%s%n operations=%s",
        modelName,
        mapToString(constants),
        mapToString(externs),
        mapToString(addresses),
        mapToString(segments),
        mapToString(buffers),
        mapToString(memories),
        mapToString(types),
        mapToString(functions),
        mapToString(operations)
        );
  }

  public static <U, V> String mapToString(final Map<U, V> map) {
    final StringBuilder builder = new StringBuilder();
    builder.append(String.format("{%n"));

    for (final Map.Entry<U, V> entry : map.entrySet()) {
      builder.append(String.format("%s = %s%n", entry.getKey(), entry.getValue()));
    }

    builder.append("}");
    return builder.toString();
  }
}
