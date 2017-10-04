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

package ru.ispras.microtesk.mmu.translator.generation.spec;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.Variable;
import ru.ispras.microtesk.basis.solver.integer.IntegerUtils;

public class IntegerFieldTrackerTestCase {
  @Test
  public void test() {
    final Variable var = new Variable("VA", DataType.BIT_VECTOR(32)); 
    final IntegerFieldTracker tracker = new IntegerFieldTracker(var);

    assertEquals(Collections.singletonList(
        IntegerUtils.makeNodeExtract(var, 0, 31)), tracker.getFields());

    tracker.exclude(8, 15);
    assertEquals(Arrays.asList(
        IntegerUtils.makeNodeExtract(var, 0, 7),
        IntegerUtils.makeNodeExtract(var, 16, 31)),
        tracker.getFields());

    tracker.exclude(12, 23);
    assertEquals(Arrays.asList(
        IntegerUtils.makeNodeExtract(var, 0, 7),
        IntegerUtils.makeNodeExtract(var, 24, 31)),
        tracker.getFields());

    tracker.exclude(31, 31);
    assertEquals(Arrays.asList(
        IntegerUtils.makeNodeExtract(var, 0, 7),
        IntegerUtils.makeNodeExtract(var, 24, 30)),
        tracker.getFields());

    tracker.exclude(0, 0);
    assertEquals(Arrays.asList(
        IntegerUtils.makeNodeExtract(var, 1, 7),
        IntegerUtils.makeNodeExtract(var, 24, 30)),
        tracker.getFields());

    tracker.excludeAll();
    assertEquals(Collections.emptyList(), tracker.getFields());
  }

  @Test
  public void test2() {
    final Variable var = new Variable("PA", DataType.BIT_VECTOR(36)); 
    final IntegerFieldTracker tracker = new IntegerFieldTracker(var);

    tracker.exclude(5, 11);
    tracker.exclude(12, 35);

    assertEquals(Collections.singletonList(
        IntegerUtils.makeNodeExtract(var, 0, 4)),
        tracker.getFields());
  }

  @Test
  public void test3() {
    final Variable var = new Variable("PA", DataType.BIT_VECTOR(36)); 
    final IntegerFieldTracker tracker = new IntegerFieldTracker(var);
  
    tracker.exclude(11, 5);
    tracker.exclude(35, 12);

    assertEquals(Collections.singletonList(
        IntegerUtils.makeNodeExtract(var, 0, 4)),
        tracker.getFields());
  }
}
