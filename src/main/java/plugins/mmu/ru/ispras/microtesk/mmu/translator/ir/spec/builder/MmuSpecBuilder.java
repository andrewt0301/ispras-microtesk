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

package ru.ispras.microtesk.mmu.translator.ir.spec.builder;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.MemoryOperation;
import ru.ispras.microtesk.mmu.model.api.PolicyId;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
import ru.ispras.microtesk.mmu.translator.ir.AttributeRef;
import ru.ispras.microtesk.mmu.translator.ir.Buffer;
import ru.ispras.microtesk.mmu.translator.ir.Field;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.mmu.translator.ir.Segment;
import ru.ispras.microtesk.mmu.translator.ir.Stmt;
import ru.ispras.microtesk.mmu.translator.ir.StmtAssign;
import ru.ispras.microtesk.mmu.translator.ir.StmtException;
import ru.ispras.microtesk.mmu.translator.ir.StmtIf;
import ru.ispras.microtesk.mmu.translator.ir.Variable;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAction;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAddressType;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAssignment;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuExpression;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuGuard;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuTransition;
import ru.ispras.microtesk.translator.TranslatorHandler;

/**
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 *
 */

public final class MmuSpecBuilder implements TranslatorHandler<Ir> {
  /** Action node where the control flow graph terminates if no exceptions are raised. */
  public static final MmuAction STOP = new MmuAction("STOP");

  private MmuSubsystem spec = null;
  private IntegerVariableTracker variables = null;
  private AtomExtractor atomExtractor = null;

  // Data variable for MMU (assignments to it must be ignored when building the control flow)
  private IntegerVariable data = null;

  /** Index used in automatically generated action names to ensure their uniqueness. */
  private int actionIndex = 0;

  private Deque<String> prefixStack = null;
  private Map<String, Integer> prefixVersion = null;

  public MmuSubsystem getSpecification() {
    return spec;
  }

  @Override
  public void processIr(final Ir ir) {
    System.out.println(ir);

    this.spec = new MmuSubsystem();
    this.variables = new IntegerVariableTracker();
    this.atomExtractor = new AtomExtractor("", variables);
    this.actionIndex = 0;
    this.prefixStack = new ArrayDeque<>();
    this.prefixVersion = new HashMap<>();

    prefixStack.push("");

    for (final Address address : ir.getAddresses().values()) {
      registerAddress(address);
    }

    for (final Buffer buffer : ir.getBuffers().values()) {
      registerDevice(buffer);
    }

    final Map<String, Memory> memories = ir.getMemories();
    if (memories.size() > 1) {
      throw new IllegalStateException("Only one load/store specification is allowed.");
    }

    final Memory memory = memories.values().iterator().next();

    final MmuAddressType address = spec.getAddress(memory.getAddress().getId());
    spec.setStartAddress(address);
    variables.defineGroupAs(address.getStruct(), memory.getAddressArg().getId());

    data = new IntegerVariable(memory.getDataArg().getId(), memory.getDataArg().getBitSize());
    variables.defineVariable(data);

    for(final Variable variable : memory.getVariables()) {
      variables.defineVariable(variable);
    }

    registerControlFlowForMemory(memory);

    System.out.println("---------------------------------");
    System.out.println(spec);
  }

  private MmuAction newBranch(String text) {
    return new MmuAction(String.format("Branch_%d[%s]", actionIndex++, text));
  }

  private MmuAction newJoin() {
    return new MmuAction(String.format("Join_%d", actionIndex++));
  }

  private void registerAddress(final Address address) {
    final Variable var = new Variable(address.getId(), address.getType());
    final IntegerVariableGroup addressVar = new IntegerVariableGroup(var);

    variables.defineGroup(addressVar);
    spec.registerAddress(new MmuAddressType(addressVar));
  }

