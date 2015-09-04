/*
 * Copyright 2013-2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.nml.ir.valueinfo;

import java.math.BigInteger;
import java.util.List;

import ru.ispras.microtesk.model.api.type.TypeId;
import ru.ispras.microtesk.translator.nml.ir.expression.Expr;
import ru.ispras.microtesk.translator.nml.ir.expression.Operands;
import ru.ispras.microtesk.translator.nml.ir.shared.Type;

final class ValueInfoCast {
  /**
   * Returns a value information object describing the type values should be cast to in order to be
   * used as operands of some operator. If value types are incompatible and cannot be cast,
   * <code>null</code> is returned.
   * 
   * If a value information object describing an operand refers to a constant value, the value is
   * cast to a corresponding type and the value information object is updated. In other cases, value
   * elements stay unchanged.
   * 
   * @param target Preferred value kind (MODEL or NATIVE), needed when values have different kinds.
   * @param values Value information objects describing arguments. Modified if holds a constant
   *        value that requires a cast.
   * @return Value information object describing the target value type or <code>null</code> is value
   *         types are incompatible.
   */

  public static ValueInfo getCast(ValueInfo.Kind target, List<ValueInfo> values) {
    assert Operands.UNARY.count() == values.size() || Operands.BINARY.count() == values.size();

    if (Operands.UNARY.count() == values.size()) {
      return values.get(0).typeInfoOnly();
    }

    final ValueInfo left = values.get(0);
    final ValueInfo right = values.get(1);

    if (left.hasEqualType(right)) {
      return left.typeInfoOnly();
    }

    if (left.getValueKind() != right.getValueKind()) {
      return getCastMixed(target, values);
    }

    if (left.isModel()) {
      return getCastModel(values);
    }

    return getCastNative(values);
  }

  private static ValueInfo getCastMixed(ValueInfo.Kind target, List<ValueInfo> values) {
    for (ValueInfo vi : values) {
      // andrewt > we cast to model type because native types do not work correctly
      // with large types (size > Integer.SIZE)
      if (vi.getValueKind() == ValueInfo.Kind.MODEL) {
        return vi.typeInfoOnly();
      }
    }

    assert false;
    return null;
  }

  private static ValueInfo getCastModel(List<ValueInfo> values) {
    final Type castType =
      ModelTypeCastRules.getCastType(values.get(0).getModelType(), values.get(1).getModelType());

    if (null == castType) {
      return null;
    }

    return ValueInfo.createModel(castType);
  }

  private static ValueInfo getCastNative(List<ValueInfo> values) {
    final Class<?> castType = NativeTypeCastRules.getCastType(
      values.get(0).getNativeType(), values.get(1).getNativeType());

    if (null == castType) {
      return null;
    }

    for (int index = 0; index < values.size(); ++index) {
      final ValueInfo vi = values.get(index);
      if (vi.isConstant()) {
        final Object value = NativeTypeCastRules.castTo(castType, vi.getNativeValue());
        values.set(index, ValueInfo.createNative(value));
      }
    }

    return ValueInfo.createNativeType(castType);
  }
}


class ModelTypeCastRules {
  private ModelTypeCastRules() {}

  private static final TypeId CAST_TYPE_MAP[][] = {
    {null,        TypeId.CARD, TypeId.INT, TypeId.BOOL},
    {TypeId.CARD, TypeId.CARD, TypeId.INT, null},
    {TypeId.INT,  TypeId.INT,  TypeId.INT, null},
    {TypeId.BOOL, null,        null,       TypeId.BOOL},
  };

  public static TypeId getCastTypeId(TypeId left, TypeId right) {
    int col = 0; // left -> col
    for (int columnIndex = 1; columnIndex < CAST_TYPE_MAP[0].length; ++columnIndex) {
      if (CAST_TYPE_MAP[0][columnIndex] == left) {
        col = columnIndex;
        break;
      }
    }

    if (0 == col) { // left is not found
      return null;
    }

    int row = 0; // right -> row
    for (int rowIndex = 1; rowIndex < CAST_TYPE_MAP.length; ++rowIndex) {
      if (CAST_TYPE_MAP[rowIndex][0] == right) {
        row = rowIndex;
        break;
      }
    }

    if (0 == row) { // right is not found
      return null;
    }

    return CAST_TYPE_MAP[col][row];
  }

  public static Type getCastType(Type left, Type right) {
    final TypeId typeId = getCastTypeId(left.getTypeId(), right.getTypeId());
    if (null == typeId) {
      return null;
    }

    final Expr bitSizeExpr = (left.getBitSize() >= right.getBitSize()) ?
      left.getBitSizeExpr() : right.getBitSizeExpr();

    return (typeId == left.getTypeId()) ? left.resize(bitSizeExpr) : right.resize(bitSizeExpr);
  }
}


class NativeTypeCastRules {
  private NativeTypeCastRules() {}

  private static abstract class Cast {
    final Class<?> from;
    final Class<?> to;

    Cast(Class<?> from, Class<?> to) {
      assert null != from;
      assert null != to;

      this.from = from;
      this.to = to;
    }

    final Object cast(Object value) {
      assert (value.getClass() == from) && (value.getClass() != to);
      return doCast(value);
    }

    abstract Object doCast(Object value);
  }

  private static final Cast INT_TO_LONG = new Cast(Integer.class, Long.class) {
    @Override
    public Object doCast(Object value) {
      return ((Number) value).longValue();
    }
  };

  private static final Class<?> CAST_TYPE_MAP[][] = {
    {null,             BigInteger.class, boolean.class},
    {BigInteger.class, BigInteger.class, null},
    {boolean.class,    null,             boolean.class}
  };

  public static Class<?> getCastType(Class<?> left, Class<?> right) {
    int col = 0; // left -> col
    for (int columnIndex = 1; columnIndex < CAST_TYPE_MAP[0].length; ++columnIndex) {
      if (CAST_TYPE_MAP[0][columnIndex] == left) {
        col = columnIndex;
        break;
      }
    }

    if (0 == col) { // left is not found
      return null;
    }

    int row = 0; // right -> row
    for (int rowIndex = 1; rowIndex < CAST_TYPE_MAP.length; ++rowIndex) {
      if (CAST_TYPE_MAP[rowIndex][0] == right) {
        row = rowIndex;
        break;
      }
    }

    if (0 == row) { // right is not found
      return null;
    }

    return CAST_TYPE_MAP[col][row];
  }

  public static Object castTo(Class<?> target, Object value) {
    if (value.getClass() == target) {
      return value;
    }

    if (INT_TO_LONG.to == target && INT_TO_LONG.from == value.getClass())
      return INT_TO_LONG.cast(value);

    assert false : String.format("Unsupported cast: %s to %s", value.getClass(), target);
    return null;
  }
}
