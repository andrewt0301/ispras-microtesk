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

import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.mmu.model.api.PolicyId;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Buffer;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Segment;
import ru.ispras.microtesk.mmu.translator.ir.Type;
import ru.ispras.microtesk.translator.generation.STBuilder;

public final class STBSpecification implements STBuilder {
  public static final String CLASS_NAME = "Specification";

  private static final Class<?> SPEC_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem.class;

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
  }

  private void buildAddresses(final ST st, final STGroup group) {
    final ST stSeparator = group.getInstanceOf("separator");
    stSeparator.add("text", "Addresses");
    st.add("members", stSeparator);
    st.add("stmts", "");

    for(final Address address : ir.getAddresses().values()) {
      final String name = address.getId();
      buildFields(name, address.getContentType(), st, null, group);

      final ST stDef = group.getInstanceOf("type_def");
      stDef.add("name", name);
      stDef.add("value_name", name + "_" + Utils.listToString(address.getAccessChain(), '_'));
      st.add("members", stDef);

      final ST stReg = group.getInstanceOf("type_reg");
      stReg.add("name", name);
      st.add("stmts", stReg);
    }
  }

  private void buildSegments(final ST st, final STGroup group) {
    final ST stSeparator = group.getInstanceOf("separator");
    stSeparator.add("text", "Segments");
    st.add("members", stSeparator);
    st.add("stmts", "");

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
    final ST stSeparator = group.getInstanceOf("separator");
    stSeparator.add("text", "Buffers");
    st.add("members", stSeparator);
    st.add("stmts", "");

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
      stDef.add("match", "null");
      stDef.add("guard_cond", "null");
      stDef.add("guard", "null");
      stDef.add("replaceable", Boolean.toString(buffer.getPolicy() != PolicyId.NONE));
      stDef.add("parent", buffer.getParent() != null ? buffer.getParent().getId() : "null");
      st.add("members", stDef);

      final ST stReg = group.getInstanceOf("buffer_reg");
      stReg.add("name", buffer.getId());
      st.add("stmts", stReg);
    }
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

  private static String toMmuExpressionText(final List<IntegerField> fields) {
    if (fields.isEmpty()) {
      return "MmuExpression.empty()";
    }

    if (fields.size() == 1) {
      final IntegerField field = fields.get(0);
      final String name = field.getVariable().getName().replace('.', '_');

      if (field.getWidth() == field.getVariable().getWidth()) {
        return String.format("MmuExpression.var(%s)", name);
      } else {
        return String.format("MmuExpression.var(%s, %d, %d)",
            name, field.getLoIndex(), field.getHiIndex());
      }
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("MmuExpression.rcat(");

    boolean isFirst = true;
    for (final IntegerField field : fields) {
      final String name = field.getVariable().getName().replace('.', '_');

      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(", ");
      }

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
}
