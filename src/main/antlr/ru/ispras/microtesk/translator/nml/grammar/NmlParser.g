/*
 * Copyright 2012-2016 ISP RAS (http://www.ispras.ru)
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

/*===============================================================================================*/
/* README SECTION                                                                                */
/*                                                                                               */
/* TODO:                                                                                         */
/* - Brief description of the parser rules' structure and format                                 */
/* - Instructions on how to debug and extend the rules                                           */
/* - "TODO" notes                                                                                */
/*===============================================================================================*/

parser grammar NmlParser;

/*===============================================================================================*/
/* Options                                                                                       */
/*===============================================================================================*/

options {
  language=Java;
  tokenVocab=NmlLexer;
  output=AST;
  superClass=ParserBase;
  backtrack=true;
}

import commonParser=CommonParser;

/*===============================================================================================*/
/* Additional tokens. Lists additional tokens to be inserted in the AST by the parser            */
/* to express some syntactic properties.                                                         */
/*===============================================================================================*/

tokens {
//RANGE; // root node for range type definitions ([a..b]). Not supported in this version.

  SIZE_TYPE; // node for constructions that specify type and size of memory resources (reg, mem, var)

  LABEL; // imaginary token for let expression that specify an alias for some specified location

  ARGS;      // node for the list of args specified in AND-rules for mode and ops
  ARG_MODE;  // node to distinguish a mode argument 
  ARG_OP;    // node to distinguish an op argument 

  ALTERNATIVES; // node for the list of alternatives specified in OR-rules for modes and ops

  ATTRS;    // node for the list of attributes MODE and OP structures
  RETURN;   // node for the "return" attribute of MODE structure
}

/*===============================================================================================*/
/* Default Exception Handler Code                                                                */
/*===============================================================================================*/

@rulecatch {
catch (final RecognitionException re) { // Default behavior
  reportError(re);
  recover(input,re);
  // We don't insert error nodes in the IR (walker tree). 
  //retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
}
}

/*===============================================================================================*/
/* Header for the generated parser Java class file (header comments, imports, etc).              */
/*===============================================================================================*/

@header {
/*
 * Copyright 2012-2016 ISP RAS (http://www.ispras.ru)
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
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT.
 */

package ru.ispras.microtesk.translator.nml.grammar;

import ru.ispras.microtesk.translator.antlrex.ParserBase;
import ru.ispras.microtesk.translator.nml.NmlSymbolKind;
}

@members {
boolean isInBitField() {
  return commonParser.isInBitField();
}

void setInBitField(boolean value) {
  commonParser.setInBitField(value);
}
}

/*===============================================================================================*/
/* Root rules of processor specifications                                                        */
/*===============================================================================================*/

// Start rule
startRule
    :  procSpec* EOF
    ;

procSpec
    :  letDef
    |  typeDef
    |  structDef
    |  memDef
    |  modeDef
    |  opDef
    ;
catch [final RecognitionException re] {
  reportError(re);
  recover(input,re);
}

/*===============================================================================================*/
/* Let-rules (statically calculated constants and aliases for memory locations)                  */
/*===============================================================================================*/

letDef
    :  LET^ id=ID ASSIGN! le=letExpr { declare($id, $le.res, false); }
    ;

letExpr returns [NmlSymbolKind res]
    :  expr    { $res = NmlSymbolKind.LET_CONST;  } // Statically calculated constant expression. E.g. let A = 2 ** 4
    |  STRING_CONST { $res = NmlSymbolKind.LET_STRING; } // Some string constant. E.g. let A = "some text"
//  |  IF^ constNumExpr THEN! letExpr (ELSE! letExpr)? ENDIF! // TODO: NOT SUPPORTED IN THE CURRENT VERSION
//  |  SWITCH Construction // TODO: NOT SUPPORTED IN THE CURRENT VERSION
    ;

/*===============================================================================================*/
/* Type rules                                                                                    */
/*===============================================================================================*/

typeDef
    :  TYPE^ id=ID ASSIGN! typeExpr { declare($id, NmlSymbolKind.TYPE, false); }
    ;

