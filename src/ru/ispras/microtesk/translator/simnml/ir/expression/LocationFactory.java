/*
 * Copyright (c) 2013 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * LocationFactory.java, Aug 7, 2013 12:48:09 PM Andrei Tatarnikov
 */

package ru.ispras.microtesk.translator.simnml.ir.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.Where;
import ru.ispras.microtesk.translator.antlrex.errors.SymbolTypeMismatch;
import ru.ispras.microtesk.translator.antlrex.errors.UndeclaredSymbol;
import ru.ispras.microtesk.translator.antlrex.symbols.ISymbol;
import ru.ispras.microtesk.translator.simnml.ESymbolKind;
import ru.ispras.microtesk.translator.simnml.antlrex.WalkerContext;
import ru.ispras.microtesk.translator.simnml.antlrex.WalkerFactoryBase;
import ru.ispras.microtesk.translator.simnml.errors.UndefinedPrimitive;
import ru.ispras.microtesk.translator.simnml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.simnml.ir.shared.MemoryExpr;
import ru.ispras.microtesk.translator.simnml.ir.shared.TypeExpr;

public final class LocationFactory extends WalkerFactoryBase
{
    private static final String OUT_OF_BOUNDS =
        "The bitfield expression tries to access bit %d which is beyond location bounds (%d bits).";

    private List<LocationAtom> log; 

    public void setLog(List<LocationAtom> locations)
    {
        log = locations;
    }

    public List<LocationAtom> getLog()
    {
        return log;
    }

    public void resetLog()
    {
        log = null;
    }
    
    private void addToLog(LocationAtom location)
    {
        if (null != log)
            log.add(location);
    }

    public LocationFactory(WalkerContext context)
    {
        super(context);
        resetLog();
    }

    public LocationAtom location(Where where, String name) throws SemanticException
    {
        final ISymbol<ESymbolKind> symbol = findSymbol(where, name);
        final ESymbolKind kind = symbol.getKind();

        if ((ESymbolKind.MEMORY != kind) && (ESymbolKind.ARGUMENT != kind))
            raiseError(where, new SymbolTypeMismatch<ESymbolKind>(name, kind, Arrays.asList(ESymbolKind.MEMORY, ESymbolKind.ARGUMENT)));

        final LocationCreator creator = (ESymbolKind.MEMORY == kind) ?
            new MemoryBasedLocationCreator(this, where, name, null) :
            new ArgumentBasedLocationCreator(this, where, name);

        final LocationAtom result = creator.create();

        addToLog(result);
        return result;
    }

    public LocationAtom location(Where where, String name, Expr index) throws SemanticException
    {
        assert null != index;

        final ISymbol<ESymbolKind> symbol = findSymbol(where, name);
        final ESymbolKind kind = symbol.getKind();

        if (ESymbolKind.MEMORY != kind)
            raiseError(where, new SymbolTypeMismatch<ESymbolKind>(name, kind, ESymbolKind.MEMORY));

        final LocationCreator creator = new MemoryBasedLocationCreator(this, where, name, index);
        final LocationAtom result = creator.create();

        addToLog(result);
        return result;
    }

    public LocationAtom bitfield(Where where, LocationAtom location, Expr from, Expr to) throws SemanticException
    {
        assert null != location;
        assert null != from;
        assert null != to;

        final int fromPos = ((Number) from.getValue()).intValue();
        final int   toPos = ((Number) to.getValue()).intValue();

        final int bitfieldSize = toPos - fromPos + 1;
        final int locationSize = ((Number) location.getType().getBitSize().getValue()).intValue();

        // assert startPos <= endPos; // TODO: restriction of the current implementation

        if (fromPos >= locationSize)
            raiseError(where, String.format(OUT_OF_BOUNDS, fromPos, locationSize));

        if (toPos >= locationSize)
            raiseError(where, String.format(OUT_OF_BOUNDS, toPos, locationSize));

        final TypeExpr bitfieldType = new TypeExpr(
            location.getType().getTypeId(), ExprClass.createConstant(bitfieldSize));

        return LocationAtom.createBitfield(location, from, to, bitfieldType);
    }

