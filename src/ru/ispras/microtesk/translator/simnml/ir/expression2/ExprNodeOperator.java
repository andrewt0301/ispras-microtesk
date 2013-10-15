/*
 * Copyright (c) 2013 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * ExprOperator.java, Aug 14, 2013 12:45:13 PM Andrei Tatarnikov
 */

package ru.ispras.microtesk.translator.simnml.ir.expression2;

import java.util.List;

public final class ExprNodeOperator extends ExprAbstract
{
    private final Operator   operator;
    private final List<Expr> operands;
    private final ValueInfo      cast;

    ExprNodeOperator(Operator operator, List<Expr> operands, ValueInfo resultValueInfo, ValueInfo castValueInfo)
    {
        super(NodeKind.OPERATOR, resultValueInfo);

        assert null != operator;
        this.operator = operator;

        assert null != operands;
        this.operands = operands;

        this.cast = castValueInfo;
    }

    public Operator getOperator()
    {
        return operator;
    }

    public List<Expr> getOperands()
    {
        return operands;
    }

    public ValueInfo getCast()
    {
        return cast;
    }
}
