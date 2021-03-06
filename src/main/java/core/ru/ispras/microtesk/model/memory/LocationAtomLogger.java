/*
 * Copyright 2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.memory;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.tracer.Record;
import ru.ispras.microtesk.model.tracer.Tracer;

/**
 * The {@link LocationAtomLogger} class logs writes to location atoms.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
final class LocationAtomLogger extends LocationAtom {
  private final LocationAtom atom;
  private final String name;
  private final int originalBitSize;
  private final int originalStartBitPos;

  protected LocationAtomLogger(final LocationAtom atom, final String name) {
    super(atom);

    InvariantChecks.checkNotNull(atom);
    InvariantChecks.checkNotNull(name);

    this.atom = (atom instanceof LocationAtomLogger) ? ((LocationAtomLogger) atom).atom : atom;
    this.name = name;
    this.originalBitSize = atom.getBitFieldSize();
    this.originalStartBitPos = atom.getBitFieldStart();
  }

  private LocationAtomLogger(
      final LocationAtom atom,
      final String name,
      final int originalBitSize,
      final int originalStartBitPos) {
    super(atom);

    InvariantChecks.checkNotNull(atom);
    InvariantChecks.checkNotNull(name);

    this.atom = (atom instanceof LocationAtomLogger) ? ((LocationAtomLogger) atom).atom : atom;
    this.name = name;
    this.originalBitSize = originalBitSize;
    this.originalStartBitPos = originalStartBitPos;
  }

  private String getName() {
    if (originalBitSize == getBitFieldSize() && originalStartBitPos == getBitFieldStart()) {
      return name;
    }

    final int start = getBitFieldStart() - originalStartBitPos;
    final int end = start + getBitFieldSize() - 1;

    return String.format("%s<%d..%d>", name, end, start);
  }

  @Override
  public boolean isInitialized() {
    return atom.isInitialized();
  }

  @Override
  public int getStorageBitSize() {
    return atom.getStorageBitSize();
  }

  @Override
  public LocationAtom resize(final int newBitSize, final int newStartBitPos) {
    // Field extending is not currently supported.
    InvariantChecks.checkTrue(newBitSize <= getBitFieldSize());
    InvariantChecks.checkTrue(newStartBitPos >= getBitFieldStart());

    final LocationAtom resized = atom.resize(newBitSize, newStartBitPos);
    return new LocationAtomLogger(
        resized,
        name,
        originalBitSize,
        originalStartBitPos
        );
  }

  @Override
  public BitVector load(final boolean useHandler) {
    return atom.load(useHandler);
  }

  @Override
  public void store(final BitVector data, final boolean callHandler) {
    atom.store(data, callHandler);

    if (Tracer.isEnabled()) {
      Tracer.addRecord(Record.newRegisterWrite(getName(), data));
    }
  }
}
