/*======================================================================================*/
/* README SECTION                                                                       */
/*                                                                                      */
/* TODO:                                                                                */
/* - Brief description of the tree rules' structure and format                          */
/* - Instructions on how to debug and extend the rules                                  */
/* - "TODO" notes                                                                       */     
/*======================================================================================*/

tree grammar SimnMLTreeWalker;

/*======================================================================================*/
/* Options                                                                              */
/*======================================================================================*/

options {
  language=Java;
  tokenVocab=SimnMLParser;
  ASTLabelType=CommonTree;
  superClass=TreeWalkerBase;
}

@rulecatch {
catch (SemanticException se) {
    reportError(se);
    recover(input,se);
}
catch (RecognitionException re) { // Default behavior
    reportError(re);
    recover(input,re);
}
}

/*======================================================================================*/
/* Header for the generated tree walker Java class file (header comments, imports, etc).*/
/*======================================================================================*/

@header {
/*
 * Copyright (c) 2012 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * SimnMLTreeWalker.java Andrei Tatarnikov
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT. 
 */

package ru.ispras.microtesk.translator.simnml.grammar;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import ru.ispras.microtesk.translator.antlrex.Where;
import ru.ispras.microtesk.translator.antlrex.SemanticException;

import ru.ispras.microtesk.translator.simnml.antlrex.TreeWalkerBase;
import ru.ispras.microtesk.translator.simnml.ESymbolKind;
import ru.ispras.microtesk.model.api.memory.EMemoryKind;

import ru.ispras.microtesk.translator.simnml.ir.PCAnalyzer;
import ru.ispras.microtesk.translator.simnml.ir.expression.*;
import ru.ispras.microtesk.translator.simnml.ir.shared.*;
import ru.ispras.microtesk.translator.simnml.ir.primitive.*;
}

/*======================================================================================*/
/* Root Rules of Processor Specification                                                */ 
/*======================================================================================*/

// Start rule
startRule 
    :  procSpec*
    ;

procSpec
@init {
// System.out.println("Sim-nML:   " + $procSpec.text);
}
    :  letDef
    |  typeDef
    |  memDef
    |  regDef
    |  varDef
    |  modeDef
    |  opDef
    ;

/*======================================================================================*/
/* Let Rules                                                                            */
/*======================================================================================*/

letDef
    :  ^(LET id=ID le=letExpr[$id.text])
    ;

letExpr [String name]
    :  ce = constExpr
{
checkNotNull($ce.start, $ce.res, $ce.text);
final LetConstant constant = getLetFactory().createConstant(name, $ce.res);
getIR().add(name, constant);
}
    |  sc = STRING_CONST
{
final LetString string = getLetFactory().createString(name, $sc.text);
getIR().add(name, string);

final LetLabel label = getLetFactory().createLabel(name, $sc.text);
if (null != label)
    getIR().add(name, label);
}
//  |  IF^ constNumExpr THEN! letExpr (ELSE! letExpr)? ENDIF! // NOT SUPPORTED IN THIS VERSION
//  |  SWITCH Construction                                    // NOT SUPPORTED IN THIS VERSION
    ;

/*======================================================================================*/
/* Type Rules                                                                           */
/*======================================================================================*/

typeDef
    :  ^(TYPE id=ID te=typeExpr)
{
checkNotNull($id, $te.res, $te.text);
getIR().add($id.text, $te.res);
}
    ;

typeExpr returns [Type res]
    :   id=ID                      { $res=getTypeFactory().createAlias($id.text); }
//  |   BOOL                       // TODO: NOT SUPPORTED IN THIS VERSION
    |   ^(t=INT  n=sizeExpr) { $res=getTypeFactory().createIntegerType(where($t), $n.res); }
    |   ^(t=CARD n=sizeExpr) { $res=getTypeFactory().createCardType(where($t), $n.res); }
