/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api.memory;

import static ru.ispras.fortress.util.InvariantChecks.checkBounds;
import static ru.ispras.fortress.util.InvariantChecks.checkGreaterThanZero;
import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.microtesk.model.api.data.Data;
import ru.ispras.microtesk.model.api.type.Type;

public abstract class Memory {
  private static final Map<String, Memory> INSTANCES = new HashMap<>();

  private final Kind kind;
  private final String name;
  private final Type type;
  private final int length;
  private final boolean isAlias;

  public static enum Kind {
    REG, MEM, VAR
  }

  public static Memory def(
      final Kind kind,
      final String name,
      final Type type,
      final int length) {
    return def(kind, name, type, length, null);
  }

  public static Memory def(
      final Kind kind,
      final String name,
      final Type type,
      final int length,
      final Location alias) {

    if (INSTANCES.containsKey(name)) {
      throw new IllegalArgumentException(name + " is already defined!");
    }

    final Memory result = (null == alias) ?
        new MemoryDirect(kind, name, type, length) :
        new MemoryAlias(kind, name, type, length, alias);

    INSTANCES.put(name, result);
    return result;
  }

  public static Memory get(final String name) {

    final Memory result = INSTANCES.get(name);
    if (null == result) {
      throw new IllegalArgumentException(name + " is not defined!");
    }

    return result;
  }

  private Memory(
      final Kind kind,
      final String name,
      final Type type,
      final int length,
      final boolean isAlias) {

    checkNotNull(kind);
    checkNotNull(name);
    checkNotNull(type);
    checkGreaterThanZero(length);

    this.kind = kind;
    this.name = name;
    this.type = type;
    this.length = length;
    this.isAlias = isAlias;
  }

  public final Kind getKind() {
    return kind;
  }

  public final String getName() {
    return name;
  }

  public final Type getType() {
    return type;
  }

  public final int getLength() {
    return length;
  }

  public final boolean isAlias() {
    return isAlias;
  }

  public final Location access() {
    return access(0);
  }

  public abstract Location access(int address);
  public abstract Location access(long address);
  public abstract Location access(Data address);

  public abstract void reset();
  public abstract MemoryAllocator newAllocator(int addressableUnitBitSize);

  @Override
  public String toString() {
    return String.format("%s %s[%d, %s], alias=%b",
        kind.name().toLowerCase(), name, length, type, isAlias);
  }

  private static final class MemoryDirect extends Memory {
    private final MemoryStorage storage;

    public MemoryDirect(
        final Kind kind,
        final String name,
        final Type type,
        final int length) {
      super(kind, name, type, length, false);
      this.storage = new MemoryStorage(length, type.getBitSize()).setId(name);
    }

    @Override
    public Location access(final int index) {
      checkBounds(index, getLength());
      return access((long) index);
    }

    @Override
    public Location access(final long index) {
      final BitVector address = BitVector.valueOf(index, storage.getAddressBitSize());
      return Location.newLocationForRegion(getType(), storage, address);
    }

    @Override
    public Location access(final Data address) {
      checkNotNull(address);
      return Location.newLocationForRegion(getType(), storage, address.getRawData());
    }

    @Override
    public void reset() {
      storage.reset();
    }

    @Override
    public final MemoryAllocator newAllocator(final int addressableUnitBitSize) {
      return new MemoryAllocator(storage, addressableUnitBitSize);
    }
  }

  private static final class MemoryAlias extends Memory {
    private final Location source;

    public MemoryAlias(
        final Kind kind,
        final String name,
        final Type type,
        final int length,
        final Location source) {
      super(kind, name, type, length, true);
      checkNotNull(source);

      final int totalBitSize = type.getBitSize() * length;
      if (source.getBitSize() != totalBitSize) {
        throw new IllegalArgumentException();
      }

      this.source = source;
    }

    @Override
    public Location access(final long address) {
      return access((int) address);
    }

    @Override
    public Location access(final int index) {
      checkBounds(index, getLength());

      final int locationBitSize = getType().getBitSize();
      final int start = locationBitSize * index;
      final int end = start + locationBitSize - 1;

      final Location bitField = source.bitField(start, end);
      return bitField.castTo(getType().getTypeId());
    }

    @Override
    public Location access(final Data address) {
      return access(address.getRawData().intValue());
    }

    @Override
    public void reset() {
      // Does not work for aliases (and should not be called)
    }

    @Override
    public final MemoryAllocator newAllocator(final int addressableUnitBitSize) {
      throw new UnsupportedOperationException("Allocators are not supported for aliases.");
    }
  }

  private static final class MemoryAliasMapping extends Memory {
    private final int itemBitSize;
    private final int sourceItemBitSize;
    private final Memory source;
    private final int base; 

    public MemoryAliasMapping(
        final Kind kind,
        final String name,
        final Type type,
        final int length,
        final Memory source,
        final int min, 
        final int max
        ) {
      super(kind, name, type, length, true);
      checkNotNull(source);

      checkBounds(min, source.getLength());
      checkBounds(max, source.getLength());

      this.itemBitSize = type.getBitSize();
      this.sourceItemBitSize = source.getType().getBitSize();

      if (itemBitSize > sourceItemBitSize) {
        throw new IllegalArgumentException(String.format(
            "Alias data size (%d) must be greater or equal to %s data size (%d).",
            sourceItemBitSize, name, itemBitSize));
      }

      final int bitSize = itemBitSize * length;
      final int sourceLength = Math.abs(max - min) + 1;
      final int sourceBitSize = sourceItemBitSize * sourceLength;

      if (bitSize != sourceBitSize) {
        throw new IllegalArgumentException(String.format(
            "Alias size mismatch. Expected alias size: %d.", bitSize));
      }

      this.source = source;
      this.base = Math.min(min, max);
    }

    @Override
    public Location access(final int address) {
      // TODO

      /*
      
      if (itemBitSize == sourceItemBitSize) {
        return source.access(base + address);
      }

      final int sourceAddress = 
          base  + (address * itemBitSize) / sourceItemBitSize;

      final Location sourceLocation = source.access(sourceAddress);
      final int start = address * itemBitSize - sourceAddress * sourceItemBitSize;

      return sourceLocation.bitField(start, start + itemBitSize - 1);
      */

      return null;
    }

    @Override
    public Location access(final long address) {
      // TODO

      /*
      if (itemBitSize == sourceItemBitSize) {
        return source.access(base + address);
      }
      */

      return null;

      /*
      final long sourceAddress = 
          min  + (address * itemBitSize) / sourceItemBitSize;
      */
      
      //return source.access(min + address);
    }

    @Override
    public Location access(final Data address) {
      /*
      if (itemBitSize == sourceItemBitSize) {
        
      }
      */

      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void reset() {
      // Does not work for aliases (and should not be called)
    }

    @Override
    public MemoryAllocator newAllocator(final int addressableUnitBitSize) {
      throw new UnsupportedOperationException(
          "Allocators are not supported for aliases.");
    }
  }
}
