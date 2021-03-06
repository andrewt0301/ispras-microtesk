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

package ru.ispras.microtesk.test.template;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.memory.Section;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The {@link ExceptionHandler} class holds template descriptions of
 * handers of certain exception types to be executed on certain processing
 * element instance.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class ExceptionHandler {

  public static final class EntryPoint {
    private final BigInteger origin;
    private final Set<String> exceptions;
    private final List<AbstractCall> calls;

    protected EntryPoint(
        final BigInteger origin,
        final Set<String> exceptions,
        final List<AbstractCall> calls) {
      InvariantChecks.checkNotNull(exceptions);
      InvariantChecks.checkNotNull(origin);
      InvariantChecks.checkGreaterOrEq(origin, BigInteger.ZERO);
      InvariantChecks.checkNotNull(calls);

      this.origin = origin;
      this.exceptions = Collections.unmodifiableSet(exceptions);
      this.calls = Collections.unmodifiableList(calls);
    }

    public BigInteger getOrigin() {
      return origin;
    }

    public Set<String> getExceptions() {
      return exceptions;
    }

    public List<AbstractCall> getCalls() {
      return calls;
    }
  }

  private final String id;
  private final Section section;
  private final Set<Integer> instances;
  private final List<EntryPoint> entryPoints;

  protected ExceptionHandler(
      final String id,
      final Section section,
      final Set<Integer> instances,
      final List<EntryPoint> entryPoints) {
    InvariantChecks.checkNotNull(id);
    InvariantChecks.checkNotNull(section);
    InvariantChecks.checkNotNull(instances);
    InvariantChecks.checkNotNull(entryPoints);

    this.id = id;
    this.section = section;
    this.instances = Collections.unmodifiableSet(instances);
    this.entryPoints = Collections.unmodifiableList(entryPoints);
  }

  public String getId() {
    return id;
  }

  public Section getSection() {
    return section;
  }

  public Set<Integer> getInstances() {
    return instances;
  }

  public List<EntryPoint> getEntryPoints() {
    return entryPoints;
  }
}