//  |   ^(t=FIX   n=static m=staticJavaExpr) // TODO: NOT SUPPORTED IN THIS VERSION
//  |   ^(t=FLOAT n=staticJavaExpr m=staticJavaExpr) // TODO: NOT SUPPORTED IN THIS VERSION
//  |   ^(t=RANGE n=staticJavaExpr m=staticJavaExpr) // TODO: NOT SUPPORTED IN THIS VERSION
    ;

/*======================================================================================*/
/* Location Rules (Memory, Registers, Variables)                                        */
/*======================================================================================*/

memDef
    :  ^(MEM id=ID st=sizeType alias?)
{
// TODO: implement IR for alises
final MemoryExprFactory factory = getMemoryExprFactory();
final MemoryExpr expr = (null != $st.size) ?
   factory.createMemory(EMemoryKind.MEM, $st.type, $st.size) :
   factory.createMemory(EMemoryKind.MEM, $st.type);

getIR().add($id.text, expr);
}
    ;

regDef
    :  ^(REG id=ID st=sizeType)
{
final MemoryExprFactory factory = getMemoryExprFactory();
final MemoryExpr expr = (null != $st.size) ?
   factory.createMemory(EMemoryKind.REG, $st.type, $st.size) :
   factory.createMemory(EMemoryKind.REG, $st.type);

getIR().add($id.text, expr);
}
    ;

varDef
    :  ^(VAR id=ID st=sizeType)
{
final MemoryExprFactory factory = getMemoryExprFactory();
final MemoryExpr expr = (null != $st.size) ?
   factory.createMemory(EMemoryKind.VAR, $st.type, $st.size) :
   factory.createMemory(EMemoryKind.VAR, $st.type);

getIR().add($id.text, expr);
}
    ;

sizeType returns [Type type, Expr size]
    :   ^(st=SIZE_TYPE s=sizeExpr t=typeExpr)
{ 
checkNotNull($st, $s.res, $s.text);
checkNotNull($st, $t.res, $t.text);
$type = $t.res;
$size = $s.res;
}
    |   ^(st=SIZE_TYPE t=typeExpr)
{
checkNotNull($st, $t.res, $t.text);
$type = $t.res;
$size = null;
}
    ;

alias
    :  ^(ALIAS locationAtom)
    ;

/*======================================================================================*/
/* Mode rules                                                                           */
/*======================================================================================*/

modeDef 
@init {reserveThis();}
    :  ^(MODE id=ID {pushSymbolScope(id);} sp=modeSpecPart[where($id), $id.text])
{
checkNotNull($id, $sp.res, $modeDef.text);
getIR().add($id.text, $sp.res);
}
    ;  finally
{
popSymbolScope();

resetThisArgs();
finalizeThis($sp.res);
}

modeSpecPart [Where w, String name] returns [Primitive res]
    :  andRes=andRule
{
checkNotNull(w, $andRes.res, $andRes.text);
setThisArgs($andRes.res);
}
       (mr=modeReturn {checkNotNull(w, $mr.res, $mr.text);})?
       attrRes=attrDefList
{
checkNotNull(w, $attrRes.res, $attrRes.text);
$res = getPrimitiveFactory().createMode($w, $name, $andRes.res, $attrRes.res, $mr.res);
}
    |  orRes=orRule
{
$res = getPrimitiveFactory().createModeOR($w, $name, $orRes.res);
}
    ;

modeReturn returns [Expr res]
    :  ^(RETURN me=dataExpr {checkNotNull($me.start, $me.res, $me.text);}) {$res = $me.res;}
    ;

/*======================================================================================*/
/* Op rules                                                                             */
/*======================================================================================*/

opDef
@init {reserveThis();}
    :  ^(OP id=ID {pushSymbolScope(id);} sp=opSpecPart[where($id), $id.text])
{
checkNotNull($id, $sp.res, $opDef.text);
getIR().add($id.text, $sp.res);
}
    ;  finally
{
popSymbolScope();

resetThisArgs();
finalizeThis($sp.res);
}

