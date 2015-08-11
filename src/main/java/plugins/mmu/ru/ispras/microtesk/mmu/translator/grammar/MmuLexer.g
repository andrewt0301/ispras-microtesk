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

lexer grammar MmuLexer;

import commonLexer=CommonLexer;

//==================================================================================================
// Header for the generated Java class file.
//==================================================================================================

@header {
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
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT. 
 */

package ru.ispras.microtesk.mmu.translator.grammar;

import ru.ispras.microtesk.translator.antlrex.Preprocessor;
import ru.ispras.microtesk.translator.antlrex.symbols.SymbolTable;
}

@members {
  private Preprocessor pp = null;

  public MmuLexer(final CharStream chars, final Preprocessor pp, final SymbolTable symbols) {
    this(chars);

    commonLexer.setPreprocessor(pp);
    this.pp = pp;

    commonLexer.setSymbols(symbols);
  }

  private void pp() {
    if(pp.isHidden()) {
      skip();
    }
  }
}

//==================================================================================================
// MMU Keywords
//==================================================================================================

MMU_LET     : 'let';
MMU_STRUCT  : 'struct';
MMU_ADDRESS : 'address';

MMU_SEGMENT : 'segment';
MMU_RANGE   : 'range';

MMU_BUFFER  : 'buffer';
MMU_VIEWOF  : 'viewof';
MMU_WAYS    : 'ways';
MMU_SETS    : 'sets';
MMU_ENTRY   : 'entry';
MMU_INDEX   : 'index';
MMU_MATCH   : 'match';
MMU_POLICY  : 'policy';
MMU_GUARD   : 'guard';

MMU         : 'mmu';
MMU_VAR     : 'var';

//==================================================================================================
// The end
//==================================================================================================
