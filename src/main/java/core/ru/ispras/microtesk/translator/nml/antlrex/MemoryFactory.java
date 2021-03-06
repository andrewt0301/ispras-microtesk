/*
 * Copyright 2012-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.nml.antlrex;

import ru.ispras.castle.util.Logger;
import ru.ispras.microtesk.model.memory.Memory;
import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.symbols.Where;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;
import ru.ispras.microtesk.translator.nml.ir.shared.MemoryAlias;
import ru.ispras.microtesk.translator.nml.ir.shared.MemoryResource;
import ru.ispras.microtesk.translator.nml.ir.shared.Type;

import java.math.BigInteger;

public final class MemoryFactory extends WalkerFactoryBase {

  public MemoryFactory(final WalkerContext context) {
    super(context);
  }

  public MemoryResource createMemory(
      final Where where,
      final Memory.Kind kind,
      final String name,
      final Type type,
      final Expr sizeExpr,
      final boolean shared,
      final MemoryAlias alias) throws SemanticException {

    if (shared && kind == Memory.Kind.VAR) {
      Logger.warning(
          "%s: Variable %s cannot be shared. The keyword will be ignored.", where, name);
    }

    final BigInteger size = sizeExpr != null ? sizeExpr.bigIntegerValue() : BigInteger.ONE;

    if (null == alias) {
      return new MemoryResource(kind, name, type, size, shared, null);
    }

    final BigInteger bitSize = size.multiply(BigInteger.valueOf(type.getBitSize()));
    final int aliasBitSize;

    if (MemoryAlias.Kind.LOCATION == alias.getKind()) {
      aliasBitSize = alias.getLocation().getType().getBitSize();
    } else { // MemoryAlias.Kind.MEMORY == alias.getKind()
      aliasBitSize = (alias.getMax() - alias.getMin() + 1)
          * alias.getMemory().getType().getBitSize();
    }

    if (!bitSize.equals(BigInteger.valueOf(aliasBitSize))) {
      raiseError(where, String.format(
          "Size of the alias (%d) must be equal to the size of the defined memory (%d).",
          aliasBitSize,
          bitSize
          ));
    }

    return new MemoryResource(kind, name, type, size, shared, alias);
  }

  public MemoryAlias createAlias(
      final Where where,
      final String memoryId,
      final Expr min,
      final Expr max) throws SemanticException {

    final MemoryResource memory = getIr().getMemory().get(memoryId);
    if (null == memory) {
      raiseError(where, memoryId + " is not defined or is not a memory storage.");
    }

    final int minIndex = min.integerValue();
    final int maxIndex = max.integerValue();

    if (!(0 <= minIndex) && (minIndex < memory.getSize().intValue())) {
      raiseError(where, String.format("min (%d) is out of bounds: [0, %d)",
          minIndex, memory.getSize()));
    }

    if (!(0 <= maxIndex) && (maxIndex < memory.getSize().intValue())) {
      raiseError(where, String.format("max (%d) is out of bounds: [0, %d)",
          maxIndex, memory.getSize()));
    }

    return MemoryAlias.forMemory(memoryId, memory, minIndex, maxIndex);
  }
}