opSpecPart [Where w, String name] returns [Primitive res]
    :  andRes=andRule
{
checkNotNull(w, $andRes.res, $andRes.text);
setThisArgs($andRes.res);
}
       attrRes=attrDefList
{
checkNotNull(w, $attrRes.res, $attrRes.text);
$res = getPrimitiveFactory().createOp($w, $name, $andRes.res, $attrRes.res);
}
    |  orRes=orRule
{
$res = getPrimitiveFactory().createOpOR($w, $name, $orRes.res);
}
    ;

/*======================================================================================*/
/* Or rules (for modes and ops)                                                         */
/*======================================================================================*/

orRule returns [List<String> res]
@init  {$res = new ArrayList<String>();}
    :  ^(ALTERNATIVES (a=ID {$res.add($a.text);})+)
    ;

/*======================================================================================*/
/* And rules (for modes and ops)                                                        */
/*======================================================================================*/

andRule returns [Map<String,Primitive> res]
@init  {final Map<String,Primitive> args = new LinkedHashMap<String,Primitive>();}
@after {$res = args;}
    :  ^(ARGS (^(id=ID at=argType)
{
checkNotNull($id, $at.res, $at.text);
declare($id, ESymbolKind.ARGUMENT, false);
args.put($id.text, $at.res);
})*)
    ;

argType returns [Primitive res]
@init  {final PrimitiveFactory factory = getPrimitiveFactory();}
    :  ^(ARG_MODE id=ID) {$res = factory.getMode(where($id), $id.text);}
    |  ^(ARG_OP id=ID)   {$res = factory.getOp(where($id), $id.text);}
    |  te=typeExpr       {$res = factory.createImm($te.res);}
    ;

/*======================================================================================*/
/* Attribute rules (for modes and ops)                                                  */
/*======================================================================================*/

attrDefList returns [Map<String, Attribute> res]
@init  {final Map<String,Attribute> attrs = new LinkedHashMap<String,Attribute>();}
@after {$res = attrs;}
    :  ^(ATTRS (attr=attrDef
{
checkNotNull($ATTRS, $attr.res, $attr.text);
attrs.put($attr.res.getName(), $attr.res);
})*)
    ;

attrDef returns [Attribute res]
    :  ^(SYNTAX {checkMemberDeclared($SYNTAX, ESymbolKind.ATTRIBUTE);} attr=syntaxDef) {$res = $attr.res;}
    |  ^(IMAGE  {checkMemberDeclared($IMAGE,  ESymbolKind.ATTRIBUTE);} attr=imageDef)  {$res = $attr.res;}
    |  ^(ACTION {checkMemberDeclared($ACTION, ESymbolKind.ATTRIBUTE);} attr=actionDef[$ACTION.text]) {$res = $attr.res;}
    |  ^(id=ID  {checkMemberDeclared($ID,     ESymbolKind.ATTRIBUTE);} attr=actionDef[$id.text]) {$res = $attr.res;}
//  |  USES ASSIGN usesDef     // NOT SUPPORTED IN THE CURRENT VERSION
    ;

syntaxDef returns [Attribute res]
    :  ^(DOT id=ID name=SYNTAX)
{
final Statement stmt = getStatementFactory().createAttributeCall(where($id), $id.text, $name.text);
$res = getAttributeFactory().createExpression("syntax", stmt);
}
    |  ae=attrExpr
{
$res = getAttributeFactory().createExpression("syntax", $ae.res);
}
    ;

imageDef returns [Attribute res]
    :  ^(DOT id=ID name=IMAGE)
{
final Statement stmt = getStatementFactory().createAttributeCall(where($id), $id.text, $name.text);
$res = getAttributeFactory().createExpression("image", stmt);
}
    |  ae=attrExpr
{
$res = getAttributeFactory().createExpression("image", $ae.res);
}
    ;

actionDef [String actionName] returns [Attribute res]
@init  {final AttributeFactory factory = getAttributeFactory();}
    :  ^(DOT id=ID name=ACTION)
{
final Statement stmt = getStatementFactory().createAttributeCall(where($id), $id.text, $name.text);
$res = factory.createAction(actionName, Collections.singletonList(stmt));
}
    |  seq=sequence
{
checkNotNull($seq.start, $seq.res, $seq.text);
$res = factory.createAction(actionName, $seq.res);
}
    ;

