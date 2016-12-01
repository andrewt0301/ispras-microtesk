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

package ru.ispras.microtesk.model.api.data.floatx;

import ru.ispras.fortress.data.types.bitvector.BitVector;

final class Float16Operations implements Operations {
  private static Operations instance = null;

  public static Operations get() {
    if (null == instance) {
      instance = new Float16Operations();
    }
    return instance;
  }

  private Float16Operations() {}

  @Override
  public FloatX add(final FloatX lhs, final FloatX rhs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX sub(final FloatX lhs, final FloatX rhs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX mul(final FloatX lhs, final FloatX rhs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX div(final FloatX lhs, final FloatX rhs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX rem(final FloatX lhs, final FloatX rhs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX sqrt(final FloatX arg) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public int compare(final FloatX first, final FloatX second) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNan(final FloatX arg) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSignalingNan(final FloatX arg) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX toFloat(final FloatX value, final Precision precision) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public BitVector toInteger(final FloatX value, final int size) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public FloatX fromInteger(final BitVector value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(final FloatX arg) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public String toHexString(final FloatX arg) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }
}