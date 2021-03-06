/*
 * Copyright 2015-2019 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.settings;

import ru.ispras.microtesk.test.engine.allocator.Allocator;

import java.util.Collection;
import java.util.Map;

/**
 * {@link StrategySettings} specifies an addressing mode allocation strategy.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class StrategySettings extends AbstractSettings {
  public static final String TAG = "strategy";

  private final Allocator allocator;
  private final Map<String, String> attributes;

  public StrategySettings(final Allocator allocator, final Map<String, String> attributes) {
    super(TAG);

    this.allocator = allocator;
    this.attributes = attributes;
  }

  public Allocator getAllocator() {
    return allocator;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<AbstractSettings> get(final String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final AbstractSettings section) {
    throw new UnsupportedOperationException();
  }
}
