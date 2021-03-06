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

package ru.ispras.microtesk.mmu.basis;

import ru.ispras.fortress.util.InvariantChecks;

/**
 * {@link MemoryAccessType} describes a memory access type, which is an operation (load or store) in
 * couple with a block size (byte, word, etc.).
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MemoryAccessType {
  public static MemoryAccessType LOAD(final MemoryDataType dataType) {
    return new MemoryAccessType(MemoryOperation.LOAD, dataType);
  }

  public static MemoryAccessType STORE(final MemoryDataType dataType) {
    return new MemoryAccessType(MemoryOperation.STORE, dataType);
  }

  public static final MemoryAccessType NONE =
      new MemoryAccessType(MemoryOperation.STORE, MemoryDataType.BYTE /* Does not matter */);

  private final MemoryOperation operation;
  private final MemoryDataType dataType;

  public MemoryAccessType(final MemoryOperation operation, final MemoryDataType dataType) {
    InvariantChecks.checkNotNull(operation);
    InvariantChecks.checkNotNull(dataType);

    this.operation = operation;
    this.dataType = dataType;
  }

  public MemoryAccessType(final MemoryDataType dataType) {
    InvariantChecks.checkNotNull(dataType);

    this.operation = null;
    this.dataType = dataType;
  }

  public MemoryOperation getOperation() {
    return operation;
  }

  public MemoryDataType getDataType() {
    return dataType;
  }

  @Override
  public int hashCode() {
    return operation.hashCode() * 31 + dataType.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || !(o instanceof MemoryAccessType)) {
      return false;
    }

    final MemoryAccessType r = (MemoryAccessType) o;
    return operation == r.operation && dataType == r.dataType;
  }

  @Override
  public String toString() {
    return String.format("%s[%s]", (operation != null ? operation.toString() : "ANY"), dataType);
  }
}
