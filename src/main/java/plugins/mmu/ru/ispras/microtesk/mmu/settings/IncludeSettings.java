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

package ru.ispras.microtesk.mmu.settings;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.settings.AbstractSettings;

import java.math.BigInteger;

/**
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class IncludeSettings extends AbstractSettings {
  public static final String TAG = String.format("%s-include", MmuSettings.TAG_PREFIX);

  private final BigInteger value;

  public IncludeSettings(final BigInteger value) {
    super(TAG);

    InvariantChecks.checkNotNull(value);
    this.value = value;
  }

  public BigInteger getValue() {
    return value;
  }
}