/*======================================================================================*/
/* Expresion-like attribute rules(format expressions in the symtax and image attributes)*/
/*======================================================================================*/

attrExpr returns [Statement res]
    :  str=STRING_CONST
{
$res = getStatementFactory().createFormat(where($str), $str.text, null);
}
    |  ^(FORMAT fs=STRING_CONST (fargs=formatIdList)?)
{
$res = getStatementFactory().createFormat(where($fs), $fs.text, $fargs.res);
}
    ;

formatIdList returns [List<Format.Argument> res]
@init  {final List<Format.Argument> args = new ArrayList<Format.Argument>();}
@after {$res = args;}
    :  (fa=formatId {args.add($fa.res);})+
    ;

formatId returns [Format.Argument res]
    :  ^(DOT id=ID name=(SYNTAX | IMAGE))
{
$res = Format.createArgument((StatementAttributeCall)getStatementFactory().createAttributeCall(where($id), $id.text, $name.text));
}
    |  e=dataExpr
{
$res = Format.createArgument($e.res);
}
    ;

/*======================================================================================*/
/* Sequence statements (for action-like attributes)                                     */
/*======================================================================================*/

sequence returns [List<Statement> res]
@init  {final List<Statement> stmts = new ArrayList<Statement>();}
@after {$res = stmts;}
    :  ^(sq=SEQUENCE (st=statement
{
checkNotNull($sq, $st.res, $st.text);
stmts.addAll($st.res);
})*)
    ;

statement returns [List<Statement> res]
    :  acs=attributeCallStatement
{
checkNotNull($acs.start, $acs.res, $acs.text);
$res = $acs.res;
}
    |  as=assignmentStatement
{
checkNotNull($as.start, $as.res, $as.text);
$res = $as.res;
}
    |  cs=conditionalStatement
{
checkNotNull($cs.start, $cs.res, $cs.text);
$res = $cs.res;
}
//  |  functionCall
//  |  ERROR^ LEFT_PARENTH! STRING_CONST RIGHT_PARENTH!
    ;

attributeCallStatement returns [List<Statement> res]
    :  id=ID
{
$res = Collections.singletonList(
    getStatementFactory().createAttributeCall(where($id), $id.text));
}
    |  ^(DOT id=ID name=(ACTION | ID))
{
$res = Collections.singletonList(
    getStatementFactory().createAttributeCall(where($id), $id.text, $name.text));
}
    ;

assignmentStatement returns [List<Statement> res]
@init {final PCAnalyzer analyzer = new PCAnalyzer(getLocationFactory(), getIR());}
    :  ^(ASSIGN le=location
{
checkNotNull($le.start, $le.res, $le.text);
analyzer.startTrackingSource();
}
    me=dataExpr)
{
final List<Statement> result = new ArrayList<Statement>();
result.add(getStatementFactory().createAssignment($le.res, $me.res));

final int ctIndex = analyzer.getControlTransferIndex();
if (ctIndex > 0)
    result.add(getStatementFactory().createControlTransfer(ctIndex));

$res = result;
}
    ;
finally {analyzer.finalize();}

conditionalStatement returns [List<Statement> res]
    :  ifs = ifStmt { $res = $ifs.res; }
    ;

ifStmt returns [List<Statement> res]
@init  {final List<StatementCondition.Block> blocks = new ArrayList<StatementCondition.Block>();}
    :  ^(IF cond=logicExpr stmts=sequence {blocks.add(StatementCondition.Block.newIfBlock($cond.res, $stmts.res));}
        (elifb=elseIfStmt                 {blocks.add($elifb.res);})*
        (eb=elseStmt                      {blocks.add($eb.res);})?)
{
$res = Collections.singletonList(getStatementFactory().createCondition(blocks));
}
    ;

elseIfStmt returns [StatementCondition.Block res]
    :  ^(ELSEIF cond=logicExpr stmts=sequence)
{
$res = StatementCondition.Block.newIfBlock($cond.res, $stmts.res);
}
    ;