  private void registerDevice(final Buffer buffer) {
    final MmuAddressType address = spec.getAddress(buffer.getAddress().getId());
    final boolean isReplaceable = PolicyId.NONE != buffer.getPolicy();

    final String addressArgName = buffer.getAddressArg().getId();
    variables.defineVariableAs(address.getVariable(), addressArgName);

    try {
      final AddressFormatExtractor addressFormat = new AddressFormatExtractor(
          variables, address.getVariable(), buffer.getIndex(), buffer.getMatch());

      final MmuBuffer parentDevice = (null != buffer.getParent()) ?
          spec.getDevice(buffer.getParent().getId()) : null;

      final MmuBuffer device = new MmuBuffer(
          buffer.getId(),
          buffer.getWays().longValue(),
          buffer.getSets().longValue(),
          address,
          addressFormat.getTagExpr(),
          addressFormat.getIndexExpr(),
          addressFormat.getOffsetExpr(),
          null, null, // TODO: Guard
          isReplaceable,
          parentDevice
          );

      for(final Field field : buffer.getEntry().getFields()) {
        final IntegerVariable fieldVar = new IntegerVariable(field.getId(), field.getBitSize());
        device.addField(fieldVar);
      }

      spec.registerDevice(device);
      variables.defineGroup(new IntegerVariableGroup(device));
    } finally {
      variables.undefine(addressArgName);
    }
  }

  private void registerControlFlowForMemory(Memory memory) {
    final MmuAddressType address = spec.getAddress(memory.getAddress().getId());

    final MmuAction root = new MmuAction("ROOT", new MmuAssignment(address.getVariable()));
    spec.registerAction(root);
    spec.setStartAction(root);

    final MmuAction start = new MmuAction("START");
    spec.registerAction(start);

    // The control flow graph terminates in the STOP node if no exceptions are raised.
    spec.registerAction(STOP);

    // The load part of the control flow graph
    spec.registerTransition(new MmuTransition(root, start, new MmuGuard(MemoryOperation.LOAD)));
    registerControlFlowForAttribute(start, memory, AbstractStorage.READ_ATTR_NAME);

    // The store part of the control flow graph
    spec.registerTransition(new MmuTransition(root, start, new MmuGuard(MemoryOperation.STORE)));
    registerControlFlowForAttribute(start, memory, AbstractStorage.WRITE_ATTR_NAME);
  }

  private void registerControlFlowForAttribute(
      final MmuAction source, 
      final Memory memory,
      final String attributeName) {
    final Attribute attribute = memory.getAttribute(attributeName);
    if (null == attribute) {
      throw new IllegalStateException(String.format(
          "Undefined attribute: %s.%s", memory.getId(), attributeName));
    }

    final MmuAction stop = registerControlFlow(source, attribute.getStmts());
    if (null != stop) {
      spec.registerTransition(new MmuTransition(stop, STOP));
    }
  }

  private MmuAction registerControlFlow(final MmuAction source, final List<Stmt> stmts) {
    MmuAction current = source;

    for (final Stmt stmt : stmts) {
      switch(stmt.getKind()) {
        case ASSIGN:
          current = registerAssignment(current, (StmtAssign) stmt);
          break;

        case EXCEPT:
          registerException(current, (StmtException) stmt);
          return null; // Control flow cannot be continued after exception.

        case IF:
          current = registerIf(current, (StmtIf) stmt);
          break;

        case TRACE: // Ignored
          break;

        default:
          throw new IllegalStateException("Unknown statement: " + stmt.getKind());
      }
    }

    return current;
  }

  private MmuAction registerAssignment(final MmuAction source, final StmtAssign stmt) {
    final Node left = stmt.getLeft();
    final Node right = stmt.getRight();

    // Assignments that use the "data" MMU variable are ignored.
    if (isDataVariable(left) || isDataVariable(right)) {
      return source;
    }

    if (isAddressTranslation(right)) {
      return registerCall(source, left, (AttributeRef) right.getUserData());
    }

    final Atom lhs = atomExtractor.extract(left);
    if (Atom.Kind.VARIABLE != lhs.getKind() && 
        Atom.Kind.GROUP != lhs.getKind() &&
        Atom.Kind.FIELD != lhs.getKind()) {
      throw new IllegalArgumentException(left + " cannot be used as left side of assignment.");
    }

    Atom rhs = atomExtractor.extract(right);
    if (Atom.Kind.VALUE == rhs.getKind()) {
      final BigInteger value = (BigInteger) rhs.getObject();
      rhs = Atom.newConcat(MmuExpression.val(value, lhs.getWidth()));
    }

    final String name = String.format("Assignment (%s = %s)", left, right);
    final AssignmentBuilder assignmentBuilder = new AssignmentBuilder(name, lhs, rhs);

    final MmuAction target = assignmentBuilder.build();
    spec.registerAction(target);

    final MmuTransition transition = new MmuTransition(source, target);
    spec.registerTransition(transition);

    return target;
  }