// TODO: NOT SUPPORTED IN THE CURRENT VERSION 
// identifierList
//  :  ID^ (ASSIGN! CARD_CONST)? (COMMA! identifierList)?
//  ;

/*===============================================================================================*/
/* Struct rules                                                                                  */
/*===============================================================================================*/

structDef
    :  STRUCT^ id=ID { declare($id, NmlSymbolKind.TYPE, false); }
       LEFT_PARENTH! structFields RIGHT_PARENTH!
    ;

structFields
    :  structField (COMMA! structField)*
    ;

structField
    :  ID COLON! typeExpr
    ;

/*===============================================================================================*/
/* Memory storage (memory, registers, variables)                                                 */
/*===============================================================================================*/

memDef
    :  SHARED? (MEM|REG|VAR)^ id=ID LEFT_HOOK! st=sizeType {checkNotNull($st.start, $st.tree);}
                                    RIGHT_HOOK! {declare($id, NmlSymbolKind.MEMORY, false);} alias?
    ;

sizeType
    :  (expr COMMA)? te=typeExpr {checkNotNull($te.start, $te.tree);}
        -> ^(SIZE_TYPE expr? typeExpr)
    ;

alias
    :  ALIAS^ ASSIGN! aliasExpr
    ;

aliasExpr
    :  ID LEFT_HOOK expr DOUBLE_DOT expr RIGHT_HOOK 
         -> ^(DOUBLE_DOT ID expr expr)
    |  location
    ;

/*===============================================================================================*/
/* Mode rules                                                                                    */
/*===============================================================================================*/

modeDef
    :  MODE^ id=ID {declareAndPushSymbolScope($id, NmlSymbolKind.MODE);}
       modeSpecPart
    ;  finally {popSymbolScope();}

modeSpecPart
    :  andRule modeReturn? attrDefList
    |  orRule
    ;

modeReturn
    :  ASSIGN expr -> ^(RETURN expr)
    ;

/*===============================================================================================*/
/* Op rules                                                                                      */
/*===============================================================================================*/

opDef
    :  PSEUDO? OP^ id=(ID | EXCEPTION) {declareAndPushSymbolScope($id, NmlSymbolKind.OP);}
       opSpecPart
    ;  finally {popSymbolScope();}

opSpecPart
    :  andRule attrDefList
    |  orRule
    ;

/*===============================================================================================*/
/* Or rules (for modes and ops)                                                                  */
/*===============================================================================================*/

orRule
    :  ASSIGN ID (VERT_BAR ID)* -> ^(ALTERNATIVES ID+)
    ;

/*===============================================================================================*/
/* And rules (for modes and ops)                                                                 */
/*===============================================================================================*/

andRule
    :  LEFT_PARENTH (argDef (COMMA argDef)*)? RIGHT_PARENTH -> ^(ARGS argDef*)
    ;

argDef
    :  id=ID^ COLON! argType {declare($id, NmlSymbolKind.ARGUMENT, false);}
    ;

argType
    :  {isDeclaredAs(input.LT(1), NmlSymbolKind.MODE)}? ID -> ^(ARG_MODE ID)
    |  {isDeclaredAs(input.LT(1), NmlSymbolKind.OP)}? ID -> ^(ARG_OP ID) 
    |  typeExpr
    ;

/*===============================================================================================*/
/* Attribute rules (for modes and ops)                                                           */
/*===============================================================================================*/

attrDefList
    :  attrDef* -> ^(ATTRS attrDef*)
    ;

attrDef
    @after {declare($id, NmlSymbolKind.ATTRIBUTE, false);}
    :  id=SYNTAX^ ASSIGN! syntaxDef
    |  id=IMAGE^ ASSIGN! imageDef
    |  id=ACTION^ ASSIGN! actionDef
    |  id=ID^ ASSIGN! actionDef
//  |  USES ASSIGN usesDef     // NOT SUPPORTED IN THE CURRENT VERSION
    ;

syntaxDef
    :  ID DOT^ SYNTAX
    |  attrExpr
    ;

imageDef
    :  ID DOT^ IMAGE
    |  attrExpr
    ;

actionDef
    :  ID DOT^ ACTION
    |  LEFT_BRACE! sequence RIGHT_BRACE!
    ;
