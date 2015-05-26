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

package ru.ispras.microtesk.test.mmu.filter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.ispras.microtesk.translator.mmu.coverage.ExecutionPath;
import ru.ispras.microtesk.translator.mmu.coverage.Hazard;
import ru.ispras.microtesk.translator.mmu.coverage.UnitedDependency;
import ru.ispras.microtesk.translator.mmu.coverage.UnitedHazard;
import ru.ispras.microtesk.translator.mmu.spec.MmuAddress;
import ru.ispras.microtesk.utils.function.BiPredicate;

/**
 * Filters off test templates, where {@code ADDR_EQUAL} is set for the virtual address and
 * {@code ADDR_NOT_EQUAL} is set for the physical address.
 * 
 * <p>NOTE: Such test templates are unsatisfiable.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class FilterVaEqualPaNotEqual implements BiPredicate<ExecutionPath, UnitedDependency> {
  @Override
  public boolean test(final ExecutionPath execution, UnitedDependency dependency) {
    final MmuAddress va = execution.getStartAddress();
    final UnitedHazard vaHazard = dependency.getHazard(va);

    final Set<Integer> vaEqualRelation =
        vaHazard != null ? vaHazard.getRelation(Hazard.Type.ADDR_EQUAL) : null;

    if (vaEqualRelation == null) {
      return true;
    }

    for (final Map.Entry<MmuAddress, UnitedHazard> addrEntry : dependency.getAddrHazards().entrySet()) {
      final MmuAddress pa = addrEntry.getKey();
      final UnitedHazard paHazard = addrEntry.getValue();

      if (pa != va) {
        final Set<Integer> paNotEqualRelation = paHazard.getRelation(Hazard.Type.ADDR_NOT_EQUAL);

        // VA.ADDR_EQUAL => PA.ADDR_EQUAL.
        if (paNotEqualRelation != null) {
          final Set<Integer> paNotEqualRelationCopy = new HashSet<>(paNotEqualRelation);
          paNotEqualRelationCopy.retainAll(vaEqualRelation);

          if (!paNotEqualRelationCopy.isEmpty()) {
            // Filter off.
            return false;
          }
        }
      }
    }

    return true;
  }
}
