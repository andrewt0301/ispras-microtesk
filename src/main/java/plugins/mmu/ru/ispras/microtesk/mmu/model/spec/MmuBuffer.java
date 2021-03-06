/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.model.spec;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.basis.AddressView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link MmuBuffer} represents an MMU buffer.
 *
 * @author <a href="mailto:protsenko@ispras.ru">Alexander Protsenko</a>
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public class MmuBuffer extends MmuStruct {
  /**
   * Describes buffer type.
   */
  public enum Kind {
    /** Stand-alone buffer, not mapped to any external resource */
    UNMAPPED (""),

    /** Mapped to memory */
    MEMORY ("memory"),

    /** Mapped to a register array */
    REGISTER ("register");

    private final String text;
    private Kind(final String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }
  }

  /** Buffer kind (stand-alone, mapped to memory/register */
  private final Kind kind;

  /** The number of ways (associativity). */
  private final long ways;
  /** The number of sets. */
  private final long sets;

  /** The MMU address. */
  private final MmuAddressInstance address;

  /** The tag calculation function. */
  private final Node tagExpression;
  /** The index calculation function. */
  private final Node indexExpression;
  /** The offset calculation function. */
  private final Node offsetExpression;

  private Collection<MmuBinding> matchBindings;

  /** The flag indicating whether the device supports data replacement. */
  private final boolean replaceable;

  // TODO: E.g., JTLB for DTLB.
  private final MmuBuffer parent;
  // TODO: E.g., DTLB for JTLB.
  private final List<MmuBuffer> children = new ArrayList<>();

  /** The address view. */
  private final AddressView<BitVector> addressView;

  public MmuBuffer(
      final String name,
      final Kind kind,
      final long ways,
      final long sets,
      final MmuAddressInstance address,
      final Node tagExpression,
      final Node indexExpression,
      final Node offsetExpression,
      final Collection<MmuBinding> matchBindings,
      final boolean replaceable,
      final MmuBuffer parent) {
    super(name);
    setBuffer(this);

    InvariantChecks.checkNotNull(kind);
    InvariantChecks.checkNotNull(address);
    InvariantChecks.checkNotNull(tagExpression);
    InvariantChecks.checkNotNull(indexExpression);
    InvariantChecks.checkNotNull(offsetExpression);
    InvariantChecks.checkNotNull(matchBindings);

    this.kind = kind;

    this.ways = ways;
    this.sets = sets;

    this.address = address;

    this.tagExpression = tagExpression;
    this.indexExpression = indexExpression;
    this.offsetExpression = offsetExpression;
    this.matchBindings = matchBindings;

    this.replaceable = replaceable;

    // TODO:
    this.parent = parent;
    if (parent != null) {
      parent.children.add(this);
    }

    this.addressView = new MmuAddressViewBuilder(
        address,
        tagExpression,
        indexExpression,
        offsetExpression).build();
  }

  /**
   * Returns buffer kind (whether it is stand-alone, mapped to memory/register).
   *
   * @return buffer kind.
   */
  public final Kind getKind() {
    return kind;
  }

  /**
   * Returns the number of ways (associativity).
   *
   * @return the number of ways.
   */
  public final long getWays() {
    return ways;
  }

  /**
   * Returns the number of sets.
   *
   * @return the number of sets.
   */
  public final long getSets() {
    return sets;
  }

  /**
   * Returns the input parameter.
   *
   * @return the input parameter.
   */
  public final MmuAddressInstance getAddress() {
    return address;
  }

  /**
   * Returns the address for the given tag, index and offset.
   *
   * @param tag the tag.
   * @param index the index.
   * @param offset the offset.
   * @return the value of the address.
   */
  public final BitVector getAddress(
      final BitVector tag,
      final BitVector index,
      final BitVector offset) {
    return addressView.getAddress(tag, index, offset);
  }

  /**
   * Returns the tag calculation function.
   *
   * @return the tag calculation function.
   */
  public final Node getTagExpression() {
    return tagExpression;
  }

  /**
   * Returns the index calculation function.
   *
   * @return the index calculation function.
   */
  public final Node getIndexExpression() {
    return indexExpression;
  }

  /**
   * Returns the offset calculation function.
   *
   * @return the offset calculation function.
   */
  public final Node getOffsetExpression() {
    return offsetExpression;
  }

  public final Collection<MmuBinding> getMatchBindings() {
    return matchBindings;
  }

  protected final void setMatchBindings(final Collection<MmuBinding> matchBindings) {
    InvariantChecks.checkNotNull(matchBindings);
    this.matchBindings = matchBindings;
  }

  /**
   * Returns the address view.
   *
   * @return the address view.
   */
  public final AddressView<BitVector> getAddressView() {
    return addressView;
  }

  /**
   * Returns the address tag.
   *
   * @param address the address.
   * @return the value of the tag.
   */
  public final BitVector getTag(final BitVector address) {
    return addressView.getTag(address);
  }

  /**
   * Returns the address index.
   *
   * @param address the address.
   * @return the value of the index.
   */
  public final BitVector getIndex(final BitVector address) {
    return addressView.getIndex(address);
  }

  /**
   * Returns the address offset.
   *
   * @param address the address.
   * @return the value of the offset.
   */
  public final BitVector getOffset(final BitVector address) {
    return addressView.getOffset(address);
  }

  public final BitVector getTagMask() {
    return getAddress(
        getTag(BitVector.valueOf(BigInteger.valueOf(-1L), address.getWidth())),
        getIndex(BitVector.valueOf(0, address.getWidth())),
        getOffset(BitVector.valueOf(0, address.getWidth())));
  }

  public final BitVector getIndexMask() {
    return getAddress(
        getTag(BitVector.valueOf(0, address.getWidth())),
        getIndex(BitVector.valueOf(BigInteger.valueOf(-1L), address.getWidth())),
        getOffset(BitVector.valueOf(0, address.getWidth())));
  }

  public final BitVector getOffsetMask() {
    return getAddress(
        getTag(BitVector.valueOf(0, address.getWidth())),
        getIndex(BitVector.valueOf(0, address.getWidth())),
        getOffset(BitVector.valueOf(BigInteger.valueOf(-1L), address.getWidth())));
  }

  public final boolean isFake() {
    return ways == 1
        && sets == 1
        && tagExpression == null
        && indexExpression == null;
  }

  /**
   * Checks whether the buffer support data replacement.
   *
   * @return {@code true} if the buffer supports data replacement; {@code false} otherwise.
   */
  public final boolean isReplaceable() {
    return replaceable;
  }

  // TODO:
  public final boolean isView() {
    return parent != null;
  }

  // TODO:
  public final MmuBuffer getParent() {
    return parent;
  }

  // TODO:
  public final boolean isParent() {
    return !children.isEmpty();
  }

  // TODO:
  public final List<MmuBuffer> getChildren() {
    return children;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof MmuBuffer)) {
      return false;
    }

    final MmuBuffer r = (MmuBuffer) o;
    return name.equals(r.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
