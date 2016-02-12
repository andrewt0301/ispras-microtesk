/*
 * Copyright 2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.sequence;

import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.test.sequence.permutator.Permutator;
import ru.ispras.testbase.knowledge.iterator.Iterator;

public final class GeneratorModificator<T> implements Generator<T> {
  private final Generator<T> generator;
  private final Permutator<T> modificator;

  private boolean hasValue;

  public GeneratorModificator(final Generator<T> generator, final Permutator<T> modificator) {
    InvariantChecks.checkNotNull(generator);
    InvariantChecks.checkNotNull(modificator);

    this.generator = generator;
    this.modificator = modificator;
  }

  private void initGenerator() {
    generator.init();
  }

  private void initModificator() {
    modificator.initialize(generator.value());
    modificator.init();
  }

  @Override
  public void init() {
    initGenerator();

    if (generator.hasValue()) {
      initModificator();
    }

    hasValue = generator.hasValue() && modificator.hasValue();
  }

  @Override
  public boolean hasValue() {
    return hasValue;
  }

  @Override
  public List<T> value() {
    return modificator.value();
  }

  @Override
  public void next() {
    modificator.next();
    if (modificator.hasValue()) {
      return;
    }

    generator.next();
    if (generator.hasValue()) {
      initModificator();
      return;
    }

    hasValue = false;
  }

  @Override
  public void stop() {
    hasValue = false;
  }

  @Override
  public Iterator<List<T>> clone() {
    throw new UnsupportedOperationException();
  }
}
