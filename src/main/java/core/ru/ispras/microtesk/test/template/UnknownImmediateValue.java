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

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.randomizer.Randomizer;

import ru.ispras.microtesk.model.data.Type;
import ru.ispras.microtesk.model.data.TypeId;
import ru.ispras.microtesk.test.engine.allocator.AllocationData;
import ru.ispras.microtesk.utils.SharedObject;

import java.math.BigInteger;

/**
 * The {@link UnknownImmediateValue} class describes an unknown immediate value to be specified as
 * an argument of an addressing mode or operation. A corresponding concrete value must be produced
 * as a result of test data generation for some test situation linked to the primitive (MODE or OP)
 * this unknown value is passed to an argument. The generated concrete value is assigned to the
 * object via the {@code setValue} method.
 *
 * <p>N.B. The value can be assigned only once, otherwise an exception will be raised. This is
 * done to avoid misuse of the class. For example, when several MODE or OP object hold a reference
 * to the same unknown value object the concrete value must be generated and assigned only once.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class UnknownImmediateValue extends SharedObject<UnknownImmediateValue>
                                         implements Value {
  private final AllocationData<Value> allocationData;
  private Type type;
  private BigInteger value;
  private BigInteger defaultValue;

  protected UnknownImmediateValue() {
    this(new AllocationData<Value>());
  }

  protected UnknownImmediateValue(final AllocationData<Value> allocationData) {
    this.allocationData = allocationData;
    this.type = null;
    this.value = null;
    this.defaultValue = null;
  }

  protected UnknownImmediateValue(final UnknownImmediateValue other) {
    super(other);

    this.allocationData = new AllocationData<Value>(other.allocationData);
    this.type = other.type;
    this.value = other.value;
    this.defaultValue = other.defaultValue;
  }

  @Override
  public UnknownImmediateValue newCopy() {
    return new UnknownImmediateValue(this);
  }

  @Override
  public Value copy() {
    return newCopy();
  }

  public AllocationData<Value> getAllocationData() {
    return allocationData;
  }

  public boolean isValueSet() {
    return value != null;
  }

  @Override
  public BigInteger getValue() {
    if (isValueSet()) {
      return value;
    }

    if (defaultValue == null) {
      if (null == type) {
        throw new IllegalStateException("Cannot create value. Type is unknown.");
      }

      final BitVector data = BitVector.newEmpty(type.getBitSize());
      Randomizer.get().fill(data);
      defaultValue = data.bigIntegerValue(type.getTypeId() == TypeId.INT);
    }

    return defaultValue;
  }

  protected void setType(final Type type) {
    this.type = type;
  }

  public void setValue(final BigInteger value) {
    if (isValueSet()) {
      throw new IllegalStateException("Value is already set.");
    }
    this.value = value;
  }

  @Override
  public String toString() {
    if (null != value) {
      return value.toString();
    }

    if (null != defaultValue) {
      return defaultValue.toString() + " (random)";
    }

    return "UnknownImmediateValue";
  }
}
