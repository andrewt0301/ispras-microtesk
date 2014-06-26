/*
 * Copyright (c) 2012 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * IMM.java, Dec 1, 2012 2:32:22 PM Andrei Tatarnikov
 */

package ru.ispras.microtesk.model.samples.simple.mode;

import java.util.Map;

import ru.ispras.microtesk.model.api.data.Data;
import ru.ispras.microtesk.model.api.data.DataEngine;
import ru.ispras.microtesk.model.api.memory.Location;
import ru.ispras.microtesk.model.api.simnml.instruction.AddressingMode;
import ru.ispras.microtesk.model.api.simnml.instruction.IAddressingMode;
import ru.ispras.microtesk.model.api.type.Type;

import static ru.ispras.microtesk.model.samples.simple.shared.Shared.*;

/*
mode IMM(i: byte)=i
syntax = format("[%d]", i)
image = format("11%4b", i)
*/

public class IMM extends AddressingMode
{
    public static final String NAME = "IMM";

    public static final Map<String, Type> DECLS = new ParamDeclBuilder()
        .declareParam("i", byte_t).build();

    public static final IFactory FACTORY = new IFactory()
    {
        @Override
        public IAddressingMode create(Map<String, Data> args)
        {
            return new IMM(args);
        }
    };

    public static final IInfo INFO = new Info(IMM.class, NAME, FACTORY, DECLS);
    
    public IMM(Map<String, Data> args)
    {
        this(getArgument("i", DECLS, args));
    }

    private Location i;

    public IMM(Location i)
    {
        this.i = i;
    }

    @Override
    public String syntax()
    {
        return String.format("[%d]", DataEngine.intValue(i.getDataCopy()));
    }

    @Override
    public String image()
    {
        // TODO: NOT SUPPORTED
        // image = format("11%4b", i)
        return null;
    }

    @Override
    public void action()
    {
        // NOTHING
    }

    @Override
    public Location access()
    {
         return i;
    }
}
