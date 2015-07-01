/*
 * Copyright 2006-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.test.sequence.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.test.sequence.engine.iterator.AbstractSequence;
import ru.ispras.microtesk.mmu.test.sequence.engine.loader.MemoryLoader;
import ru.ispras.microtesk.mmu.translator.coverage.ExecutionPath;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuDevice;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSpecification;

/**
 * {@link MmuSolution} represents a solution (test data) for a number of dependent instruction
 * calls (test template).
 * 
 * <p>Solution includes test data for individual executions (see {@link MmuTestData}) and
 * a set of entries to be written into the devices (buffers).</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MmuSolution {

  /** Contains test data for individual executions. */
  private final List<MmuTestData> solution;

  /** Contains addresses to be accessed to prepare hit/miss situations. */
  private final MemoryLoader loader;

  /**
   * Contains entries to be written into the devices to prepare hit/miss situations.
   * 
   * <p>This map unites the analogous maps of the test data of the executions stored in
   * {@link MmuSolution#solution}.</p>
   */
  private final Map<MmuDevice, Map<Long, /* Entry */ Object>> entries = new LinkedHashMap<>();

  /**
   * Constructs an uninitialized solution for the given test template.
   * 
   * @param memory the MMU specification.
   * @param template the test template.
   */
  public MmuSolution(final MmuSpecification memory, final AbstractSequence template) {
    InvariantChecks.checkNotNull(memory);
    InvariantChecks.checkNotNull(template);

    solution = new ArrayList<>(template.size());
    for (int i = 0; i < template.size(); i++) {
      final ExecutionPath execution = template.getExecution(i);

      solution.add(new MmuTestData(memory, execution));
    }

    for (final MmuDevice device : memory.getDevices()) {
      entries.put(device, new LinkedHashMap<Long, Object>());
    }

    loader = new MemoryLoader(memory);
  }

  /**
   * Returns the number of executions in the test template.
   * 
   * @return the test template size.
   */
  public int size() {
    return solution.size();
  }

  /**
   * Returns the test data for the i-th execution.
   * 
   * @param i the execution index.
   * @return the test data.
   * @throws IndexOutOfBoundsException if {@code i} is out of bounds.
   */
  public MmuTestData getTestData(final int i) {
    InvariantChecks.checkBounds(i, solution.size());

    return solution.get(i);
  }

  /**
   * Returns the test data for all executions.
   * 
   * @return the list of test data.
   */
  public List<MmuTestData> getTestData() {
    return solution;
  }

  /**
   * Sets the test data for the i-th execution.
   * 
   * @param i the execution index.
   * @param testData the test data to be set.
   * @throws IndexOutOfBoundsException if {@code i} is out of bounds.
   */
  public void setTestData(final int i, final MmuTestData testData) {
    InvariantChecks.checkBounds(i, solution.size());
    InvariantChecks.checkNotNull(testData);

    solution.set(i, testData);
  }

  /**
   * Returns the memory loader.
   * 
   * @return the memory loader.
   */
  public MemoryLoader getLoader() {
    return loader;
  }

  /**
   * Returns the entries to written to the given device.
   * 
   * @param device the MMU device (buffer).
   * @return the index-to-entry map.
   * @throws NullPointerException if {@code device} is null.
   */
  public Map<Long, Object> getEntries(final MmuDevice device) {
    InvariantChecks.checkNotNull(device);

    return entries.get(device);
  }

  /**
   * Sets the entries to be written to the given device.
   * 
   * @param device the MMU device (buffer).
   * @param entries the entries to be written.
   * @throws NullPointerException if some parameters are null.
   */
  public void setEntries(final MmuDevice device, final Map<Long, Object> entries) {
    InvariantChecks.checkNotNull(device);
    InvariantChecks.checkNotNull(entries);

    this.entries.put(device, entries);
  }

  /**
   * Adds the entry to the set of entries to be written to the given device.
   * 
   * @param device the MMU device (buffer).
   * @param internalAddress the internal address of the entry (index).
   * @param entry the entry to be added.
   * @throws NullPointerException if some parameters are null.
   */
  public void addEntry(final MmuDevice device, final long internalAddress, final Object entry) {
    InvariantChecks.checkNotNull(device);
    InvariantChecks.checkNotNull(internalAddress);
    InvariantChecks.checkNotNull(entry);

    entries.get(device).put(internalAddress, entry);
  }
}
