/*
 * Copyright 2016-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.tools.symexec;

import ru.ispras.microtesk.model.Immediate;
import ru.ispras.microtesk.model.IsaPrimitive;
import ru.ispras.microtesk.model.memory.Location;
import ru.ispras.microtesk.translator.nml.coverage.PathConstraintBuilder;
import ru.ispras.microtesk.translator.nml.coverage.SsaAssembler;
import ru.ispras.microtesk.translator.nml.coverage.TestBase;
import ru.ispras.microtesk.utils.NamePath;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.Nodes;
import ru.ispras.fortress.solver.constraint.Constraint;
import ru.ispras.fortress.solver.constraint.Formulas;
import ru.ispras.testbase.TestBaseContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormulaBuilder {
  public static final class Result {
    public final List<Node> ssa;
    public final Map<String, IsaPrimitive> vars;
    public final Map<String, NodeVariable> versions;

    public Result(
      final List<Node> ssa,
      final Map<String, IsaPrimitive> vars,
      final Map<String, NodeVariable> versions) {
      this.ssa = ssa;
      this.vars = vars;
      this.versions = versions;
    }
  }

  public static Result buildFormulas(
    final String model,
    final List<IsaPrimitive> sequence) {
    final SsaAssembler assembler = new SsaAssembler(TestBase.get().getStorage(model));
    final List<Node> formulae = new ArrayList<>(sequence.size());
    final Map<String, IsaPrimitive> vars = new LinkedHashMap<>();

    int n = 0;
    for (final IsaPrimitive p : sequence) {
      final String prefix = String.format("op_%d", n++, p.getName());
      final String tag = String.format("%s_%s", prefix, p.getName());

      final Map<String, Object> ctx = new HashMap<>();
      final Map<String, BitVector> consts = new LinkedHashMap<>();
      buildContext(ctx, consts, vars, p, tag);

      for (final Map.Entry<String, BitVector> e : consts.entrySet()) {
        final String name = String.format("%s_%s", prefix, e.getKey());
        final BitVector value = e.getValue();

        final Node variable = NodeVariable.newBitVector(name, value.getBitSize());
        variable.setUserData(1);

        formulae.add(Nodes.eq(variable, NodeValue.newBitVector(value)));
      }

      final Node f = assembler.assemble(ctx, p.getName(), tag);
      formulae.add(f);
    }

    final Constraint c = new PathConstraintBuilder(formulae).build();
    return new Result(
      ((Formulas) c.getInnerRep()).exprs(),
      vars,
      assembler.getVersions());
  }

  private static void buildContext(
      final Map<String, Object> ctx,
      final Map<String, BitVector> consts,
      final Map<String, IsaPrimitive> vars,
      final IsaPrimitive p,
      final String tag) {
    buildContext(ctx, consts, vars, NamePath.get(p.getName()), p, NamePath.get(tag));
    ctx.put(TestBaseContext.INSTRUCTION, p.getName());
  }

  private static void buildContext(
    final Map<String, Object> ctx,
    final Map<String, BitVector> consts,
    final Map<String, IsaPrimitive> vars,
    final NamePath prefix,
    final IsaPrimitive src,
    final NamePath tag) {

    int immcnt = 0;
    for (final Map.Entry<String, IsaPrimitive> entry : src.getArguments().entrySet()) {
      final NamePath path = prefix.resolve(entry.getKey());
      final String key = path.toString();
      final IsaPrimitive arg = entry.getValue();

      if (arg instanceof Immediate) {
        final Location location = ((Immediate) arg).access();
        consts.put(key, BitVector.valueOf(location.getValue(), location.getBitSize()));
        // override context for immediates
        ctx.put(key, Immediate.TYPE_NAME);
        immcnt++;
      } else {
        ctx.put(key, arg.getName());
        buildContext(ctx, consts, vars, path, arg, tag);
      }
      if (immcnt == src.getArguments().size()) {
        final String epath = tag.resolve(prefix.subpath(1)).toString();
        vars.put(epath, src);
      }
    }
  }
}
