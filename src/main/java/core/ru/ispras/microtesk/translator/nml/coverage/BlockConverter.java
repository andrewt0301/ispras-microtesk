/*
 * Copyright 2014-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.nml.coverage;

import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.expression.ExprUtils;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.solver.constraint.Constraint;
import ru.ispras.fortress.solver.constraint.ConstraintBuilder;
import ru.ispras.fortress.solver.constraint.ConstraintKind;
import ru.ispras.fortress.solver.constraint.Formulas;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.transformer.Transformer;
import ru.ispras.fortress.transformer.TransformerRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class BlockConverter {
  final Set<Block> visited;
  final List<Constraint> converted;
  final NodeTransformer xform;

  private BlockConverter() {
    this.visited = Collections.newSetFromMap(new IdentityHashMap<Block, Boolean>());
    this.converted = new ArrayList<>();
    this.xform = new NodeTransformer();

    final TransformerRule rule = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return Version.hasVersion(node);
      }

      @Override
      public Node apply(Node node) {
        return Version.bakeVersion((NodeVariable) node);
      }
    };
    this.xform.addRule(Node.Kind.VARIABLE, rule);
  }

  public static Collection<Constraint> convert(String name, Block block) {
    final BlockConverter conv = new BlockConverter();
    conv.convertRecursive(name, block);

    return conv.converted;
  }

  private void convertRecursive(String name, Block block) {
    if (!visited.contains(block)) {
      visited.add(block);
      converted.add(convertBlock(name, block, this.xform));
      for (GuardedBlock child : block.getChildren()) {
        convertRecursive(child.name, child.block);
      }
    }
  }

  private static Constraint convertBlock(String name, Block block, NodeTransformer xform) {
    final ConstraintBuilder builder =
        new ConstraintBuilder(ConstraintKind.FORMULA_BASED);
    final Formulas formulas = new Formulas();
    for (NodeOperation node : block.getStatements()) {
      if (ExprUtils.isOperation(node, SsaOperation.THIS_CALL)) {
        formulas.add(node);
      } else if (ExprUtils.isOperation(node, SsaOperation.CALL)) {
        formulas.add(convertCall(node, xform));
      } else {
        formulas.add(Transformer.transform(node, xform));
      }
    }
    for (GuardedBlock child : block.getChildren()) {
      final Node guard = Transformer.transform(child.guard, xform);
      formulas.add(new NodeOperation(SsaOperation.BLOCK, newNamed(child.name), guard));
    }

    builder.setName(name);
    builder.setInnerRep(formulas);
    builder.addVariables(formulas.getVariables());

    return builder.build();
  }

  static NodeOperation convertCall(final Node node, final NodeTransformer xform) {
    final NodeOperation call = (NodeOperation) node;
    final Node callee = call.getOperand(0);
    if (ExprUtils.isOperation(callee, SsaOperation.CLOSURE)) {
      final Node closure = convertClosure(callee, xform);
      return new NodeOperation(SsaOperation.CALL,
                               closure,
                               call.getOperand(1));
    }
    return call;
  }

  static Node convertClosure(final Node node, final NodeTransformer xform) {
    final Closure closure = new Closure(node);
    final List<Node> arguments =
        new ArrayList<>(closure.getArguments().size() + 1);

    arguments.add(closure.getOriginRef());
    for (final Node arg : closure.getArguments()) {
      if (ExprUtils.isOperation(arg, SsaOperation.CLOSURE)) {
        arguments.add(convertClosure(arg, xform));
      } else if (ExprUtils.isOperation(arg, SsaOperation.ARGUMENT_LINK)) {
        arguments.add(arg);
      } else {
        arguments.add(Transformer.transform(arg, xform));
      }
    }
    return new NodeOperation(SsaOperation.CLOSURE, arguments);
  }

  private static NodeVariable newNamed(String name) {
    if (name == null) {
      name = "";
    }
    return new NodeVariable(name, DataType.BOOLEAN);
  }
}
