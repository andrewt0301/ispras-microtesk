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

package ru.ispras.microtesk.basis.solver.bitvector;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.data.types.bitvector.BitVectorMath;
import ru.ispras.fortress.util.InvariantChecks;

/**
 * {@link BitVectorRange} class represents a non-empty range.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BitVectorRange {
  /**
   * This enumeration contains types of range bounds.
   */
  public static enum RangePointType {
    /** Start point of a range. */
    MIN,
    /** End point of of a range. */
    MAX,
    /** Start/end point of of a range. */
    ALL
  }

  /**
   * Transforms the collection of ranges to the list of disjoint ranges.
   * 
   * @param ranges the collection of ranges.
   * @return the list of disjoint ranges.
   * @throws IllegalArgumentException if {@code ranges} is null.
   */
  public static List<BitVectorRange> divide(final Collection<BitVectorRange> ranges) {
    InvariantChecks.checkNotNull(ranges);

    if (ranges.isEmpty()) {
      return new ArrayList<>();
    }

    // Create a line of the range points.
    final Map<BitVector, RangePointType> line = new HashMap<>();

    for (final BitVectorRange range : ranges) {
      // Add the starting point of the range into the line.
      if (line.containsKey(range.getMin())) {
        RangePointType type = line.get(range.getMin());
        if (type.equals(RangePointType.MAX)) {
          // MIN + MAX = ALL.
          line.put(range.getMin(), RangePointType.ALL);
        }
      } else {
        line.put(range.getMin(), RangePointType.MIN);
      }
      // Add the end point of the range in to the line.
      if (line.containsKey(range.getMax())) {
        RangePointType type = line.get(range.getMax());
        if (type.equals(RangePointType.MIN)) {
          // MAX + MIN = ALL.
          line.put(range.getMax(), RangePointType.ALL);
        }
      } else {
        line.put(range.getMax(), RangePointType.MAX);
      }
    }

    // Divide the line into disjoint ranges.
    final List<BitVectorRange> dividedRanges = new ArrayList<>();
    final SortedSet<BitVector> keys = new TreeSet<BitVector>(line.keySet());

    BitVector minValue = keys.first();
    BitVector startValue = minValue;

    for (final BitVector key : keys) {
      final RangePointType type = line.get(key);
      switch (type) {
        case ALL:
          if (!key.equals(minValue)) {
            dividedRanges.add(
                new BitVectorRange(
                    minValue,
                    BitVectorMath.sub(key, BitVector.valueOf(1, key.getBitSize()))));
          }
          dividedRanges.add(new BitVectorRange(key, key));
          minValue = BitVectorMath.add(key, BitVector.valueOf(1, key.getBitSize()));
          break;
        case MAX:
          dividedRanges.add(new BitVectorRange(minValue, key));
          minValue = BitVectorMath.add(key, BitVector.valueOf(1, key.getBitSize()));
          break;
        case MIN:
          if (!key.equals(startValue) && !minValue.equals(key)) {
            dividedRanges.add(
                new BitVectorRange(
                    minValue,
                    BitVectorMath.sub(key, BitVector.valueOf(1, key.getBitSize()))));
            minValue = key;
          }
          break;
      }
    }

    return dividedRanges;
  }

  /**
   * Selects all ranges from the the given collection that are within the given bounds.
   * 
   * @param ranges the collection of ranges.
   * @param bounds the bounds.
   * @return the ranges included into the bounds.
   * @throws IllegalArgumentException if some of the parameters are null.
   */
  public static Collection<BitVectorRange> select(
      final Collection<BitVectorRange> ranges,
      final BitVectorRange bounds) {
    InvariantChecks.checkNotNull(ranges);
    InvariantChecks.checkNotNull(bounds);

    final Set<BitVectorRange> result = new LinkedHashSet<>();

    for (final BitVectorRange range : ranges) {
      if (bounds.contains(range)) {
        result.add(range);
      }
    }

    return result;
  }

  /** The lower bound of the range. */
  private BitVector min;
  /** The upper bound of the range. */
  private BitVector max;

  /**
   * Constructs a range with the given lower ({@code min}) and upper ({@code max}) bounds.
   * 
   * @param min the lower bound of the range.
   * @param max the upper bound of the range.
   * @throws IllegalArgumentException if {@code min} or {@code max} is null.
   * @throws IllegalArgumentException if ({@code min > max}).
   */
  public BitVectorRange(final BitVector min, final BitVector max) {
    InvariantChecks.checkNotNull(min);
    InvariantChecks.checkNotNull(max);
    InvariantChecks.checkGreaterOrEq(max.bigIntegerValue(false), min.bigIntegerValue(false));

    this.min = min;
    this.max = max;
  }

  /**
   * Constructs a single-value range.
   * 
   * @param value the only value of the range.
   */
  public BitVectorRange(final BitVector value) {
    this(value, value);
  }

  /**
   * Returns the lower bound of the range.
   * 
   * @return the lower bound of the range.
   */
  public BitVector getMin() {
    return min;
  }

  /**
   * Sets the lower bound of the range.
   * 
   * @param min the lower bound to be set.
   * @throws IllegalArgumentException if {@code max} is null or ({@code min > max}).
   */
  public void setMin(final BitVector min) {
    InvariantChecks.checkNotNull(min);
    InvariantChecks.checkGreaterOrEq(this.max.bigIntegerValue(false), min.bigIntegerValue(false));

    this.min = min;
  }

  /**
   * Returns the upper bound of the range.
   * 
   * @return the upper bound of the range.
   */
  public BitVector getMax() {
    return max;
  }

  /**
   * Sets the upper bound of the range.
   * 
   * @param max the upper bound to be set.
   * @throws IllegalArgumentException if {@code max} is null or ({@code min > max}).
   */
  public void setMax(final BitVector max) {
    InvariantChecks.checkNotNull(max);
    InvariantChecks.checkGreaterOrEq(max.bigIntegerValue(false), this.min.bigIntegerValue(false));

    this.max = max;
  }

  /**
   * Returns the size of the range.
   * 
   * @return the size of the range.
   */
  public BigInteger size() {
    return max.bigIntegerValue(false).subtract(min.bigIntegerValue(false)).add(BigInteger.ONE);
  }

  /**
   * Checks whether this range overlaps with the given one ({@code rhs}).
   * 
   * @param rhs the range to be compared with this one.
   * @return {@code true} if this range overlaps with the given one; {@code false} otherwise.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public boolean overlaps(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);
    return min.compareTo(rhs.max) <= 0 && rhs.min.compareTo(max) <= 0;
  }

  /**
   * Checks whether this range contains (as a subset) the given one ({@code rhs}).
   * 
   * @param rhs the range to be compared with this one.
   * @return {@code true} if this range contains the given one; {@code false} otherwise.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public boolean contains(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);
    return min.compareTo(rhs.min) <= 0 && max.compareTo(rhs.max) >= 0;
  }

  /**
   * Checks whether this range contains the given point.
   * 
   * @param value the point.
   * @return {@code true} if this range contains the given point; {@code false} otherwise.
   * @throws IllegalArgumentException if {@code value} is null.
   */
  public boolean contains(final BitVector value) {
    InvariantChecks.checkNotNull(value);
    return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
  }

  /**
   * Returns the intersection of this range with the given one. If the ranges are not overlapping,
   * returns {@code null}.
   * 
   * @param rhs the range to be intersected with this one.
   * @return the range representing the intersection or {@code null} if the ranges are disjoint.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public BitVectorRange intersect(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);

    if (!overlaps(rhs)) {
      return null;
    }

    final BigInteger minValue = min.bigIntegerValue(false).max(rhs.min.bigIntegerValue(false));
    final BigInteger maxValue = max.bigIntegerValue(false).min(rhs.max.bigIntegerValue(false));

    return new BitVectorRange(
        BitVector.valueOf(minValue, min.getBitSize()),
        BitVector.valueOf(maxValue, max.getBitSize()));
  }

  /**
   * Returns the union of this range with the given one. If the ranges are not overlapping,
   * returns {@code null}.
   * 
   * @param rhs the range to be merged with this one.
   * @return the range representing the union or {@code null} if the ranges are disjoint.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public BitVectorRange merge(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);

    if (!overlaps(rhs)) {
      return null;
    }

    final BigInteger minValue = min.bigIntegerValue(false).min(rhs.min.bigIntegerValue(false));
    final BigInteger maxValue = max.bigIntegerValue(false).max(rhs.max.bigIntegerValue(false));

    return new BitVectorRange(
        BitVector.valueOf(minValue, min.getBitSize()),
        BitVector.valueOf(maxValue, max.getBitSize()));
  }

  /**
   * Returns the list of ranges representing the union of this range with the given one.
   * If the ranges are overlapping, the list consists of one range ({@code merge(rhs)});
   * otherwise, it includes two ranges: {@code this} and {@code rhs}.
   * 
   * @param rhs the range to be united with this one.
   * @return the list of ranges representing the union.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public List<BitVectorRange> union(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);

    final List<BitVectorRange> result = new ArrayList<BitVectorRange>();

    if (overlaps(rhs)) {
      result.add(merge(rhs));
    } else {
      result.add(this);
      result.add(rhs);
    }

    return result;
  }

  /**
   * Returns the list of ranges representing the difference between this range and the given one.
   * If the ranges are not overlapping, the list consists of this range; otherwise it may include
   * up to 2 ranges.
   * 
   * @param rhs the range to be subtracted from this one.
   * @return the difference.
   * @throws IllegalArgumentException if {@code rhs} is null.
   */
  public List<BitVectorRange> minus(final BitVectorRange rhs) {
    InvariantChecks.checkNotNull(rhs);

    final List<BitVectorRange> result = new ArrayList<BitVectorRange>();

    if (overlaps(rhs)) {
      final BitVector min1 = min;
      final BitVector max1 = BitVectorMath.sub(rhs.min, BitVector.valueOf(1, min.getBitSize()));

      final BitVector min2 = BitVectorMath.add(rhs.max, BitVector.valueOf(1, min.getBitSize()));
      final BitVector max2 = max;

      if (min1.compareTo(max1) <= 0) {
        result.add(new BitVectorRange(min1, max1));
      }

      if (min2.compareTo(max2) <= 0) {
        result.add(new BitVectorRange(min2, max2));
      }
    } else {
      result.add(this);
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || !(o instanceof BitVectorRange)) {
      return false;
    }

    final BitVectorRange r = (BitVectorRange) o;

    return min.compareTo(r.min) == 0 && max.compareTo(r.max) == 0;
  }

  @Override
  public int hashCode() {
    return 31 * max.hashCode() + min.hashCode();
  }

  @Override
  public String toString() {
    return String.format("[0x%s, 0x%s]", min.toHexString(), max.toHexString());
  }
}