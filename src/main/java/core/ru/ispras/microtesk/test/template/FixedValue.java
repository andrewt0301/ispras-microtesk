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

package ru.ispras.microtesk.test.template;

import java.math.BigInteger;

import ru.ispras.fortress.util.InvariantChecks;

public final class FixedValue implements Value {
  private final BigInteger value;

  protected FixedValue(final BigInteger value) {
    InvariantChecks.checkNotNull(value);
    this.value = value;
  }

  @Override
  public Value copy() {
    return this;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
