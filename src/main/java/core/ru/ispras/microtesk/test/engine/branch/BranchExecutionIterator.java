/*
 * Copyright 2009-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.engine.branch;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.testbase.knowledge.iterator.Iterator;

import java.util.List;

/**
 * {@link BranchExecutionIterator} implements a composite iterator of branch structures and
 * execution traces.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BranchExecutionIterator implements Iterator<List<BranchEntry>> {
  private final int maxBranchExecutions;
  private final int maxBlockExecutions;
  private final int maxExecutionTraces;

  private final Iterator<List<BranchEntry>> branchStructureIterator;

  private BranchTraceIterator branchTraceIterator;
  private boolean hasValue;

  public BranchExecutionIterator(
      final Iterator<List<BranchEntry>> branchStructureIterator,
      final int maxBranchExecutions,
      final int maxBlockExecutions,
      final int maxExecutionTraces) {
    InvariantChecks.checkNotNull(branchStructureIterator);
    InvariantChecks.checkTrue(maxBranchExecutions >= 0  || maxBranchExecutions == -1);
    InvariantChecks.checkTrue(maxBlockExecutions >= 0   || maxBlockExecutions == -1);
    InvariantChecks.checkTrue(maxExecutionTraces >= 0   || maxExecutionTraces == -1);
    InvariantChecks.checkTrue(maxBranchExecutions != -1 || maxBlockExecutions != -1);

    this.branchStructureIterator = branchStructureIterator;
    this.maxBranchExecutions = maxBranchExecutions;
    this.maxBlockExecutions = maxBlockExecutions;
    this.maxExecutionTraces = maxExecutionTraces;

    hasValue = false;
  }

  private boolean initBranchStructureIterator() {
    branchStructureIterator.init();

    while (branchStructureIterator.hasValue()) {
      if (initBranchTraceIterator()) {
        return true;
      }

      branchStructureIterator.next();
    }

    return branchStructureIterator.hasValue();
  }

  private boolean initBranchTraceIterator() {
    branchTraceIterator = new BranchTraceIterator(
        branchStructureIterator.value(),
        maxBranchExecutions,
        maxBlockExecutions,
        maxExecutionTraces);

    branchTraceIterator.init();

    return branchTraceIterator.hasValue();
  }

  @Override
  public void init() {
    hasValue = true;

    if (!initBranchStructureIterator()) {
      stop();
      return;
    }
    if (!initBranchTraceIterator()) {
      stop();
      return;
    }
  }

  @Override
  public boolean hasValue() {
    return hasValue;
  }

  @Override
  public List<BranchEntry> value() {
    InvariantChecks.checkTrue(hasValue());
    return branchTraceIterator.value();
  }

  public List<Integer> trace() {
    InvariantChecks.checkTrue(hasValue());
    return branchTraceIterator.trace();
  }

  private boolean nextBranchStructureIterator() {
    if (branchStructureIterator.hasValue()) {
      branchStructureIterator.next();

      while (branchStructureIterator.hasValue()) {
        if (initBranchTraceIterator()) {
          break;
        }

        branchStructureIterator.next();
      }
    }

    return branchStructureIterator.hasValue();
  }

  private boolean nextBranchTraceIterator() {
    if (branchTraceIterator.hasValue()) {
      branchTraceIterator.next();
    }

    return branchTraceIterator.hasValue();
  }

  @Override
  public void next() {
    if (!hasValue()) {
      return;
    }

    if (nextBranchTraceIterator()) {
      return;
    }

    if (nextBranchStructureIterator()) {
      return;
    }

    stop();
  }

  @Override
  public void stop() {
    hasValue = false;
  }

  @Override
  public BranchExecutionIterator clone() {
    throw new UnsupportedOperationException();
  }
}
