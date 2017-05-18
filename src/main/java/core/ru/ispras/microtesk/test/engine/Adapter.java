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

package ru.ispras.microtesk.test.engine;

import java.util.Map;

import ru.ispras.microtesk.test.template.AbstractSequence;

/**
 * {@link Adapter} defines an interface of adapters of abstract call sequence solution provided
 * by corresponding {@link Engine}.
 * 
 * @author <a href="mailto:kotsynyak@ispras.ru">Artem Kotsynyak</a>
 */
public interface Adapter {
  void configure(Map<String, Object> attributes);

  AdapterResult adapt(EngineContext engineContext, AbstractSequence abstractSequence);

  void onStartProgram();

  void onEndProgram();
}