elseStmt returns [StatementCondition.Block res]
    :  ^(ELSE stmts=sequence)
{
$res = StatementCondition.Block.newElseBlock($stmts.res);
}
    ;

/*======================================================================================*/
/* Extended Expression Rules                                                            */
/*                                                                                      */
/* There are several use cases for expressions that impose certain restrictions on them:*/
/*                                                                                      */
/* 1. Constant expressions. These expressions are statically calculated at translation  */
/*    time and evaluated to constant Java values (currently, "int" or "long"). Constant */
/*    expressions are used in Let constructions.                                        */
/*                                                                                      */
/* 2. Size expressions. These expressions are constant expressions evaluated to         */
/*    constant integer values. Size has the Java "int" type. Size expressions are used  */
/*    to describe types (e.g. card(32)) and memory locations (reg, mem and var          */
/*    definitions).                                                                     */
/*                                                                                      */
/* 3. Index expressions. These expressions should be evaluated to Java integer values.  */
/*    Index is represented by then Java "int" type. Index expressions are used to       */
/*    access locations by their index in a memory line (e.g. GPR[index + 1]) and to     */
/*    address bitfields of locations (e.g. temp<x+1 .. x+5>).                           */
/*                                                                                      */
/* 4. Logic expressions. There expressions are evaluated to boolean values. Logic       */
/*    expressions are used in condition statements.                                     */
/*                                                                                      */
/* 5. Data expressions. Data expressions are described in terms of locations. All       */
/*    manipulations with locations are described by data expressions. These expressions */
/*    are used in assignment statements, etc.                                           */
/*======================================================================================*/

constExpr returns [Expr res]
    :  e=expr[ValueInfo.Kind.NATIVE, 0]
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().evaluateConst(where($e.start), $e.res);
}
    ;

sizeExpr returns [Expr res]
    :  e=expr[ValueInfo.Kind.NATIVE, 0]
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().evaluateSize(where($e.start), $e.res);
}
    ;

indexExpr returns [Expr res]
    :  e=expr[ValueInfo.Kind.NATIVE, 0]
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().evaluateIndex(where($e.start), $e.res);
}
    ;

logicExpr returns [Expr res]
    :  e=expr[ValueInfo.Kind.NATIVE, 0]
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().evaluateLogic(where($e.start), $e.res);
}
    ;

dataExpr returns [Expr res]
    :  e=expr[ValueInfo.Kind.MODEL, 0]
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().evaluateData(where($e.start), $e.res);
}
    ;   

/*======================================================================================*/
/* Expression rules                                                                     */
/*======================================================================================*/

expr [ValueInfo.Kind target, int depth] returns [Expr res]
@after {$res = $e.res;}
    : e=nonNumExpr[target, depth]
    | e=numExpr[target, depth]
    ;

/*======================================================================================*/
/* Non-numeric expressions (TODO: temporary implementation)                             */
/*======================================================================================*/

nonNumExpr [ValueInfo.Kind target, int depth] returns [Expr res]
@after {$res = $e.res;}
    : e=ifExpr[target, depth]	
    ;

ifExpr [ValueInfo.Kind target, int depth] returns [Expr res]
@after {$res = $e.res;}
    :  ^(IF logicExpr e=expr[target, depth] elseIfExpr[target, depth]* elseExpr[target, depth]?)
    ;

elseIfExpr [ValueInfo.Kind target, int depth] returns [Expr res]
    :  ^(ELSEIF logicExpr expr[target, depth])
    ;

elseExpr [ValueInfo.Kind target, int depth] returns [Expr res]
    :  ^(ELSE expr[target, depth])
    ;

/*======================================================================================*/
/* Numeric expressions                                                                  */
/*======================================================================================*/
    
numExpr [ValueInfo.Kind target, int depth] returns [Expr res]
@after {$res = $e.res;}
    :  e=binaryExpr[target, depth]
    |   e=unaryExpr[target, depth]
    |        e=atom
    ;

