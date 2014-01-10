/*
 * Copyright (c) 2013 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * StatementCondition.java, Jul 19, 2013 11:55:00 AM Andrei Tatarnikov
 */

package ru.ispras.microtesk.translator.simnml.ir.primitive;

import java.util.List;
import ru.ispras.microtesk.translator.simnml.ir.expression.Expr;

public final class StatementCondition extends Statement
{
    public static final class Block
    {
        private final Expr             condition;
        private final List<Statement> statements;

        private Block(Expr condition, List<Statement> statements)
        {
            if (null == statements)
                throw new NullPointerException();

            this.condition = condition;
            this.statements = statements;
        }

        public static Block newIfBlock(Expr condition, List<Statement> statements)
        {
            if (null == condition)
                throw new NullPointerException();

            return new Block(condition, statements);
        }

        public static Block newElseBlock(List<Statement> statements)
        {
            return new Block(null, statements);
        }

        public Expr getCondition()
        {
            return condition;
        }

        public boolean isElseBlock()
        {
            return null == condition;
        }

        public List<Statement> getStatements()
        {
            return statements;
        }
    }

    private final List<Block> blocks;

    StatementCondition(List<Block> blocks)
    {
        super(Kind.COND);

        if (null == blocks)
            throw new NullPointerException();

        if (blocks.isEmpty())
            throw new IllegalArgumentException();

        this.blocks = blocks;
    }

    public int getBlockCount()
    {
        return blocks.size();
    }

    public Block getBlock(int index)
    {
        if (!((0 <= index) && (index < getBlockCount())))
            throw new IndexOutOfBoundsException();

        return blocks.get(index);
    }
}
