/*
 * Copyright 2012-2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api.exception;

public class ConfigurationException extends MicroTESKException {
  private static final long serialVersionUID = -7710697576919321538L;

  public ConfigurationException() {}

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String format, Object... args) {
    super(String.format(format, args));
  }

  public ConfigurationException(Throwable cause) {
    super(cause);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
