/*
 * Copyright 2012-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api.metadata;

import static ru.ispras.fortress.util.InvariantChecks.checkFalse;
import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;
import static ru.ispras.fortress.util.InvariantChecks.checkTrue;

import java.util.Map;

/**
 * The MetaOperation class stores information on the given operation.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */

public final class MetaOperation implements MetaData {
  private final String name;
  private final String typeName;
  private final boolean isRoot;
  private final Map<String, MetaArgument> args;
  private final Map<String, MetaShortcut> shortcuts;
  private final boolean hasRootShortcuts;
  private final boolean branch;
  private final boolean conditionalBranch;
  private final boolean exception;

  public MetaOperation(
      final String name,
      final String typeName,
      final boolean isRoot,
      final Map<String, MetaArgument> args,
      final Map<String, MetaShortcut> shortcuts,
      final boolean branch,
      final boolean conditionalBranch,
      final boolean exception) {
    checkNotNull(name);
    checkNotNull(typeName);
    checkNotNull(args);
    checkNotNull(shortcuts);

    if (!branch) {
      checkFalse(conditionalBranch);
    }

    if (conditionalBranch) {
      checkTrue(branch);
    }

    this.name = name;
    this.typeName = typeName;
    this.isRoot = isRoot;
    this.args = args;
    this.shortcuts = shortcuts;

    boolean rootShortcuts = false;
    for (final MetaShortcut ms : shortcuts.values()) {
      if (ms.getOperation().isRoot()) {
        rootShortcuts = true;
        break;
      }
    }

    this.hasRootShortcuts = rootShortcuts;
    this.branch = branch;
    this.conditionalBranch = conditionalBranch;
    this.exception = exception;
  }

  /**
   * Returns the operation name.
   * 
   * @return The operation name.
   */

  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the type name of the operation. If a meta operation describes a shortcut the type name
   * is different from the name. The name means the name of the target operation, while the type
   * name means the name of the entry operation (that encapsulates the path to the target).
   * 
   * @return The operation type name (for composite operation the name of the topmost operation).
   */

  public String getTypeName() {
    return typeName;
  }

  /**
   * Checks whether the current operation is a root. An operation is a root if it does not have
   * parents.
   * 
   * @return {@code true} if it is a root operation or {@code false} otherwise.
   */

  public boolean isRoot() {
    return isRoot;
  }

  /**
   * Returns a collection of operation arguments.
   * 
   * @return Collection of operation arguments.
   */

  public Iterable<MetaArgument> getArguments() {
    return args.values();
  }

  /**
   * Return an argument of the given operation that has the specified name.
   * 
   * @param name Argument name.
   * @return Argument with the specified name or {@code null} if no such argument is defined.
   */

  public MetaArgument getArgument(final String name) {
    return args.get(name);
  }

  /**
   * Returns a collection of shortcuts applicable to the given operation in different contexts.
   * 
   * @return A collection of shortcuts.
   */

  public Iterable<MetaShortcut> getShortcuts() {
    return shortcuts.values();
  }

  /**
   * Returns a shortcut for the given operation that can be used in the specified context.
   * 
   * @param contextName Context name.
   * @return Shortcut for the given context or {@code null} if no such shortcut exists.
   */

  public MetaShortcut getShortcut(final String contextName) {
    return shortcuts.get(contextName);
  }

  /**
   * Checks whether the current operation has root shortcuts. This means that the operation can be
   * addressed directly in a test template to specify a complete instruction call (not as a part of
   * a call specified as an argument for other operation).
   * 
   * @return {@code true} if it the operation has root shortcuts or {@code false} otherwise.
   */

  public boolean hasRootShortcuts() {
    return hasRootShortcuts;
  }

  /**
   * Checks whether the operation is a branch operation (causes control transfer).
   * 
   * @return {@code true} if the operation is a branch operation
   * or {@code false} otherwise.
   */

  public boolean isBranch() {
    return branch;
  }

  /**
   * Checks whether the operation is a conditional branch operation
   * (causes control transfer on some condition).
   * 
   * @return {@code true} if the operation is a conditional branch operation
   * or {@code false} otherwise.
   */

  public boolean isConditionalBranch() {
    return conditionalBranch;
  }

  /**
   * Checks whether the current operation can throw an exception.
   *  
   * @return {@code true} if the operation can throw an exception
   * or {@code false} otherwise.
   */

  public boolean canThrowException() {
    return exception;
  }
}
