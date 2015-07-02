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

package ru.ispras.microtesk.mmu.test.sequence.engine.filter;

import java.util.Collection;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.test.sequence.engine.iterator.MemoryAccessStructure;
import ru.ispras.microtesk.mmu.translator.coverage.MemoryAccess;
import ru.ispras.microtesk.mmu.translator.coverage.MemoryDependency;
import ru.ispras.microtesk.mmu.translator.coverage.MemoryUnitedDependency;
import ru.ispras.microtesk.utils.function.BiPredicate;
import ru.ispras.microtesk.utils.function.Predicate;
import ru.ispras.microtesk.utils.function.TriPredicate;

/**
 * {@link FilterTemplate} composes execution- and dependency-level filters into a template-level
 * filter.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class FilterTemplate implements Predicate<MemoryAccessStructure> {
  private final Collection<Predicate<MemoryAccess>> executionFilters;
  private final Collection<TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency>> dependencyFilters;
  private final Collection<BiPredicate<MemoryAccess, MemoryUnitedDependency>> unitedDependencyFilters;

  /**
   * Constructs a template-level filter from execution- and dependency-level filters.
   * 
   * @param executionFilters the collection of execution-level filters.
   * @param dependencyFilters the collection of dependency-level filters.
   * @param unitedDependencyFilters the collection of united-dependency-level filters.
   * @throws IllegalArgumentException if some parameters are null.
   */
  public FilterTemplate(
      final Collection<Predicate<MemoryAccess>> executionFilters,
      final Collection<TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency>> dependencyFilters,
      final Collection<BiPredicate<MemoryAccess, MemoryUnitedDependency>> unitedDependencyFilters) {
    InvariantChecks.checkNotNull(executionFilters);
    InvariantChecks.checkNotNull(dependencyFilters);
    InvariantChecks.checkNotNull(unitedDependencyFilters);

    this.executionFilters = executionFilters;
    this.dependencyFilters = dependencyFilters;
    this.unitedDependencyFilters = unitedDependencyFilters;
  }

  @Override
  public boolean test(final MemoryAccessStructure template) {
    for (int i = 0; i < template.size(); i++) {
      final MemoryAccess execution1 = template.getAccess(i);

      // Apply the execution-level filters.
      for (final Predicate<MemoryAccess> filter : executionFilters) {
        if (!filter.test(execution1)) {
          // Filter off.
          return false;
        }
      }

      for (int j = i + 1; j < template.size(); j++) {
        final MemoryAccess execution2 = template.getAccess(j);
        final MemoryDependency dependency = template.getDependency(i, j);

        if (dependency == null) {
          continue;
        }

        // Apply the dependency-level filters.
        for (final TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency> filter : dependencyFilters) {
          if (!filter.test(execution1, execution2, dependency)) {
            // Filter off.
            return false;
          }
        }
      }

      final MemoryUnitedDependency unitedDependency = template.getUnitedDependency(i);

      // Apply the united-dependency-level filters.
      for (final BiPredicate<MemoryAccess, MemoryUnitedDependency> filter : unitedDependencyFilters) {
        if (!filter.test(execution1, unitedDependency)) {
          // Filter off.
          return false;
        }
      }
    }

    return true;
  }
}
