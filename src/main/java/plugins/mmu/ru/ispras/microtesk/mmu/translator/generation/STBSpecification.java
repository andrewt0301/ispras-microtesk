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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.model.api.PolicyId;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
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
    st.add("imps", String.format("%s.*", INTEGER_CLASS.getPackage().getName()));
    st.add("imps", String.format("%s.*", MMU_OPERATION_CLASS.getPackage().getName()));
    st.add("imps", String.format("%s.*", SPEC_CLASS.getPackage().getName()));
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("body");

    stBody.add("name", CLASS_NAME);
    stBody.add("spec", SPEC_CLASS.getSimpleName());
    st.add("members", stBody);

    buildAddresses(stBody, group);
    buildSegments(stBody, group);
    buildBuffers(stBody, group);
    buildControlFlow(stBody, group);
  }

  private static void buildSeparator(
      final ST st,
      final STGroup group,
      final String text) {
    final ST stSeparator = group.getInstanceOf("separator");
    stSeparator.add("text", text);
    st.add("members", stSeparator);
    st.add("stmts", "");
  }

  private void buildAddresses(final ST st, final STGroup group) {
    buildSeparator(st, group, "Addresses");

    for(final Address address : ir.getAddresses().values()) {
      final String name = address.getId();
      final ST stDef = group.getInstanceOf("address_def");

      buildFields(name, address.getContentType(), st, stDef, group);

      stDef.add("name", name);
      stDef.add("value_name", name + "_" + Utils.listToString(address.getAccessChain(), '_'));
      st.add("members", stDef);

      final ST stReg = group.getInstanceOf("address_reg");
      stReg.add("name", name);
      st.add("stmts", stReg);
    }
  }

  private void buildSegments(final ST st, final STGroup group) {
    buildSeparator(st, group, "Segments");

    for(final Segment segment : ir.getSegments().values()) {
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

  private void buildBuffers(final ST st, final STGroup group) {
    buildSeparator(st, group, "Buffers");

    for(final Buffer buffer : ir.getBuffers().values()) {
      final ST stDef = group.getInstanceOf("buffer_def");
      buildFields(buffer.getId(), buffer.getEntry(), st, stDef, group);

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

  private void buildControlFlow(final ST st, final STGroup group) {
    final Map<String, Memory> memories = ir.getMemories();
    if (memories.size() > 1) {
      throw new IllegalStateException("Only one mmu specification is allowed.");
    }

    final Memory memory = memories.values().iterator().next();
    buildSeparator(st, group, String.format("Control Flow (%s)", memory.getId()));

    st.add("stmts", String.format("builder.setVirtualAddress(%s);", memory.getAddress().getId()));

    buildFields(memory.getDataArg().getName(), memory.getDataArg().getType(), st, null, group);
    st.add("members", "");

    for (final Variable variable : memory.getVariables()) {
      buildFields(variable.getName(), variable.getType(), st, null, group);
    }

    st.add("members", "");
    st.add("stmts", "");

    buildAction(st, group, "ROOT",
        String.format("new MmuBinding(%s.getVariable())", memory.getAddress().getId()));
    st.add("stmts", "builder.setStartAction(ROOT);");
    st.add("stmts", "");

    buildAction(st, group, "START");
    st.add("stmts", "builder.registerTransition(new MmuTransition(ROOT, START, new MmuGuard(MemoryOperation.LOAD)));");
    st.add("stmts", "");

    buildAction(st, group, "STOP");
    st.add("stmts", "builder.registerTransition(new MmuTransition(ROOT, START, new MmuGuard(MemoryOperation.STORE)));");
    st.add("stmts", "");

    buildControlFlowForAttribute(st, group, "START", memory, AbstractStorage.READ_ATTR_NAME);
    buildControlFlowForAttribute(st, group, "START", memory, AbstractStorage.WRITE_ATTR_NAME);
  }

  private void buildControlFlowForAttribute(
      final ST st,
      final STGroup group,
      final String start,
      final Memory memory,
      final String attributeName) {
    final Attribute attribute = memory.getAttribute(attributeName);
    if (null == attribute) {
      throw new IllegalStateException(String.format(
          "Undefined attribute: %s.%s", memory.getId(), attributeName));
    }

    final String stop = buildControlFlowForStmts(st, group, start, attribute.getStmts());
    if (null != stop) {
      st.add("stmts", String.format(
          "builder.registerTransition(new MmuTransition(%s, STOP));", stop));
    }
  }

  private String buildControlFlowForStmts(
      final ST st,
      final STGroup group,
      final String start,
      final List<Stmt> stmts) {
    String current = start;

    for (final Stmt stmt : stmts) {
      switch(stmt.getKind()) {
        case ASSIGN:
          current = buildStmtAssign(st, group, current, (StmtAssign) stmt);
          break;

        case EXCEPT:
          buildStmtException(st, group, current, (StmtException) stmt);
          return null; // Control flow cannot be continued after exception.

        case IF:
          current = buildStmtIf(st, group, current, (StmtIf) stmt);
          break;
          
        case MARK:
          buildStmtMark(st, group, (StmtMark) stmt);
          break;

        case TRACE: // Ignored
          break;

        default:
          throw new IllegalStateException("Unknown statement: " + stmt.getKind());
      }
    }

    return current;
  }

  private void buildStmtException(
      final ST st,
      final STGroup group,
      final String current,
      final StmtException stmt) {
    // TODO Auto-generated method stub
  }

  private String buildStmtIf(
      final ST st,
      final STGroup group,
      final String current,
      final StmtIf stmt) {
    // TODO Auto-generated method stub
    return null;
  }

  private String buildStmtAssign(
      final ST st,
      final STGroup group,
      final String current,
      final StmtAssign stmt) {
    // TODO Auto-generated method stub
    return null;
  }

  private void buildStmtMark(
      final ST st,
      final STGroup group,
      final StmtMark stmt) {
    // TODO Auto-generated method stub
  }

  private static void buildFields(
      final String name,
      final Type type,
      final ST st,
      final ST stBuffer,
      final STGroup group) {
    if (type.isStruct()) {
      for (final Map.Entry<String, Type> field : type.getFields().entrySet()) {
        buildFields(
            name + "." + field.getKey(),
            field.getValue(),
            st,
            stBuffer,
            group
            );
      }
    } else {
      final String id = name.replace('.', '_');

      if (null != stBuffer) {
        stBuffer.add("fields", id);
      }

      final ST stVariable = group.getInstanceOf("variable_def");
      stVariable.add("id", id);
      stVariable.add("name", name);
      stVariable.add("size", type.getBitSize());
      st.add("members", stVariable);
    }
  }

  private static void buildAction(
      final ST st,
      final STGroup group,
      final String name,
      final String... args) {
    final ST stDef = group.getInstanceOf("action_def");
    stDef.add("name", name);
    for (final String arg : args) {
      stDef.add("args", arg);
    }
    st.add("members", stDef);

    final ST stReg = group.getInstanceOf("action_reg");
    stReg.add("name", name);
    st.add("stmts", stReg);
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
      sb.append("new IntegerField(");
      sb.append(name);
      sb.append(", ");
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
      sb.append(String.format("val(new %s(\"%s\", 10), %d",
          BigInteger.class.getName(), field.getVariable().getValue(), field.getWidth()));
    } else {
      final String name = field.getVariable().getName().replace('.', '_');
      sb.append(String.format("var(%s", name));

      if (field.getWidth() != field.getVariable().getWidth()) {
        sb.append(String.format(", %d, %d", name, field.getLoIndex(), field.getHiIndex()));
      }
    }

    sb.append(')');
    return sb.toString();
  }

  private static String toMmuBindingsText(
      final List<Pair<IntegerVariable, IntegerField>> bindings) {

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s.<MmuBinding>asList(", Arrays.class.getName()));

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
}
