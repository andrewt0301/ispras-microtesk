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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.mmu.translator.ir.AttributeRef;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.mmu.translator.ir.Segment;
import ru.ispras.microtesk.mmu.translator.ir.Stmt;
import ru.ispras.microtesk.mmu.translator.ir.StmtAssign;
import ru.ispras.microtesk.mmu.translator.ir.StmtException;
import ru.ispras.microtesk.mmu.translator.ir.StmtIf;
import ru.ispras.microtesk.mmu.translator.ir.StmtMark;
import ru.ispras.microtesk.mmu.translator.ir.Variable;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAction;

final class ControlFlowBuilder {
  public static final Class<?> ACTION_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuAction.class;

  public static final Class<?> GUARD_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuGuard.class;

  public static final Class<?> TRANSITION_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuTransition.class;

  private final Ir ir;
  private final String context;

  private final ST st;
  private final STGroup group;
  private final ST stReg;

  private final Set<String> exceptions = new HashSet<>();
  private List<String> currentMarks = null;

  private int branchIndex = 0;
  private int joinIndex = 0;
  private int assignIndex = 0;

  protected ControlFlowBuilder(
      final Ir ir,
      final String context,
      final ST st,
      final STGroup group,
      final ST stReg) {
    InvariantChecks.checkNotNull(ir);
    InvariantChecks.checkNotNull(context);
    InvariantChecks.checkNotNull(st);
    InvariantChecks.checkNotNull(group);
    InvariantChecks.checkNotNull(stReg);

    this.ir = ir;
    this.context = context;
    this.st = st;
    this.group = group;
    this.stReg = stReg;

    st.add("imps", ACTION_CLASS.getName());
    st.add("imps", GUARD_CLASS.getName());
    st.add("imps", TRANSITION_CLASS.getName());
  }

  public void build(
      final String start,
      final String stop,
      final String startRead,
      final List<Stmt> stmtsRead,
      final String startWrite,
      final List<Stmt> stmtsWrite) {
    InvariantChecks.checkNotNull(start);
    InvariantChecks.checkNotNull(stop);
    InvariantChecks.checkNotNull(startRead);
    InvariantChecks.checkNotNull(startWrite);
    InvariantChecks.checkNotNull(stmtsRead);
    InvariantChecks.checkNotNull(stmtsWrite);

    st.add("members", "");

    buildAction(start, true);
    buildAction(stop, true);

    buildAction(startRead);
    buildAction(startWrite);

    buildTransition(start, startRead, "new MmuGuard(MemoryOperation.LOAD)");
    final String stopRead = buildStmts(startRead, stmtsRead);
    if (null != stopRead) {
      buildTransition(stopRead, stop);
    }

    buildTransition(start, startWrite, "new MmuGuard(MemoryOperation.STORE)");
    final String stopWrite = buildStmts(startWrite, stmtsWrite);
    if (null != stopWrite) {
      buildTransition(stopWrite, stop);
    }
  }

  public void build(
      final String start,
      final String stop,
      final List<Stmt> stmts) {
    InvariantChecks.checkNotNull(start);
    InvariantChecks.checkNotNull(stop);
    InvariantChecks.checkNotNull(stmts);

    st.add("members", "");
    buildAction(start, true);
    buildAction(stop, true);

    final String current = buildStmts(start, stmts);
    if (null != current) {
      buildTransition(current, stop);
    }
  }