binaryExpr [ValueInfo.Kind target, int depth] returns [Expr res]
@after
{
checkNotNull($e1.start, $e1.res, $e1.text);
checkNotNull($e2.start, $e2.res, $e2.text);
$res = getExprFactory().operator(where($op), target, $op.text, $e1.res, $e2.res);
}
    :  ^(op=OR            e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=AND           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=VERT_BAR      e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=UP_ARROW      e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=AMPER         e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=EQ            e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=NEQ           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=LEQ           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=GEQ           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=LEFT_BROCKET  e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=RIGHT_BROCKET e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=LEFT_SHIFT    e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=RIGHT_SHIFT   e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=ROTATE_LEFT   e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=ROTATE_RIGHT  e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=PLUS          e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=MINUS         e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=MUL           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=DIV           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=REM           e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    |  ^(op=DOUBLE_STAR   e1=expr[target, depth + 1] e2=expr[target, depth + 1])
    ;

unaryExpr [ValueInfo.Kind target, int depth] returns [Expr res]
@after
{
checkNotNull($e.start, $e.res, $e.text);
$res = getExprFactory().operator(where($op), target, $op.text, $e.res);
}
    :  ^(op=UPLUS   e=expr[target, depth + 1])
    |  ^(op=UMINUS  e=expr[target, depth + 1])
    |  ^(op=TILDE   e=expr[target, depth + 1])
    |  ^(op=NOT     e=expr[target, depth + 1])
    ;

atom returns [Expr res]
    :  ^(CONST token=ID)  {$res = getExprFactory().namedConstant(where($token), $token.text);}
    |  ^(token=LOCATION le=locationExpr[0])
{
checkNotNull($le.start, $le.res, $le.text);
$res = getExprFactory().location($le.res);
}
    |  token=CARD_CONST   {$res = getExprFactory().constant(where($token), $token.text,10);}
    |  token=BINARY_CONST {$res = getExprFactory().constant(where($token), $token.text, 2);}
    |  token=HEX_CONST    {$res = getExprFactory().constant(where($token), $token.text,16);}
    |  ^(token=COERCE te=typeExpr e=dataExpr)
{
checkNotNull($te.start, $te.res, $te.text);
checkNotNull($e.start,   $e.res,  $e.text);
$res = getExprFactory().coerce(where($token), $e.res, $te.res);
}
    ;

/*======================================================================================*/
/* Location rules (rules for accessing model memory)                                    */
/*======================================================================================*/

location returns [Location res]
    :  ^(LOCATION le=locationExpr[0] {checkNotNull($le.start, $le.res, $le.text);})
{
$res = $le.res;
}
    ;

locationExpr [int depth] returns [Location res]
    :  ^(node=DOUBLE_COLON left=locationVal right=locationExpr[depth+1])
{
checkNotNull($left.start,  $left.res,  $left.text);
checkNotNull($right.start, $right.res, $right.text);

$res = getLocationFactory().concat(where($node), $left.res, $right.res);
}
    |  value=locationVal
{
$res = $value.res;
}
    ;

locationVal returns [LocationAtom res]
    :  ^(node=LOCATION_BITFIELD la=locationAtom je1=indexExpr (je2=indexExpr)?)
{
checkNotNull($la.start, $la.res, $la.text);
checkNotNull($je1.start, $je1.res, $je1.text);

if (null == $je2.res)
    $res = getLocationFactory().bitfield(where($node), $la.res, $je1.res);
else
    $res = getLocationFactory().bitfield(where($node), $la.res, $je1.res, $je2.res);
}
    |  la=locationAtom
{
$res = $la.res;
}
    ;

locationAtom returns [LocationAtom res]
    :  ^(LOCATION_INDEX id=ID e=indexExpr)
{
checkNotNull($e.start, $e.res, $e.text);
$res = getLocationFactory().location(where($id), $id.text, $e.res);
}
    |  id=ID
{
$res = getLocationFactory().location(where($id), $id.text);
}
    ;

/*======================================================================================*/
