/*
 * Copyright 2017-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.basis.solver.bitvector;

import ru.ispras.castle.util.Logger;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.ExprTreeVisitorDefault;
import ru.ispras.fortress.expression.ExprTreeWalker;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.utils.FortressUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link BitVectorFormulaProblemSat4j} represents a bit-vector problem.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BitVectorFormulaProblemSat4j extends BitVectorFormulaBuilder {
  /** Using directly ISolver instead of Sat4jFormula.Builder slows down performance. */
  private final Sat4jFormula.Builder builder;

  /** Contains the indices of the variables. */
  private final Map<Variable, Integer> indices;
  private int index;

  /** Contains the used/unused bits of the variables. */
  private final Map<Variable, BitVector> masks;

  public BitVectorFormulaProblemSat4j() {
    this.builder = new Sat4jFormula.Builder();
    this.indices = new LinkedHashMap<>();
    // Variable identifier should be positive.
    this.index = 1;
    this.masks = new LinkedHashMap<>();
  }

  public BitVectorFormulaProblemSat4j(final BitVectorFormulaProblemSat4j r) {
    this.builder = new Sat4jFormula.Builder(r.builder);
    this.indices = new LinkedHashMap<>(r.indices);
    this.index = r.index;
    this.masks = new LinkedHashMap<>(r.masks);
  }

  public Map<Variable, Integer> getIndices() {
    return indices;
  }

  public Map<Variable, BitVector> getMasks() {
    return masks;
  }

  public Sat4jFormula getFormula() {
    return builder.build();
  }

  @Override
  public void addFormula(final Node formula) {
    Logger.debug("Add formula: %s", formula);

    handleConstants(formula);

    final NodeOperation operation = (NodeOperation) formula;
    final Enum<?> operationId = operation.getOperationId();

    if (operationId == StandardOperation.EQ || operationId == StandardOperation.NOTEQ) {
      handleEq(operation);
    } else if (operationId == StandardOperation.AND) {
      handleAnd(operation);
    } else if (operationId == StandardOperation.OR) {
      handleOr(operation);
    }
  }

  private void handleConstants(final Node node) {
    final ExprTreeWalker walker = new ExprTreeWalker(
        new ExprTreeVisitorDefault() {
          @Override
          public void onVariable(final NodeVariable nodeVariable) {
            final int i = index;
            final int x = getVarIndex(nodeVariable);

            // If the variable is new.
            if (x >= i) {
              final Variable variable = nodeVariable.getVariable();

              if (variable.hasValue()) {
                setUsedBits(variable);

                // Generate n unit clauses (c[i] ? x[i] : ~x[i]).
                builder.addAllClauses(
                    Sat4jUtils.encodeVarEqualConst(
                        nodeVariable,
                        x,
                        FortressUtils.getInteger(variable.getData())));
              }
            }
          }
        });

    walker.visit(node);
  }

  private void handleEq(final NodeOperation equation) {
    final Node lhs = FortressUtils.getVariable(equation.getOperand(0)) != null
        ? equation.getOperand(0)
        : equation.getOperand(1);

    final Node rhs = FortressUtils.getVariable(equation.getOperand(0)) != null
        ? equation.getOperand(1)
        : equation.getOperand(0);

    Logger.debug("Handle equation: %s", equation);

    final int n = FortressUtils.getBitSize(lhs);
    final int x = getVarIndex(lhs);

    setUsedBits(lhs);
    setUsedBits(rhs);

    if (equation.getOperationId() == StandardOperation.EQ) {
      if (rhs.getKind() == Node.Kind.VALUE) {
        final NodeValue value = (NodeValue) rhs;

        // Equality x == c.
        builder.addAllClauses(
            Sat4jUtils.encodeVarEqualConst(lhs, x, FortressUtils.getInteger(value)));
      } else {
        final int y = getVarIndex(rhs);

        // Equality x == y.
        builder.addAllClauses(
            Sat4jUtils.encodeVarEqualVar(lhs, x, rhs, y));
      }
    } else {
      if (rhs.getKind() == Node.Kind.VALUE) {
        final NodeValue value = (NodeValue) rhs;

        // Inequality x != c.
        builder.addAllClauses(
            Sat4jUtils.encodeVarNotEqualConst(lhs, x, FortressUtils.getInteger(value)));
      } else {
        final int y = getVarIndex(rhs);

        // Inequality x != y.
        builder.addAllClauses(
            Sat4jUtils.encodeVarNotEqualVar(lhs, x, rhs, y, index));

        index += 2 * n;
      }
    }
  }

  private void handleAnd(final NodeOperation operation) {
    for (final Node operand : operation.getOperands()) {
      final NodeOperation clause = (NodeOperation) operand;
      final Enum<?> clauseId = clause.getOperationId();

      if (clauseId == StandardOperation.EQ || clauseId == StandardOperation.NOTEQ) {
        handleEq(clause);
      } else if (clauseId == StandardOperation.AND) {
        handleAnd(clause);
      } else if (clauseId == StandardOperation.OR) {
        handleOr(clause);
      }
    }
  }

  // TODO: call handleEq inside
  private void handleOr(final NodeOperation operation) {
    int ej = index;

    builder.addClause(Sat4jUtils.createClause(index, operation.getOperandCount()));
    index += operation.getOperandCount();

    for (final Node operand : operation.getOperands()) {
      final NodeOperation equation = (NodeOperation) operand;

      InvariantChecks.checkTrue(
          equation.getOperationId() == StandardOperation.EQ
          || equation.getOperationId() == StandardOperation.NOTEQ);

      final Node lhs = FortressUtils.getVariable(equation.getOperand(0)) != null
          ? equation.getOperand(0)
          : equation.getOperand(1);

      final Node rhs = FortressUtils.getVariable(equation.getOperand(0)) != null
          ? equation.getOperand(1)
          : equation.getOperand(0);

      final int n = FortressUtils.getBitSize(lhs);
      final int x = getVarIndex(lhs);

      setUsedBits(lhs);
      setUsedBits(rhs);

      if (equation.getOperationId() == StandardOperation.EQ) {
        if (rhs.getKind() == Node.Kind.VALUE) {
          final NodeValue value = (NodeValue) rhs;

          // Equality x == c.
          builder.addAllClauses(
              Sat4jUtils.encodeVarEqualConst(ej, lhs, x, FortressUtils.getInteger(value)));
        } else {
          final int y = getVarIndex(rhs);

          // Equality x == y.
          builder.addAllClauses(
              Sat4jUtils.encodeVarEqualVar(ej, lhs, x, rhs, y, index));

          index += 2 * n;
        }
      } else {
        if (rhs.getKind() == Node.Kind.VALUE) {
          final NodeValue value = (NodeValue) rhs;

          // Inequality x != c.
          builder.addAllClauses(
              Sat4jUtils.encodeVarNotEqualConst(ej, lhs, x, FortressUtils.getInteger(value)));
        } else {
          final int y = getVarIndex(rhs);

          // Inequality x != y.
          builder.addAllClauses(
              Sat4jUtils.encodeVarNotEqualVar(ej, lhs, x, rhs, y, index));

          index += 2 * n;
        }
      }

      ej++;
    } // for equation.
  }

  private int getVarIndex(final Node node) {
    final Variable variable = FortressUtils.getVariable(node);
    InvariantChecks.checkNotNull(variable);

    final Integer oldIndex = indices.get(variable);

    if (oldIndex != null) {
      return oldIndex;
    }

    final int newIndex = index;

    indices.put(variable, newIndex);
    index += variable.getType().getSize();

    return newIndex;
  }

  private BitVector getVarMask(final Variable variable) {
    BitVector mask = masks.get(variable);

    if (mask == null) {
      masks.put(variable, mask = BitVector.newEmpty(FortressUtils.getBitSize(variable)));
    }

    return mask;
  }

  private void setUsedBits(final Node node) {
    final Variable variable = FortressUtils.getVariable(node);

    if (variable == null) {
      return;
    }

    final BitVector mask = getVarMask(variable);

    for (int i = FortressUtils.getLowerBit(node); i <= FortressUtils.getUpperBit(node); i++) {
      mask.setBit(i, true);
    }
  }

  private void setUsedBits(final Variable variable) {
    final BitVector mask = getVarMask(variable);
    mask.setAll();
  }

  @Override
  public BitVectorFormulaProblemSat4j clone() {
    return new BitVectorFormulaProblemSat4j(this);
  }
}
