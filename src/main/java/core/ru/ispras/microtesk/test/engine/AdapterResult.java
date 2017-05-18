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

package ru.ispras.microtesk.test.engine;

import java.util.Collections;
import java.util.List;

import ru.ispras.fortress.util.Result;
import ru.ispras.microtesk.test.ConcreteSequence;

/**
 * {@link AdapterResult} defines result of an {@link Adapter}.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class AdapterResult extends Result<AdapterResult.Status, ConcreteSequence> {
  public static enum Status {
    OK,
    ERROR
  }

  public AdapterResult(
      final AdapterResult.Status status,
      final ConcreteSequence result,
      final List<String> errors) {
    super(status, result, errors);
  }

  public AdapterResult(final ConcreteSequence result) {
    super(Status.OK, result, Collections.<String>emptyList());
  }

  public AdapterResult(final String error) {
    super(Status.ERROR, null, Collections.singletonList(error));
  }
}