    public LocationConcat concat(Where w, LocationAtom left, Location right)
    {
        assert null != left;
        assert null != right;

        final int leftSize =
            ((Number) left.getType().getBitSize().getValue()).intValue();

        final int rightSize =
            ((Number) right.getType().getBitSize().getValue()).intValue();

        final int concatSize = leftSize + rightSize; 

        final TypeExpr concatType = new TypeExpr(
            left.getType().getTypeId(), ExprClass.createConstant(concatSize));

        if (right instanceof LocationAtom)
            return new LocationConcat(concatType, Arrays.asList((LocationAtom) right, left));

        final List<LocationAtom> concatenated = new ArrayList<LocationAtom>(((LocationConcat) right).getLocations());
        concatenated.add(left);

        return new LocationConcat(concatType, concatenated);
    }

    private ISymbol<ESymbolKind> findSymbol(Where where, String name) throws SemanticException
    {
        final ISymbol<ESymbolKind> symbol = getSymbols().resolve(name);

        if (null == symbol)
            raiseError(where, new UndeclaredSymbol(name));

        return symbol;
    }
}

interface LocationCreator
{
    public LocationAtom create() throws SemanticException;
}

final class MemoryBasedLocationCreator extends WalkerFactoryBase implements LocationCreator
{
    private static final String BAD_INDEX_EXPR =
        "The %s expression cannot be used as an index. It is not a Java-compatible integer expression.";

    private final Where where;
    private final String name;
    private final Expr  index;

    public MemoryBasedLocationCreator(WalkerContext context, Where where, String name, Expr index)
    {
        super(context);

        this.where = where;
        this.name  = name;
        this.index = index;
    }

    @Override
    public LocationAtom create() throws SemanticException
    {
        final MemoryExpr memory = findMemory();

        if (null != index)
            checkIndexType(index);

        return LocationAtom.createMemoryBased(name, memory, index);
    }

    private MemoryExpr findMemory() throws SemanticException
    {
        if (!getIR().getMemory().containsKey(name))
            raiseError(where, new UndefinedPrimitive(name, ESymbolKind.MEMORY));

        return getIR().getMemory().get(name);
    }

    private void checkIndexType(final Expr index) throws SemanticException
    {
        final String errorMessage =
            String.format(BAD_INDEX_EXPR, index.getText());

        final boolean isJavaExpr = 
           (index.getKind() == EExprKind.JAVA) || (index.getKind() == EExprKind.JAVA_STATIC);

        if (!isJavaExpr)
            raiseError(where, errorMessage);

        assert null != index.getJavaType();

        final boolean isIntegerType =
            index.getJavaType().equals(int.class) || index.getJavaType().equals(Integer.class); 

        if (!isIntegerType)
            raiseError(where, errorMessage);
    }
}

final class ArgumentBasedLocationCreator extends WalkerFactoryBase implements LocationCreator
{
    private static final String UNEXPECTED_PRIMITIVE =
        "The %s argument refers to a %s primitive that cannot be used as a location.";

    private final Where where;
    private final String name;

    public ArgumentBasedLocationCreator(WalkerContext context, Where where, String name)
    {
        super(context);

        this.where = where;
        this.name  = name;
    }

    @Override
    public LocationAtom create() throws SemanticException
    {
        final Primitive primitive = findArgument();

        if ((Primitive.Kind.MODE != primitive.getKind()) && (Primitive.Kind.IMM != primitive.getKind()))
            raiseError(where, String.format(UNEXPECTED_PRIMITIVE, name, primitive.getKind()));            

        return LocationAtom.createPrimitiveBased(name, primitive);
    }

    private Primitive findArgument() throws SemanticException
    {
        if (!getThisArgs().containsKey(name))
            raiseError(where, new UndefinedPrimitive(name, ESymbolKind.ARGUMENT));

        return getThisArgs().get(name);
    }
}
