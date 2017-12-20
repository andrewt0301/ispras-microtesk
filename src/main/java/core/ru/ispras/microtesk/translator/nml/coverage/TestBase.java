/*
 * Copyright 2015-2017 ISP RAS (http://www.ispras.ru)
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

import static ru.ispras.fortress.expression.Nodes.EQ;
import static ru.ispras.fortress.expression.Nodes.NOT;
import static ru.ispras.fortress.expression.Nodes.OR;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.Nodes;
import ru.ispras.fortress.solver.SolverId;
import ru.ispras.fortress.solver.SolverResult;
import ru.ispras.fortress.solver.constraint.Constraint;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.SysUtils;
import ru.ispras.microtesk.utils.StringUtils;
import ru.ispras.testbase.TestBaseContext;
import ru.ispras.testbase.TestBaseQuery;
import ru.ispras.testbase.TestBaseQueryResult;
import ru.ispras.testbase.TestBaseRegistry;
import ru.ispras.testbase.TestData;
import ru.ispras.testbase.TestDataProvider;

public final class TestBase {
  final String path;
  final Map<String, Map<String, SsaForm>> storage;
  final ru.ispras.testbase.stub.TestBase testBase;

  private static TestBase instance = new TestBase();

  private static SolverId solverId = SolverId.CVC4_TEXT;

  public static TestBase get() {
    return instance;
  }

  public static void setSolverId(final SolverId value) {
    InvariantChecks.checkNotNull(value);
    solverId = value;
    ru.ispras.testbase.stub.TestBase.setSolverId(value);
  }

  private TestBase(final String path) {
    this.path = path;
    this.storage = new HashMap<>();
    this.testBase = ru.ispras.testbase.stub.TestBase.get();
  }

  private TestBase() {
    this(SysUtils.getHomeDir());
  }

  public TestBaseQueryResult executeQuery(TestBaseQuery query) {
    final TestBaseQueryResult rc = testBase.executeQuery(query);
    if (rc.getStatus() == TestBaseQueryResult.Status.OK ||
        rc.getStatus() == TestBaseQueryResult.Status.ERROR) {
      return rc;
    }

    SolverResult result;
    try {
      final PathConstraintBuilder builder = constraintBuilder(query);

      final Collection<Node> bindings = gatherBindings(query, builder.getVariables());
      bindings.add(findPathSpec(query, builder.getVariables()));

      final String testCase = (String) query.getContext().get(TestBaseContext.TESTCASE);
      if (testCase.equals("normal")) {
        final List<NodeVariable> marks = builder.getSpecialMarks();
        if (!marks.isEmpty()) {
          bindings.add(NOT(OR(marks)));
        }
      } else if (!testCase.equals("undefined") && !testCase.equals("unpredicted")) {
        final List<NodeVariable> marks = new ArrayList<>();
        for (final NodeVariable mark : builder.getSpecialMarks()) {
          if (mark.getName().matches(".*\\.undefined(!(\\d+))?$") ||
              mark.getName().matches(".*\\.unpredicted(!(\\d+))?$")) {
            marks.add(mark);
          }
        }
        if (!marks.isEmpty()) {
          bindings.add(NOT(OR(marks)));
        }
        bindings.add(EQ(findGuard(testCase, builder.getVariables()), Nodes.TRUE));
      } else {
        // unrestrited access to all paths: same as above, but w/o mark filtering
        bindings.add(EQ(findGuard(testCase, builder.getVariables()), Nodes.TRUE));
      }

      final Constraint constraint = builder.build(bindings);
      result = solverId.getSolver().solve(constraint);
    } catch (Throwable e) {
      final List<String> errors = new ArrayList<>(rc.getErrors().size() + 1);

      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));

      errors.add(sw.getBuffer().toString());
      errors.addAll(rc.getErrors());

      return TestBaseQueryResult.reportErrors(errors);
    }

    return fromSolverResult(query, result);
  }

  private static Node findGuard(final String testCase,
                                final Map<String, NodeVariable> variables) {
    final Map<String, List<String>> components = new TreeMap<>();
    for (final NodeVariable var : variables.values()) {
      if (var.isType(DataType.BOOLEAN)) {
        final int pos = var.getName().indexOf('!');
        if (pos > 0) {
          final String key = var.getName().substring(0, pos);
          components.put(key, splitInverse(key));
        }
      }
    }
    final List<String> subset = splitInverse(testCase);
    for (final Map.Entry<String, List<String>> entry : components.entrySet()) {
      if (isOrderedSubset(subset, entry.getValue())) {
        final NodeVariable node = variables.get(entry.getKey() + "!1");
        if (node != null) {
          return node;
        }
      }
    }
    throw new IllegalArgumentException(testCase);
  }

  private static boolean isOrderedSubset(final List<String> parts,
                                         final List<String> path) {
    final Iterator<String> pathIt = path.iterator();
    final Iterator<String> partsIt = parts.iterator();

    String part = partsIt.next();
    int matches = 0;
    while (pathIt.hasNext()) {
      final String component = pathIt.next();
      if (component.equals(part)) {
        ++matches;
        if (partsIt.hasNext()) {
          part = partsIt.next();
        } else {
          return true;
        }
      }
    }
    return !partsIt.hasNext() && matches > 0;
  }

  private static List<String> splitInverse(final String s) {
    final List<String> tokens = Arrays.asList(s.split("\\."));
    Collections.reverse(tokens);

    return tokens;
  }

  private Node findPathSpec(TestBaseQuery query, Map<String, NodeVariable> variables) {
    final Map<String, Object> context = query.getContext();
    final String name = (String) context.get(TestBaseContext.INSTRUCTION);
    final String situation = (String) context.get(TestBaseContext.TESTCASE);
    final Pair<String, String> pair = StringUtils.splitOnFirst(situation, '.');

    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (entry.getValue().equals(name)) {
        final String varName = entry.getKey() + pair.second + "!1";
        if (variables.containsKey(varName)) {
          return variables.get(varName);
        }
      }
    }
    return NodeValue.newBoolean(true);
  }

  private TestBaseQueryResult fromSolverResult(TestBaseQuery query, SolverResult result) {
    switch (result.getStatus()) {
    case SAT:
      return TestBaseQueryResult.success(parseResult(query, result));

    case ERROR:
      final List<String> errors = new ArrayList<>();
      for (String error : result.getErrors()) {
        errors.add(error);
      }
      return TestBaseQueryResult.reportErrors(errors);

    default:
    }
    return TestBaseQueryResult.success(TestDataProvider.empty());
  }

  private TestDataProvider parseResult(TestBaseQuery query, SolverResult result) {
    final Map<String, Data> values = new HashMap<>();
    for (Variable var : result.getVariables()) {
      values.put(var.getName(), var.getData());
    }

    final Map<String, Object> valueNodes = new HashMap<>();
    for (Map.Entry<String, Node> entry : query.getBindings().entrySet()) {
      if (entry.getValue().getKind() == Node.Kind.VARIABLE) {
        final String name = entry.getKey() + "!1";
        if (values.containsKey(name)) {
          valueNodes.put(entry.getKey(), new NodeValue(values.get(name)));
        } else {
          if (entry.getValue().isType(DataTypeId.LOGIC_INTEGER)) {
            // TODO: Bit width required.
            // TODO: Randomization.
            valueNodes.put(entry.getKey(), NodeValue.newInteger(0));
          } else {
            // TODO: Bit width required.
            // TODO: Randomization.
          }
        }
      }
    }

    final TestData data = new TestData(valueNodes);
    final Iterator<TestData> iterator = Collections.singletonList(data).iterator();

    return new TestDataProvider() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public TestData next() {
        return iterator.next();
      }
    };
  }

  private Collection<Node> gatherBindings(TestBaseQuery query, Map<String, NodeVariable> variables) {
    final List<Node> bindings = new ArrayList<>();
    final NodeTransformer caster = new NodeTransformer(IntegerCast.rules());

    for (Map.Entry<String, Node> entry: query.getBindings().entrySet()) {
      if (entry.getValue().getKind() == Node.Kind.VALUE) {
        final String name = entry.getKey() + "!1";
        if (variables.containsKey(name)) {
          final Node binding = EQ(variables.get(name), entry.getValue());
          bindings.add(Utility.transform(binding, caster));
        }
      }
    }
    return bindings;
  }

  private PathConstraintBuilder constraintBuilder(final TestBaseQuery query) {
    final Map<String, Object> context = query.getContext();
    final String model = (String) context.get(TestBaseContext.PROCESSOR);
    final String instr = (String) context.get(TestBaseContext.INSTRUCTION);
    final SsaAssembler assembler = new SsaAssembler(getStorage(model));
    final Node formula = assembler.assemble(context, instr);

    return new PathConstraintBuilder(formula);
  }

  public Map<String, SsaForm> getStorage(final String model) {
    if (storage.containsKey(model)) {
      return storage.get(model);
    }

    final Map<String, SsaForm> ssa = SsaStorage.load(model, path);
    storage.put(model, ssa);

    return ssa;
  }

  public TestBaseRegistry getRegistry() {
    return testBase.getRegistry();
  }
}
