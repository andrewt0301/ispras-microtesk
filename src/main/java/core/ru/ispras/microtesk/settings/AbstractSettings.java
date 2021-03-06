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

package ru.ispras.microtesk.settings;

import ru.ispras.fortress.util.InvariantChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link AbstractSettings} represents abstract settings.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public abstract class AbstractSettings {
  private final String tag;

  /**
   * Contains the settings' sections (standard and generator-specific ones).
   *
   * <p>The key is the section tag; the value is the list of the settings' sections.</p>
   */
  private Map<String, Collection<AbstractSettings>> sections = new HashMap<>();

  public AbstractSettings(final String tag) {
    this.tag = tag;
  }

  public final String getTag() {
    return tag;
  }

  /**
   * Returns the name of the settings (to be overridden in subclasses).
   *
   * @return the settings name.
   */
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public final <T extends AbstractSettings> T getSingle(final String tag) {
    InvariantChecks.checkNotNull(tag);

    final Collection<AbstractSettings> sections = get(tag);
    if (null == sections) {
      return null;
    }

    for (final AbstractSettings section : sections) {
      return (T) section;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public final <T extends AbstractSettings> T getSingle(final String tag, final String name) {
    InvariantChecks.checkNotNull(tag);
    InvariantChecks.checkNotNull(name);

    final Collection<AbstractSettings> sections = get(tag);
    if (null == sections) {
      return null;
    }

    for (final AbstractSettings section : sections) {
      if (name.equals(section.getName())) {
        return (T) section;
      }
    }

    return null;
  }

  /**
   * Default implementation (to be overridden in subclasses).
   *
   * @param tag the tag of the sections to be returned.
   * @return Sections that correspond to the specified section tag.
   */
  public Collection<AbstractSettings> get(final String tag) {
    InvariantChecks.checkNotNull(tag);
    return sections.get(tag);
  }

  /**
   * Default implementation (to be overridden in subclasses).
   *
   * @param section the settings's section to be added.
   */
  public void add(final AbstractSettings section) {
    InvariantChecks.checkNotNull(section);

    Collection<AbstractSettings> tagSections = sections.get(section.getTag());
    if (tagSections == null) {
      sections.put(section.getTag(), tagSections = new ArrayList<>());
    }

    tagSections.add(section);
  }

  @Override
  public String toString() {
    return String.format("%s", sections.values());
  }
}
