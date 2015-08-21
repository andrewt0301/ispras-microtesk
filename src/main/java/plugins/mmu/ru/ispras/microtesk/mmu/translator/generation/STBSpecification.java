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

package ru.ispras.microtesk.mmu.translator.generation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.model.api.PolicyId;
import ru.ispras.microtesk.mmu.translator.generation.spec.Atom;
import ru.ispras.microtesk.mmu.translator.generation.spec.AtomExtractor;
import ru.ispras.microtesk.mmu.translator.generation.spec.BufferExprAnalyzer;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
import ru.ispras.microtesk.mmu.translator.ir.AttributeRef;
import ru.ispras.microtesk.mmu.translator.ir.Buffer;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.mmu.translator.ir.Segment;
import ru.ispras.microtesk.mmu.translator.ir.Stmt;
import ru.ispras.microtesk.mmu.translator.ir.StmtAssign;
import ru.ispras.microtesk.mmu.translator.ir.StmtException;
import ru.ispras.microtesk.mmu.translator.ir.StmtIf;
import ru.ispras.microtesk.mmu.translator.ir.StmtMark;
import ru.ispras.microtesk.mmu.translator.ir.Type;
import ru.ispras.microtesk.mmu.translator.ir.Variable;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAction;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuExpression;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuTransition;
import ru.ispras.microtesk.mmu.translator.ir.spec.builder.AssignmentBuilder;
import ru.ispras.microtesk.translator.generation.STBuilder;

final class STBSpecification implements STBuilder {
  public static final String CLASS_NAME = "Specification";

  private static final Class<?> SPEC_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem.class;

  private static final Class<?> MMU_OPERATION_CLASS =
      ru.ispras.microtesk.mmu.basis.MemoryOperation.class;

  private static final Class<?> INTEGER_CLASS = 
      ru.ispras.microtesk.basis.solver.integer.IntegerVariable.class;

  private final String packageName;
  private final Ir ir;

  public STBSpecification(final String packageName, final Ir ir) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(ir);

