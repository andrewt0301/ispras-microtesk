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

parser grammar MmuParser;

//==================================================================================================
// Options
//==================================================================================================

options {
  language=Java;
  tokenVocab=MmuLexer;
  output=AST;
  superClass=ParserBase;
  backtrack=true;
}

import CommonParser;

//==================================================================================================
// Default Exception Handler
//==================================================================================================

@rulecatch {
  catch (SemanticException re) {
    reportError(re);
    recover(input,re);
  }
  catch (RecognitionException re) {
    reportError(re);
    recover(input,re);
  }
}

//==================================================================================================
// Header for the Generated Java File
//==================================================================================================

@header {
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
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT. 
 */

package ru.ispras.microtesk.translator.mmu.grammar;

import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.ParserBase;
import ru.ispras.microtesk.translator.simnml.ESymbolKind;
}

//==================================================================================================
// MMU Specification
//==================================================================================================

startRule 
    : bufferOrAddress* EOF!
    ;

bufferOrAddress
    : address
    | buffer
    ;

//==================================================================================================
// Address
//==================================================================================================

address
    : MMU_ADDRESS^ ID LEFT_BRACE!
        (addressParameter SEMI!)*
      RIGHT_BRACE!
    ;

addressParameter
    : width
    ;

width
    : MMU_WIDTH! ASSIGN! constExpr
    ;

//==================================================================================================
// Buffer
//==================================================================================================

buffer
    : MMU_BUFFER^ ID LEFT_BRACE!
        (bufferParameter SEMI!)*
      RIGHT_BRACE!
    ;

bufferParameter
    : associativity
    | sets
    | line
    | index
    | match
    | policy
    ;

associativity
    : MMU_ASSOCIATIVITY^ ASSIGN! constExpr
    ;

sets
    : MMU_SETS^ ASSIGN! constExpr
    ;

line
    : MMU_LINE^ LEFT_PARENTH! l=lineExpr RIGHT_PARENTH! 
    ;

lineExpr returns [ESymbolKind res]
		:	
			tag
			COMMA!
			data
		;
		
		tag
			:	id=MMU_TAG^ COLON! lengthExpr { declare($id, $lengthExpr.res, false); }
			;
		
		data
			:	id=MMU_DATA^ COLON! lengthExpr { declare($id, $lengthExpr.res, false); }
			;
		
		lengthExpr returns [ESymbolKind res]
			: 		constExpr { $res = ESymbolKind.OP;  }		
			;
		
index
	:	MMU_INDEX^ LEFT_PARENTH! MMU_ADDR! COLON! id=ID RIGHT_PARENTH! ASSIGN! MMU_ADDR! LEFT_BROCKET! ind=indexExpr RIGHT_BROCKET! SEMI! { checkDeclaration($id, $ind.res); }
	;
	
	indexExpr returns [ESymbolKind res]
		:	constExpr     { $res = ESymbolKind.OP;  }  
		;
	
match
	:	MMU_MATCH^ LEFT_PARENTH! MMU_ADDR! COLON! id=ID RIGHT_PARENTH! ASSIGN! LINE! DOT! TAG! EQ! MMU_ADDR! LEFT_BROCKET! ma=matchExpr RIGHT_BROCKET! SEMI! { checkDeclaration($id, $ma.res); }
	;
		
	matchExpr returns [ESymbolKind res]
		:	constExpr     { $res = ESymbolKind.OP;  }
		;
		
policy
    	:	id=MMU_POLICY^ ASSIGN! pol=policyExpr  SEMI! { declare($id, $pol.res, false); }
    	;
    	
policyExpr returns [ESymbolKind res]
    : MMU_RANDOM
    | MMU_FIFO
    | MMU_PLRU
    | MMU_LRU
;
catch [RecognitionException re] {
    reportError(re);
    recover(input,re);
}

//==================================================================================================
// Expression
//==================================================================================================

constExpr
    : expr
    ;

//==================================================================================================
// The End
//==================================================================================================
