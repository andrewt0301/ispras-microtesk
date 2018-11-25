package ru.ispras.microtesk.translator.mir;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.ExprTreeWalker;
import ru.ispras.fortress.expression.ExprTreeVisitorDefault;
import ru.ispras.fortress.expression.ExprUtils;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;
import ru.ispras.microtesk.translator.nml.ir.expr.Location;
import ru.ispras.microtesk.translator.nml.ir.expr.LocationSource;
import ru.ispras.microtesk.translator.nml.ir.expr.LocationSourceMemory;
import ru.ispras.microtesk.translator.nml.ir.expr.LocationSourcePrimitive;
import ru.ispras.microtesk.translator.nml.ir.expr.NodeInfo;
import ru.ispras.microtesk.translator.nml.ir.expr.TypeCast;
import ru.ispras.microtesk.translator.nml.ir.primitive.Instance;
import ru.ispras.microtesk.translator.nml.ir.primitive.InstanceArgument;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.nml.ir.primitive.PrimitiveAND;
import ru.ispras.microtesk.translator.nml.ir.primitive.Statement;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAssignment;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAttributeCall;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementCondition;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementFunctionCall;
import ru.ispras.microtesk.translator.nml.ir.shared.Alias;
import ru.ispras.microtesk.translator.nml.ir.shared.MemoryExpr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class NmlIrTrans {
  public static MirContext translate(final List<Statement> source) {
    final MirContext ctx = new MirContext();
    translate(ctx.newBlock(), source);

    return ctx;
  }

  private static List<MirBlock> translate(final MirBlock entry, final List<Statement> code) {
    MirBlock ctx = entry;
    List<MirBlock> terminals = Collections.singletonList(ctx);

    for (final Statement s : code) {
      if (terminals.size() > 1) {
        final MirBlock sink = entry.ctx.newBlock();
        for (final MirBlock block : terminals) {
          block.jump(sink);
        }
        ctx = sink;
        terminals = Collections.singletonList(ctx);
      }

      switch (s.getKind()) {
      case ASSIGN:
        translate(ctx, (StatementAssignment) s);
        break;

      case COND:
        terminals = translate(ctx, (StatementCondition) s);
        break;

      case CALL:
      case FUNCALL:
      case FORMAT:
        break;
      }
    }
    return terminals;
  }

  private static void translate(final MirBlock ctx, final StatementAssignment s) {
    if (s.getLeft().isInternalVariable()) {
      return;
    }
    final Operand arg = translate(ctx, s.getRight().getNode());
    final Operand zero = new Constant(s.getRight().getNode().getDataType().getSize(), 0);
    final Rvalue rhs = BvOpcode.Add.make(arg, zero);

    final Node node = s.getLeft().getNode();
    if (ExprUtils.isVariable(node)) {
      translateAccess(ctx, locationOf(node), new WriteAccess(ctx, rhs));
    } else if (ExprUtils.isOperation(node)) {
      final int size = sizeOf(node);
      final List<Node> operands = operandsOf(node);
      final Local rvalue = ctx.assignLocal(rhs);

      int offset = size;
      for (final Node lhs : operands) {
        offset -= sizeOf(lhs);

        final Rvalue field = BvOpcode.Lshr.make(rvalue, valueOf(offset, size));
        translateAccess(ctx, locationOf(lhs), new WriteAccess(ctx, field));
      }
    }
  }

  private static List<Node> operandsOf(final Node node) {
    if (ExprUtils.isOperation(node)) {
      return ((NodeOperation) node).getOperands();
    }
    return Collections.emptyList();
  }

  private static Operand translate(final MirBlock ctx, final Node node) {
    if (ExprUtils.isOperation(node)) {
      final TransRvalue visitor = new TransRvalue(ctx);
      new ExprTreeWalker(visitor).visit(node);

      return visitor.getResult();
    } else if (ExprUtils.isVariable(node)) {
      return newStatic(node);
    } else if (ExprUtils.isValue(node)) {
      return newConstant(node);
    }
    throw new IllegalStateException();
  }

  private static Constant newConstant(final Node node) {
    final NodeValue value = (NodeValue) node;
    if (node.isType(DataType.INTEGER)) {
      // FIXME
      return new Constant(64, value.getInteger());
    }
    final BitVector bv = value.getBitVector();
    return new Constant(bv.getBitSize(), bv.bigIntegerValue());
  }

  private static Static newStatic(final Node node) {
    return new Static(((NodeVariable) node).getName());
  }

  static BinOpcode mapOpcode(final NodeOperation node) {
    final Enum<?> e = node.getOperationId();
    if (OPCODE_MAPPING.containsKey(e)) {
      return OPCODE_MAPPING.get(e);
    }
    Logger.warning("missing opcode mapping: %s in '%s'", e, node.toString());
    return BvOpcode.Add;
  }

  private static final class TransRvalue extends ExprTreeVisitorDefault {
    private final MirBlock ctx;
    private final Map<Node, Operand> mapped = new IdentityHashMap<>();
    private Operand result;

    public TransRvalue(final MirBlock ctx) {
      this.ctx = ctx;
    }

    public Operand getResult() {
      return result;
    }

    @Override
    public void onOperationEnd(final NodeOperation node) {
      if (!mapped.containsKey(node)) {
        final Operand local;
        if (node.getOperationId() instanceof StandardOperation) {
          switch ((StandardOperation) node.getOperationId()) {
          default:
            local = translateMapping(node);
            break;

          case BVCONCAT:
            local = translateConcat2(node);
            break;
          }
        } else {
          local = translateMapping(node);
        }
        mapped.put(node, local);

        result = local;
      }
    }

    private Operand translateMapping(final NodeOperation node) {
      final BinOpcode opc = mapOpcode(node);
      final Iterator<Node> it = node.getOperands().iterator();
      final DataType type = node.getDataType();

      Operand op1 = lookUp(it.next());
      if (!it.hasNext()) {
        final Local lhs = ctx.newLocal(type);
        final Rvalue rhs = opc.make(op1, null);
        ctx.assign(lhs, rhs);

        op1 = lhs;
      }
      while (it.hasNext()) {
        final Local lhs = ctx.newLocal(type);
        final Rvalue rhs = opc.make(op1, lookUp(it.next()));
        ctx.assign(lhs, rhs);

        op1 = lhs;
      }
      return op1;
    }

    private Rvalue translateMapping2(final NodeOperation node) {
      final BinOpcode opc = mapOpcode(node);
      final Iterator<Node> it = node.getOperands().iterator();
      final DataType type = node.getDataType();

      Operand op1 = lookUp(it.next());
      if (!it.hasNext()) {
        return opc.make(op1, null);
      }

      Operand op2 = lookUp(it.next());
      while (it.hasNext()) {
        final Local lhs = ctx.newLocal(type);
        final Rvalue rhs = opc.make(op1, op2);
        ctx.assign(lhs, rhs);

        op1 = lhs;
        op2 = lookUp(it.next());
      }
      return opc.make(op1, op2);
    }

    private Operand translateConcat2(final NodeOperation node) {
      final Local lhs = ctx.newLocal(node.getDataType());
      final List<Operand> rhs = new ArrayList<>();
      for (final Node operand : node.getOperands()) {
        rhs.add(lookUp(operand));
      }
      ctx.append(new Concat(lhs, rhs));

      return lhs;
    }

    private Operand translateConcat(final NodeOperation node) {
      final Iterator<Node> it = node.getOperands().iterator();
      final DataType type = node.getDataType();

      Operand expr = lookUp(it.next());
      while (it.hasNext()) {
        final Node op = it.next();

        final Rvalue shift =
          BvOpcode.Shl.make(expr, valueOf(sizeOf(op), sizeOf(type)));

        final Operand zero = new Constant(op.getDataType().getSize(), 0);
        final Rvalue arg = BvOpcode.Add.make(lookUp(op), zero);
        final Rvalue zext = new Zext(type.getSize(), arg);

        final Rvalue bitor =
          BvOpcode.Or.make(ctx.assignLocal(shift), ctx.assignLocal(zext));

        expr = ctx.assignLocal(bitor);
      }
      return expr;
    }

    private Operand lookUp(final Node node) {
      switch (node.getKind()) {
      case VALUE:
        return newConstant(node);

      case OPERATION:
        return mapped.get(node);

      case VARIABLE:
        return translateAccess(ctx, locationOf(node), new ReadAccess(ctx));
      }
      throw new UnsupportedOperationException();
    }
  }

  private static Location locationOf(final Node node) {
    return (Location) ((NodeInfo) node.getUserData()).getSource();
  }

  private static Constant valueOf(final long value, final int size) {
    return new Constant(size, BigInteger.valueOf(value));
  }

  private static int sizeOf(final Node node) {
    return sizeOf(node.getDataType());
  }

  private static int sizeOf(final Data data) {
    return sizeOf(data.getType());
  }

  private static int sizeOf(final DataType type) {
    return type.getSize();
  }

  private static Lvalue translateAccess(
      final MirBlock ctx,
      final Location l,
      final Accessor client) {
    final LocationSource source = l.getSource();
    final Access access = new Access(ctx, l);

    if (source instanceof LocationSourcePrimitive) {
      final Primitive p = ((LocationSourcePrimitive) source).getPrimitive();
      final Local arg = ctx.getNamedLocal(l.getName());

      if (p.getKind() == Primitive.Kind.MODE) {
        return client.accessMode(arg, access, p);
      } else {
        return client.accessLocal(arg, access);
      }
    } else if (source instanceof LocationSourceMemory) {
      final MemoryExpr mem = sourceToMemory(source);
      final Lvalue ref = new Static(mem.getName());
      return client.accessMemory(ref, access);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static MemoryExpr sourceToMemory(final LocationSource source) {
    return ((LocationSourceMemory) source).getMemory();
  }

  private static abstract class Accessor {
    protected final MirBlock ctx;

    Accessor(final MirBlock ctx) {
      this.ctx = ctx;
    }

    abstract Lvalue accessMode(Local source, Access access, Primitive p);
    abstract Lvalue accessLocal(Local source, Access access);
    abstract Lvalue accessMemory(Lvalue source, Access access);

    protected Lvalue index(final Lvalue src, final Access access) {
      if (access.index != null) {
        return new Index(src, access.index);
      }
      return src;
    }

    protected Lvalue extract(final Lvalue src, final Access access) {
      if (access.lo != null) {
        return ctx.extract(src, access.lo, access.hi);
      }
      return src;
    }
  }

  private static class Access {
    public final Local index;
    public final Operand lo;
    public final Operand hi;
    public final int size;

    public Access(final MirBlock ctx, final Location l) {
      if (l.getBitfield() != null) {
        final Location.Bitfield bits = l.getBitfield();
        this.lo = translate(ctx, bits.getFrom().getNode());
        this.hi = translate(ctx, bits.getTo().getNode());
        this.size = bits.getType().getBitSize();
      } else {
        this.lo = null;
        this.hi = null;
        this.size = l.getType().getBitSize();
      }

      BigInteger length = BigInteger.ZERO;
      if (l.getIndex() != null) {
        final MemoryExpr mem = sourceToMemory(l.getSource());
        length = mem.getSize();
      }
      if (length.compareTo(BigInteger.ONE) > 0) {
        this.index = ctx.assignLocal(translate(ctx, l.getIndex().getNode()));
      } else {
        this.index = null;
      }
    }
  }

  private static class ReadAccess extends Accessor {
    ReadAccess(final MirBlock ctx) {
      super(ctx);
    }

    @Override
    public Lvalue accessLocal(final Local source, final Access access) {
      return extract(index(source, access), access);
    }

    @Override
    public Lvalue accessMode(final Local source, final Access access, final Primitive p) {
      final DataType type = TypeCast.getFortressDataType(p.getReturnType());
      final Local value = ctx.newLocal(type);
      ctx.append(new Call(source, Collections.<Operand>emptyList(), value));

      return accessLocal(value, access);
    }

    @Override
    public Lvalue accessMemory(final Lvalue mem, final Access access) {
      final Lvalue source = index(mem, access);
      final Local target = ctx.newLocal(null);
      ctx.append(new Load(source, target));

      return extract(target, access);
    }
  }

  private static class WriteAccess extends Accessor {
    final Rvalue value;

    WriteAccess(final MirBlock ctx, final Rvalue value) {
      super(ctx);
      this.value = value;
    }

    @Override
    public Lvalue accessLocal(final Local target, final Access access) {
      final Lvalue dst = index(target, access);
      write(dst, access);
      return dst;
    }

    @Override
    public Lvalue accessMode(final Local target, final Access access, final Primitive p) {
      final DataType type = TypeCast.getFortressDataType(p.getReturnType());
      final Local value = ctx.newLocal(type);
      accessLocal(value, access);

      final Call call =
        new Call(target, Collections.<Operand>singletonList(value), null);
      ctx.append(call);
      return value;
    }

    @Override
    public Lvalue accessMemory(final Lvalue mem, final Access access) {
      final Lvalue target = index(mem, access);
      final Local value = ctx.newLocal(null);
      if (access.lo != null) {
        ctx.append(new Load(target, value));
        write(value, access);
      } else {
        ctx.assign(value, this.value);
      }
      ctx.append(new Store(target, value));

      return target;
    }

    private void write(final Lvalue target, final Access access) {
      if (access.lo != null) {
        final Rvalue zext = new Zext(64, value); // FIXME
        final Rvalue rhs = BvOpcode.Shl.make(ctx.assignLocal(zext), access.lo);

        final Operand mask = createMask(target, access);
        final Rvalue clear = BvOpcode.And.make(target, mask);
        final Rvalue store =
          BvOpcode.Or.make(ctx.assignLocal(clear), ctx.assignLocal(rhs));

        ctx.assign(target, store);
      } else {
        ctx.assign(target, value);
      }
    }

    private Operand createMask(final Lvalue source, final Access access) {
      // final Constant ones = valueOf(-1, 1024);
      // final Rvalue not = Opcode.BitXor.make();
      return null;
    }
  }

  private static List<MirBlock> translate(final MirBlock ctx, final StatementCondition s) {
    final List<MirBlock> blocks = new ArrayList<>();

    MirBlock current = ctx;
    for (int i = 0; i < s.getBlockCount(); ++i) {
      final StatementCondition.Block block = s.getBlock(i);
      if (!block.isElseBlock()) {
        final Operand cond = translate(current, block.getCondition().getNode());
        final Pair<MirBlock, MirBlock> target = current.branch(cond);

        blocks.addAll(translate(target.first, block.getStatements()));

        current = target.second;
      } else {
        blocks.addAll(translate(current, block.getStatements()));
      }
    }
    if (!s.getBlock(s.getBlockCount() - 1).isElseBlock()) {
      blocks.add(current);
    }
    return blocks;
  }

  private static final Map<Enum<?>, BinOpcode> OPCODE_MAPPING =
      new IdentityHashMap<>();
  static {
    OPCODE_MAPPING.put(StandardOperation.AND, BvOpcode.And);
    OPCODE_MAPPING.put(StandardOperation.OR, BvOpcode.Or);
    OPCODE_MAPPING.put(StandardOperation.NOT, new BinOpcode() {
      @Override
      public Rvalue make(final Operand lhs, final Operand rhs) {
        return BvOpcode.Xor.make(lhs, new Constant(1, 1));
      }
    });

    OPCODE_MAPPING.put(StandardOperation.EQ, CmpOpcode.Eq);
    OPCODE_MAPPING.put(StandardOperation.NOTEQ, CmpOpcode.Ne);
    OPCODE_MAPPING.put(StandardOperation.BVULT, CmpOpcode.Ult);
    OPCODE_MAPPING.put(StandardOperation.BVUGT, CmpOpcode.Ugt);
    OPCODE_MAPPING.put(StandardOperation.BVULE, CmpOpcode.Ule);
    OPCODE_MAPPING.put(StandardOperation.BVUGE, CmpOpcode.Uge);
    OPCODE_MAPPING.put(StandardOperation.BVSLT, CmpOpcode.Slt);
    OPCODE_MAPPING.put(StandardOperation.BVSGT, CmpOpcode.Sgt);
    OPCODE_MAPPING.put(StandardOperation.BVSLE, CmpOpcode.Sle);
    OPCODE_MAPPING.put(StandardOperation.BVSGE, CmpOpcode.Sge);

    OPCODE_MAPPING.put(StandardOperation.BVADD, BvOpcode.Add);
    OPCODE_MAPPING.put(StandardOperation.BVSUB, BvOpcode.Sub);
    OPCODE_MAPPING.put(StandardOperation.BVMUL, BvOpcode.Mul);
    OPCODE_MAPPING.put(StandardOperation.BVXOR, BvOpcode.Xor);
    OPCODE_MAPPING.put(StandardOperation.BVAND, BvOpcode.And);
    OPCODE_MAPPING.put(StandardOperation.BVOR, BvOpcode.Or);
    OPCODE_MAPPING.put(StandardOperation.BVUDIV, BvOpcode.Udiv);
    OPCODE_MAPPING.put(StandardOperation.BVSDIV, BvOpcode.Sdiv);
    OPCODE_MAPPING.put(StandardOperation.BVUREM, BvOpcode.Urem);
    OPCODE_MAPPING.put(StandardOperation.BVSREM, BvOpcode.Srem);
    OPCODE_MAPPING.put(StandardOperation.BVNOT, new BinOpcode() {
      @Override
      public Rvalue make(final Operand lhs, final Operand rhs) {
        return BvOpcode.Xor.make(lhs, new Constant(64, -1));
      }
    });

    OPCODE_MAPPING.put(StandardOperation.BVLSHL, BvOpcode.Shl);
    OPCODE_MAPPING.put(StandardOperation.BVASHL, BvOpcode.Shl);
    OPCODE_MAPPING.put(StandardOperation.BVASHR, BvOpcode.Ashr);
    OPCODE_MAPPING.put(StandardOperation.BVLSHR, BvOpcode.Lshr);
  }
}