    this.packageName = packageName;
    this.ir = ir;
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");
    buildHeader(st);
    buildBody(st, group);
    return st;
  }

  private void buildHeader(final ST st) {
    st.add("name", CLASS_NAME); 
    st.add("pack", packageName);

    st.add("imps", Arrays.class.getName());
    st.add("imps", BigInteger.class.getName());
    st.add("imps", String.format("%s.*", INTEGER_CLASS.getPackage().getName()));
    st.add("imps", String.format("%s.*", MMU_OPERATION_CLASS.getPackage().getName()));
    st.add("imps", String.format("%s.*", SPEC_CLASS.getPackage().getName()));
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("body");

    stBody.add("name", CLASS_NAME);
    stBody.add("spec", SPEC_CLASS.getSimpleName());
    st.add("members", stBody);

    final BodyBuilder build = new BodyBuilder(ir, stBody, group);
    build.build();
  }

  private static String toMmuExpressionText(final List<IntegerField> fields) {
    if (fields.isEmpty()) {
      return "MmuExpression.empty()";
    }

    if (fields.size() == 1) {
      final IntegerField field = fields.get(0);
      return toMmuExpressionText(field);
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

      final String name = field.getVariable().getName().replace('.', '_');
      sb.append(name);
      sb.append(".field(");
      sb.append(field.getLoIndex());
      sb.append(", ");
      sb.append(field.getHiIndex());
      sb.append(')');
    }

    sb.append(')');
    return sb.toString();
  }

  private static String toMmuExpressionText(final IntegerField field) {
    final StringBuilder sb = new StringBuilder();
    sb.append("MmuExpression.");

    if (field.getVariable().isDefined()) {
      sb.append(String.format("val(%s, %d",
          Utils.toString(field.getVariable().getValue()), field.getWidth()));
    } else {
      final String name = field.getVariable().getName().replace('.', '_');
      sb.append(String.format("var(%s", name));

      if (!field.isVariable()) {
        sb.append(String.format(", %d, %d", field.getLoIndex(), field.getHiIndex()));
      }
    }

    sb.append(')');
    return sb.toString();
  }

  private static String toMmuBindingsText(
      final List<Pair<IntegerVariable, IntegerField>> bindings) {

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("Arrays.<MmuBinding>asList(", Arrays.class.getSimpleName()));

    boolean isFirst = true;
    for (final Pair<IntegerVariable, IntegerField> binding : bindings) {
      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(',');
      }

      sb.append(System.lineSeparator());
      sb.append("    ");

      final String leftText = binding.first.getName().replace('.', '_');
      final String rightText = toMmuExpressionText(binding.second);

      sb.append(String.format("new MmuBinding(%s, %s)", leftText, rightText));
    }

    sb.append(')');
    return sb.toString();
  }

  private static final class BodyBuilder {
    private final Ir ir;
    private final ST st;
    private final STGroup group;

    private final Set<String> exceptions = new HashSet<>();
    private List<String> currentMarks = null;
    private int actionIndex = 0;
    private int transitionIndex = 0;

    private BodyBuilder(final Ir ir, final ST st, final STGroup group) {
      this.ir = ir;
      this.st = st;
      this.group = group;
    }

    public void build() {
      buildAddresses();
      //buildSegments();
      buildBuffers();
      buildControlFlow();
    }

    private void buildSeparator(final String text) {
      final ST stSeparator = group.getInstanceOf("separator");
      stSeparator.add("text", text);
      st.add("members", stSeparator);
      st.add("stmts", "");
    }

    private List<String> buildFields(
        final String name,
        final Type type,
        final ST stStruct,
        final boolean needStructs) {
      final String id = name.replace('.', '_');
      if (type.isStruct()) {
        final List<String> names = new ArrayList<>(); 

        for (final Map.Entry<String, Type> field : type.getFields().entrySet()) {
          final String fieldName = name + "." + field.getKey();
          final Type fieldType = field.getValue();

          final List<String> fieldNames = buildFields(fieldName, fieldType, stStruct, needStructs);
          names.addAll(fieldNames);
        }

        if (needStructs && 
            names.size() > 1 && 
            !ir.getAddresses().containsKey(name) && 
            !ir.getSegments().containsKey(name)) {
          final ST stVariable = group.getInstanceOf("struct_def");
          stVariable.add("id", id);
          stVariable.add("name", name);
          stVariable.add("fields", names);
          st.add("members", stVariable);
        }

        return names;
      } else {
        if (null != stStruct) {
          stStruct.add("fields", id);
        }

        final ST stVariable = group.getInstanceOf("variable_def");
        stVariable.add("id", id);
        stVariable.add("name", name);
        stVariable.add("size", type.getBitSize());
        st.add("members", stVariable);

        return Collections.singletonList(id);
      }
    }
/*
    private void buildFieldAliases(
        final String name,
        final Type type,
        final String aliasName) {
      final String id = name.replace('.', '_');
      final String aliasId = aliasName.replace('.', '_');

      if (type.isStruct()) {
        final List<String> names = new ArrayList<>(); 

        for (final Map.Entry<String, Type> field : type.getFields().entrySet()) {
          final String fieldName = name + "." + field.getKey();
          final Type fieldType = field.getValue();

          final List<String> fieldNames = buildFields(fieldName, fieldType, stStruct, needStructs);
          names.addAll(fieldNames);
        }

        if (needStructs && 
            names.size() > 1 && 
            !ir.getAddresses().containsKey(name) && 
            !ir.getSegments().containsKey(name)) {
          final ST stVariable = group.getInstanceOf("struct_def");
          stVariable.add("id", id);
          stVariable.add("name", name);
          stVariable.add("fields", names);
          st.add("members", stVariable);
        }

        return names;
      } else {
        

        final String text = String.format("private , args)
        final ST stVariable = group.getInstanceOf("variable_def");
        stVariable.add("id", id);
        stVariable.add("name", name);
        stVariable.add("size", type.getBitSize());
        
        st.add("members", stVariable);
      }
    }
*/

    private void buildAddresses() {
      buildSeparator("Addresses");

      for(final Address address : ir.getAddresses().values()) {
        final String name = address.getId();
        final ST stDef = group.getInstanceOf("address_def");

        buildFields(name, address.getContentType(), stDef, true);

        stDef.add("name", name);
        stDef.add("value_name", name + "_" + Utils.toString(address.getAccessChain(), "_"));
        st.add("members", stDef);

        final ST stReg = group.getInstanceOf("address_reg");
        stReg.add("name", name);
        st.add("stmts", stReg);
      }
    }

    private void buildSegments() {
      buildSeparator("Segments");

      for(final Segment segment : ir.getSegments().values()) {
        for (final Variable variable : segment.getVariables()) {
          buildFields(variable.getName(), variable.getType(), null, true);
        }

        final ST stDef = group.getInstanceOf("segment_def");
        stDef.add("name", segment.getId());
        stDef.add("va", segment.getAddress().getId());
        stDef.add("pa", segment.getDataArgAddress().getId());
        stDef.add("start", String.format("0x%xL", segment.getMin()));
        stDef.add("end", String.format("0x%xL", segment.getMax()));
        stDef.add("mapped", "false");
        stDef.add("va_expr", "null");
        stDef.add("pa_expr", "null");
        st.add("members", stDef);

        final ST stReg = group.getInstanceOf("segment_reg");
        stReg.add("name", segment.getId());
        st.add("stmts", stReg);
      }
    }

    private void buildBuffers() {
      buildSeparator("Buffers");

      for(final Buffer buffer : ir.getBuffers().values()) {
        final ST stDef = group.getInstanceOf("buffer_def");
        buildFields(buffer.getId(), buffer.getEntry(), stDef, false);

        final BufferExprAnalyzer analyzer = new BufferExprAnalyzer(
            buffer.getAddress(), buffer.getAddressArg(), buffer.getIndex(), buffer.getMatch());

        stDef.add("name", buffer.getId());
        stDef.add("ways", String.format("%dL", buffer.getWays().longValue()));
        stDef.add("sets", String.format("%dL", buffer.getSets().longValue()));
        stDef.add("addr", buffer.getAddress().getId());
        stDef.add("tag", toMmuExpressionText(analyzer.getTagFields()));
        stDef.add("index", toMmuExpressionText(analyzer.getIndexFields()));
        stDef.add("offset", toMmuExpressionText(analyzer.getOffsetFields()));
        stDef.add("match", toMmuBindingsText(analyzer.getMatchBindings()));
        stDef.add("guard_cond", "null"); // TODO
        stDef.add("guard", "null"); // TODO
        stDef.add("replaceable", Boolean.toString(buffer.getPolicy() != PolicyId.NONE));
        stDef.add("parent", buffer.getParent() != null ? buffer.getParent().getId() : "null");
        st.add("members", stDef);

        final ST stReg = group.getInstanceOf("buffer_reg");
        stReg.add("name", buffer.getId());
        st.add("stmts", stReg);
      }
    }

    private void buildControlFlow() {
      final Map<String, Memory> memories = ir.getMemories();
      if (memories.size() > 1) {
        throw new IllegalStateException("Only one mmu specification is allowed.");
      }

      final Memory memory = memories.values().iterator().next();
      buildSeparator(String.format("Control Flow (%s)", memory.getId()));

      st.add("stmts", String.format("builder.setVirtualAddress(%s);", memory.getAddress().getId()));

      buildFields(memory.getDataArg().getName(), memory.getDataArg().getType(), null, true);
      st.add("members", "");

      for (final Variable variable : memory.getVariables()) {
        buildFields(variable.getName(), variable.getType(), null, true);
      }

      st.add("members", "");
      st.add("stmts", "");

      buildAction("ROOT",
          String.format("new MmuBinding(%s.getVariable())", memory.getAddress().getId()));
      st.add("stmts", "builder.setStartAction(ROOT);");
      st.add("stmts", "");

      buildAction("START");
      buildTransition("ROOT", "START", "new MmuGuard(MemoryOperation.LOAD)");
      st.add("stmts", "");

      buildAction("STOP");
      buildTransition("ROOT", "START", "new MmuGuard(MemoryOperation.STORE)");
      st.add("stmts", "");
      st.add("members", "");

      buildControlFlowForAttribute("START", memory, AbstractStorage.READ_ATTR_NAME);
      buildControlFlowForAttribute("START", memory, AbstractStorage.WRITE_ATTR_NAME);
    }

    private void buildControlFlowForAttribute(
        final String start,
        final Memory memory,
        final String attributeName) {
      final Attribute attribute = memory.getAttribute(attributeName);
      if (null == attribute) {
        throw new IllegalStateException(String.format(
            "Undefined attribute: %s.%s", memory.getId(), attributeName));
      }

      currentMarks = null;
      final String stop = buildControlFlowForStmts(st, group, start, attribute.getStmts());
      if (null != stop) {
        buildTransition(stop, "STOP");
      }
    }

    private String buildControlFlowForStmts(
        final ST st,
        final STGroup group,
        final String start,
        final List<Stmt> stmts) {
      InvariantChecks.checkNotNull(start);
      String current = start;

      for (final Stmt stmt : stmts) {
        switch(stmt.getKind()) {
          case ASSIGN:
            current = buildStmtAssign(current, (StmtAssign) stmt);
            break;

          case EXCEPT:
            buildStmtException(current, (StmtException) stmt);
            return null; // Control flow cannot be continued after exception.

          case IF:
            current = buildStmtIf(current, (StmtIf) stmt);
            break;

          case MARK:
            buildStmtMark((StmtMark) stmt);
            break;

          case TRACE: // Ignored
            break;

          default:
            throw new IllegalStateException("Unknown statement: " + stmt.getKind());
        }
      }

      return current;
    }

    private String buildStmtAssign(final String source, final StmtAssign stmt) {
      final Node left = stmt.getLeft();
      final Node right = stmt.getRight();

      // Assignments that use the "data" MMU variable are ignored.
      if (isDataVariable(left) || isDataVariable(right)) {
        return source;
      }

      if (isAddressTranslation(right)) {
        // TODO
        //return registerCall(source, left, (AttributeRef) right.getUserData());
        return source;
      }

      final Atom lhs = AtomExtractor.extract(left);
      final Atom rhs = AtomExtractor.extract(right);

      if (Atom.Kind.VARIABLE != lhs.getKind() && 
          Atom.Kind.GROUP != lhs.getKind() &&
          Atom.Kind.FIELD != lhs.getKind()) {
        throw new IllegalArgumentException(left + " cannot be used as left side of assignment.");
      }

      System.out.println("!!!!! LEFT = " + lhs);
      System.out.println("!!!!! RIGHT = " + rhs);

      final String target = newAssign();
      st.add("members", String.format("// ASSIGN %s = %s", left, right));

      /*
      if (!lhs.getKind().isStruct() && !rhs.getKind().isStruct()) {
        buildAction(target, String.format("new MmuBinding(%s, %s)", lhs.getText(), rhs.getText()));
      } else {
        // TODO: NEED BINDINGS
        buildAction(target);
      }*/

      buildAction(target);
      buildTransition(source, target);
      return target;
    }

    private void buildStmtException(final String current, final StmtException stmt) {
      final String exception = stmt.getMessage();

      if (!exceptions.contains(exception)) {
        buildAction(exception);
        exceptions.add(exception);
      }

      buildTransition(current, exception);
    }

    private String buildStmtIf(final String source, final StmtIf stmt) {
      InvariantChecks.checkNotNull(source);

      final String join = newJoin();
      buildAction(join);

      String current = source;
      for (final Pair<Node, List<Stmt>> block : stmt.getIfBlocks()) {
        final Node condition = block.first;
        final List<Stmt> stmts = block.second;
        final GuardPrinter guardPrinter = new GuardPrinter(ir, condition);

        final String ifTrueStart = newBranch();
        st.add("members", "// IF " + condition.toString());
        buildAction(ifTrueStart);

        buildTransition(current, ifTrueStart, guardPrinter.getGuard());

        final String ifTrueStop = buildControlFlowForStmts(st, group, ifTrueStart, stmts);
        if (null != ifTrueStop) {
          buildTransition(ifTrueStop, join);
        }

        final String ifFalseStart = newBranch();
        st.add("members", "// IF NOT " + condition.toString());
        buildAction(ifFalseStart);

        buildTransition(current, ifFalseStart, guardPrinter.getNegatedGuard());
        current = ifFalseStart;
      }

      current = buildControlFlowForStmts(st, group, current, stmt.getElseBlock());
      if (null != current) {
        buildTransition(current, join);
      }

      st.add("members", "// END IF");
      st.add("members", "");
      return join;
    }

    private void buildStmtMark(final StmtMark stmt) {
      if (null == currentMarks) {
        currentMarks = new ArrayList<>();
      }
      currentMarks.add(stmt.getName());
    }

    private void buildAction(final String name, final String... args) {
      InvariantChecks.checkNotNull(name);

      final ST stDef = group.getInstanceOf("action_def");
      stDef.add("name", name);

      for (final String arg : args) {
        stDef.add("args", arg);
      }

      if (null != currentMarks) {
        for (final String mark : currentMarks) {
          stDef.add("marks", mark);
        }
        currentMarks = null;
      }

      st.add("members", stDef);

      final ST stReg = group.getInstanceOf("action_reg");
      stReg.add("name", name);
      st.add("stmts", stReg);
    }

    private void buildTransition(final String source, final String target) {
      buildTransition(source, target, null);
    }

    private void buildTransition(final String source, final String target, final String guard) {
      InvariantChecks.checkNotNull(source);
      InvariantChecks.checkNotNull(target);

      final String name = String.format("%s_%s_%d", source, target, transitionIndex++);

      final ST stDef = group.getInstanceOf("transition_def");
      stDef.add("name", name);
      stDef.add("source", source);
      stDef.add("target", target);
      if (null != guard) {
        stDef.add("guard", guard);
      }
      st.add("members", stDef);

      final ST stReg = group.getInstanceOf("transition_reg");
      stReg.add("name", name);
      st.add("stmts", stReg);
    }

    private String newBranch() {
      return String.format("BRANCH_%d", actionIndex++);
    }

    private String newJoin() {
      return String.format("JOIN_%d", actionIndex++);
    }

    private String newAssign() {
      return String.format("ASSIGN_%d", actionIndex++);
    }

    private boolean isDataVariable(final Node expr) {
      if (expr.getKind() != Node.Kind.VARIABLE) {
        return false;
      }

      if (expr.getUserData() instanceof Variable) {
        final Variable variable = (Variable) expr.getUserData();
        for (final Memory memory : ir.getMemories().values()) {
          if (memory.getDataArg().equals(variable)) {
            return true;
          }
        }
      }

      return false;
    }

    private boolean isAddressTranslation(final Node expr) {
      if (expr.getUserData() instanceof AttributeRef) {
        final AttributeRef ref = (AttributeRef) expr.getUserData();
        return ref.getTarget() instanceof Segment;
      }
      return false;
    }
  }
}
