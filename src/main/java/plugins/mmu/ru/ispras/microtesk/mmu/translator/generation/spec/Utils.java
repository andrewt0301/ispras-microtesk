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

package ru.ispras.microtesk.mmu.translator.generation.spec;

import java.math.BigInteger;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;

public final class Utils {
  private Utils() {}

  public static String toString(final BigInteger value) {
    InvariantChecks.checkNotNull(value);
    return String.format("new BigInteger(\"%d\", 10)", value);
  }

  public static String getVariableName(final String context, final String name) {
    InvariantChecks.checkNotNull(context);
    InvariantChecks.checkNotNull(name);

    final int dotIndex = name.indexOf('.');
    if (dotIndex == -1) {
      return name + ".get()";
    }

    final String prefix = name.substring(0, dotIndex);
    final String suffix = name.substring(dotIndex + 1, name.length());

    if (prefix.equals(context)) {
      return suffix;
    }

    return prefix + ".get()." + suffix;
  }

  public static String toString(final String context, final IntegerField field) {
    return toString(context, field, true);
  }

  public static String toString(
      final String context,
      final IntegerField field,
      final boolean printAsVariable) {
    InvariantChecks.checkNotNull(context);
    InvariantChecks.checkNotNull(field);

    if (field.getVariable().isDefined()) {
      return Utils.toString(field.getVariable().getValue());
    }

    final String name = getVariableName(context, field.getVariable().getName());
    if (field.isVariable() && printAsVariable) {
      return name;
    }

    return String.format(
        "%s.field(%d, %d)", name, field.getLoIndex(), field.getHiIndex());
  }

  public static String toMmuExpressionText(final String context, final List<IntegerField> fields) {
    InvariantChecks.checkNotNull(context);

    if (null == fields) {
      return "null";
    }

    if (fields.isEmpty()) {
      return "MmuExpression.empty()";
    }

    if (fields.size() == 1) {
      return toMmuExpressionText(context, fields.get(0));
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("MmuExpression.rcat(");

    boolean isFirst = true;
    for (final IntegerField field : fields) {
      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(", ");
      }

      final IntegerVariable variable = field.getVariable();
      final String variableText;

      if (variable.isDefined()) {
        variableText = String.format("new IntegerVariable(%d, %s)",
            variable.getWidth(), toString(variable.getValue()));
      } else {
        variableText =
            getVariableName(context, variable.getName());
      }

      final String text = String.format(
          "%s.field(%d, %d)", variableText, field.getLoIndex(), field.getHiIndex());

      sb.append(text);
    }

    sb.append(')');
    return sb.toString();
  }

  public static String toMmuExpressionText(final String context, final IntegerField field) {
    InvariantChecks.checkNotNull(context);
    InvariantChecks.checkNotNull(field);

    final StringBuilder sb = new StringBuilder();
    sb.append("MmuExpression.");

    if (field.getVariable().isDefined()) {
      sb.append(String.format(
          "val(%s, %d", toString(field.getVariable().getValue()), field.getWidth()));
    } else {
      final String name = getVariableName(context, field.getVariable().getName());
      sb.append(String.format("var(%s", name));

      if (!field.isVariable()) {
        sb.append(String.format(", %d, %d", field.getLoIndex(), field.getHiIndex()));
      }
    }

    sb.append(')');
    return sb.toString();
  }
}