  private String buildStmts(final String start, final List<Stmt> stmts) {
    InvariantChecks.checkNotNull(start);
    String current = start;

    for (final Stmt stmt : stmts) {
      switch(stmt.getKind()) {
        case ASSIGN:
          current = buildStmtAssign(current, (StmtAssign) stmt);
          break;

        case IF:
          current = buildStmtIf(current, (StmtIf) stmt);
          break;

        case EXCEPT:
          buildStmtException(current, (StmtException) stmt);
          return null; // Control flow cannot be continued after exception.

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
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(stmt);

    final Node left = stmt.getLeft();
    final Node right = stmt.getRight();

    // Assignments that use the "data" MMU variable are ignored.
    if (isDataVariable(left) || isDataVariable(right)) {
      return source;
    }

    if (isSegmentAccess(right)) {
      return buildSegmentAccess(source, left, (AttributeRef) right.getUserData());
    }

    return source;
  }

  private String buildStmtIf(final String source, final StmtIf stmt) {
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(stmt);

    String current = source;

    final String join = newJoin();
    buildAction(join);

    for (final Pair<Node, List<Stmt>> block : stmt.getIfBlocks()) {
      // TODO: FIXME: GUARDS NEEDED !!!

      final Node condition = block.first;
      final List<Stmt> stmts = block.second;

      final String ifTrueStart = newBranch();
      buildAction(ifTrueStart);

      buildTransition(current, ifTrueStart);

      final String ifTrueStop = buildStmts(ifTrueStart, stmts);
      if (null != ifTrueStop) {
        buildTransition(ifTrueStop, join);
      }

      final String ifFalseStart = newBranch();
      buildAction(ifFalseStart);

      buildTransition(current, ifFalseStart);
      current = ifFalseStart;
    }

    current = buildStmts(current, stmt.getElseBlock());
    if (null != current) {
      buildTransition(current, join);
    }

    return join;
  }

  private void buildStmtException(final String source, final StmtException stmt) {
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(stmt);

    final String exception = stmt.getMessage();
    if (!exceptions.contains(exception)) {
      buildAction(exception);
      exceptions.add(exception);
    }

    buildTransition(source, exception);
  }

  private void buildStmtMark(final StmtMark stmt) {
    InvariantChecks.checkNotNull(stmt);

    if (null == currentMarks) {
      currentMarks = new ArrayList<>();
    }
    currentMarks.add(stmt.getName());
  }

  private void buildAction(final String id, final String... args) {
    buildAction(id, false, args);
  }

  private void buildAction(final String id, boolean isPre, final String... args) {
    InvariantChecks.checkNotNull(id);

    final ST stAction = group.getInstanceOf("action");
    stAction.add("id", id);
    stAction.add("name", context + "." + id);

    for (final String arg : args) {
      stAction.add("args", arg);
    }

    if (null != currentMarks) {
      for (final String mark : currentMarks) {
        stAction.add("marks", mark);
      }
      currentMarks = null;
    }

    st.add("members", stAction);

    if (isPre) {
      stReg.add("pres", id);
    } else {
      stReg.add("acts", id);
    }
  }

  private void buildTransition(final String source, final String target) {
    buildTransition(source, target, null);
  }

  private void buildTransition(final String source, final String target, final String guard) {
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(target);

    final ST stTrans = group.getInstanceOf("transition");

    stTrans.add("source", source);
    stTrans.add("target", target);

    if (null != guard) {
      stTrans.add("guard", guard);
    }

    stReg.add("trans", stTrans);
  }

  private String newBranch() {
    return String.format("BRANCH_%d", branchIndex++);
  }

  private String newJoin() {
    return String.format("JOIN_%d", joinIndex++);
  }

  private String newAssign() {
    return String.format("ASSIGN_%d", assignIndex++);
  }

  private boolean isDataVariable(final Node expr) {
    InvariantChecks.checkNotNull(expr);

    final Memory memory = ir.getMemories().get(context);
    if (null == memory) {
      return false;
    }

    if (expr.getKind() != Node.Kind.VARIABLE) {
      return false;
    }

    if (!(expr.getUserData() instanceof Variable)) {
      return false;
    }

    final Variable variable = (Variable) expr.getUserData();
    return variable.equals(memory.getDataArg());
  }

  private boolean isSegmentAccess(final Node expr) {
    InvariantChecks.checkNotNull(expr);

    if (expr.getUserData() instanceof AttributeRef) {
      final AttributeRef ref = (AttributeRef) expr.getUserData();
      return ref.getTarget() instanceof Segment;
    }
    return false;
  }

  private String buildSegmentAccess(final String source, final Node lhs, final AttributeRef rhs) {
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(lhs);
    InvariantChecks.checkNotNull(rhs);

    final String segmentStart = String.format("%s.get().START", rhs.getTarget().getId());
    buildTransition(source, segmentStart);

    final String segmentStop = String.format("%s.get().STOP", rhs.getTarget().getId());
    final String assignResult = newAssign();
 
    buildAction(assignResult); // TODO FIXME BINDINGS!
    buildTransition(segmentStop, assignResult);

    return assignResult;
  }
}
