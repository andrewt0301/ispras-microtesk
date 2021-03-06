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

package ru.ispras.microtesk.translator.nml.ir;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.util.TreeVisitor;
import ru.ispras.microtesk.translator.nml.ir.primitive.Attribute;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.nml.ir.primitive.PrimitiveAnd;
import ru.ispras.microtesk.translator.nml.ir.primitive.PrimitiveOr;
import ru.ispras.microtesk.translator.nml.ir.primitive.Shortcut;
import ru.ispras.microtesk.translator.nml.ir.primitive.Statement;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAssignment;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAttributeCall;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementCondition;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementFormat;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementFunctionCall;
import ru.ispras.microtesk.translator.nml.ir.shared.LetConstant;
import ru.ispras.microtesk.translator.nml.ir.shared.LetLabel;
import ru.ispras.microtesk.translator.nml.ir.shared.MemoryResource;
import ru.ispras.microtesk.translator.nml.ir.shared.Type;

/**
* The {@link IrVisitor} interface is to be implemented by all visitor objects
* applied to the IR of an ISA specification.
*
* @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
*/
public interface IrVisitor extends TreeVisitor {
  void setStatus(final Status status);

  /**
   * Notifies that traversing the resource section has been started.
   * The section includes constants, types and memory storages.
   */
  void onResourcesBegin();

  /**
   * Notifies that traversing the resource section has been finished.
   */
  void onResourcesEnd();

  /**
   * Notifies that a let construct describing a constant value has been visited.
   *
   * @param let Object describing the constant.
   */
  void onLetConstant(LetConstant let);

  /**
   * Notifies that a let construct associates a symbolic name with a memory location
   * (in other words, establishes a label).
   *
   * @param let Object describing the label associated with a memory location.
   */
  void onLetLabel(LetLabel let);

  /**
   * Notifies that a type has been visited.
   *
   * @param name Type name.
   * @param type Type description.
   */
  void onType(String name, Type type);

  /**
   * Notifies that a memory storage has been visited.
   *
   * @param name Memory storage name.
   * @param memory Memory storage description.
   */
  void onMemory(String name, MemoryResource memory);

  /**
   * Notifies that visiting primitives (objects describing MODEs and OPs) has been started.
   */
  void onPrimitivesBegin();

  /**
   * Notifies that visiting primitives (objects describing MODEs and OPs) has been finished.
   */
  void onPrimitivesEnd();

  /**
   * Notifies that visiting a primitive (object describing a MODE or OP) has been started.
   *
   * @param item Primitive object describing a MODE or OP.
   */
  void onPrimitiveBegin(Primitive item);

  /**
   * Notifies that visiting a primitive has been finished.
   *
   * @param item Primitive object.
   */
  void onPrimitiveEnd(Primitive item);

  /**
   * Notifies that visiting an item of an OR-rule has been started.
   *
   * @param orRule OR-rule description.
   * @param item Item being visited.
   */
  void onAlternativeBegin(PrimitiveOr orRule, Primitive item);

  /**
   * Notifies that visiting an item of an OR-rule has been finished.
   *
   * @param orRule OR-rule description.
   * @param item Item being visited.
   */
  void onAlternativeEnd(PrimitiveOr orRule, Primitive item);

  /**
   * Notifies that visiting an AND-rule argument has been started.
   *
   * @param andRule AND-rule description.
   * @param argName Argument name.
   * @param argType Argument type description.
   */
  void onArgumentBegin(PrimitiveAnd andRule, String argName, Primitive argType);

  /**
   * Notifies that visiting an AND-rule argument has been finished.
   *
   * @param andRule AND-rule description.
   * @param argName Argument name.
   * @param argType Argument type description.
   */
  void onArgumentEnd(PrimitiveAnd andRule, String argName, Primitive argType);

  /**
   * Notifies that visiting an attribute of an AND-rule has been started.
   *
   * @param andRule AND-rule description.
   * @param attr Attribute description.
   */
  void onAttributeBegin(PrimitiveAnd andRule, Attribute attr);

  /**
   * Notifies that visiting an attribute of an AND-rule has been finished.
   *
   * @param andRule AND-rule description.
   * @param attr Attribute description.
   */
  void onAttributeEnd(PrimitiveAnd andRule, Attribute attr);

  /**
   * Notifies that visiting a statement (in an attribute) has been started.
   *
   * @param andRule AND-rule that contains the statement.
   * @param attr Attribute that contains the statement.
   * @param stmt Statement description.
   */
  void onStatementBegin(PrimitiveAnd andRule, Attribute attr, Statement stmt);

  /**
   * Notifies that visiting a statement (in an attribute) has been finished.
   *
   * @param andRule AND-rule that contains the statement.
   * @param attr Attribute that contains the statement.
   * @param stmt Statement description.
   */
  void onStatementEnd(PrimitiveAnd andRule, Attribute attr, Statement stmt);

  /**
   * Notifies that visiting a shortcut has been started.
   *
   * @param andRule AND-rule the shortcut refers to.
   * @param shortcut Shortcut description.
   */
  void onShortcutBegin(PrimitiveAnd andRule, Shortcut shortcut);

  /**
   * Notifies that visiting a shortcut has been finished.
   *
   * @param andRule AND-rule the shortcut refers to.
   * @param shortcut Shortcut description.
   */
  void onShortcutEnd(PrimitiveAnd andRule, Shortcut shortcut);

  void onAssignment(StatementAssignment stmt);

  void onFormat(StatementFormat stmt);

  void onFunctionCall(StatementFunctionCall stmt);

  void onAttributeCallBegin(StatementAttributeCall stmt);

  void onAttributeCallEnd(StatementAttributeCall stmt);

  void onConditionBegin(StatementCondition stmt);

  void onConditionEnd(StatementCondition stmt);

  void onConditionBlockBegin(Node condition);

  void onConditionBlockEnd(Node condition);
}