  private MmuAction registerCall(final MmuAction source, final Node lhs, final AttributeRef rhs) {
    final AbstractStorage storage = rhs.getTarget();

    final String prefix = getPrefix(storage.getId());
    for (final Variable var : storage.getVariables()) {
      defineNestedVar(prefix, var);
    }
    final Variable input = defineNestedVar(prefix, storage.getAddressArg());
    final Variable output = defineNestedVar(prefix, storage.getDataArg());

    final MmuAction preAction =
        registerAssignment(source, input.getVariable(), rhs.getAddressArgValue());

    pushPrefix(prefix);
    final MmuAction midAction =
        registerControlFlow(preAction, rhs.getAttribute().getStmts());
    popPrefix();

    return registerAssignment(midAction, lhs, output.getVariable());
  }

  private MmuAction registerAssignment(final MmuAction source, final Node lhs, final Node rhs) {
    return registerAssignment(source, new StmtAssign(lhs, rhs));
  }

  private Variable defineNestedVar(final String prefix, final Variable var) {
    final Variable nested = var.rename(prefix + var.getId());
    variables.defineVariable(nested);
    return nested;
  }

  private String getPrefix(final String suffix) {
    final String source = prefixStack.peek() + suffix;

    Integer version = prefixVersion.get(source);
    if (version == null) {
      version = 0;
    }
    prefixVersion.put(source, version + 1);

    return String.format("%s_%d.", source, version);
  }

  private void pushPrefix(final String prefix) {
    prefixStack.push(prefix);
    atomExtractor = new AtomExtractor(prefix, variables);
  }

  private void popPrefix() {
    prefixStack.pop();
    atomExtractor = new AtomExtractor(prefixStack.peek(), variables);
  }

  private boolean isDataVariable(Node expr) {
    if (expr.getKind() != Node.Kind.VARIABLE) {
      return false;
    }

    final String name = ((NodeVariable) expr).getName();
    final IntegerVariable variable = variables.getVariable(name);

    return data.equals(variable);
  }

  private boolean isAddressTranslation(final Node e) {
    if (e.getUserData() instanceof AttributeRef) {
      final AttributeRef ref = (AttributeRef) e.getUserData();
      return ref.getTarget() instanceof Segment;
    }
    return false;
  }

  private MmuAction registerIf(final MmuAction source, final StmtIf stmt) {
    final MmuAction join = newJoin();
    spec.registerAction(join);

    MmuAction current = source;

    for (final Pair<Node, List<Stmt>> block : stmt.getIfBlocks()) {
      final Node condition = block.first;
      final List<Stmt> stmts = block.second;

      final GuardExtractor guardExtractor = 
          new GuardExtractor(spec, atomExtractor, condition);

      final MmuAction ifTrueStart = newBranch(condition.toString());
      spec.registerAction(ifTrueStart);

      final MmuGuard guardIfTrue = guardExtractor.getGuard();
      spec.registerTransition(new MmuTransition(current, ifTrueStart, guardIfTrue));

      final MmuAction ifTrueStop = registerControlFlow(ifTrueStart, stmts);
      if (null != ifTrueStop) {
        spec.registerTransition(new MmuTransition(ifTrueStop, join));
      }

      final MmuAction ifFalseStart = newBranch("not " + condition.toString());
      spec.registerAction(ifFalseStart);

      final MmuGuard guardIfFalse = guardExtractor.getNegatedGuard();
      spec.registerTransition(new MmuTransition(current, ifFalseStart, guardIfFalse));

      current = ifFalseStart;
    }

    current = registerControlFlow(current, stmt.getElseBlock());
    if (null != current) {
      spec.registerTransition(new MmuTransition(current, join));
    }

    return join;
  }

  private void registerException(final MmuAction source, final StmtException stmt) {
    final String name = stmt.getMessage();

    final MmuAction target = new MmuAction(name);
    spec.registerAction(target);

    final MmuTransition transition = new MmuTransition(source, target);
    spec.registerTransition(transition);
  }
}
