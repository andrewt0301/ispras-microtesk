/*
 * Copyright (c) 2014 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * TestDataEngine.java, Oct 3, 2014 3:18:19 PM Andrei Tatarnikov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.test.data;

import java.util.HashMap;
import java.util.Map;

import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.microtesk.model.api.IModel;
import ru.ispras.microtesk.test.template.Argument;
import ru.ispras.microtesk.test.template.Primitive;
import ru.ispras.microtesk.test.template.RandomValue;
import ru.ispras.microtesk.test.template.Situation;
import ru.ispras.microtesk.test.template.UnknownValue;
import ru.ispras.testbase.TestBaseContext;
import ru.ispras.testbase.TestBaseQuery;
import ru.ispras.testbase.TestBaseQueryBuilder;

public final class TestDataEngine
{
    private final IModel model;
    
    public TestDataEngine(IModel model)
    {
        if (null == model)
            throw new NullPointerException();

        this.model = model;
    }

    public TestResult generateData(Situation situation, Primitive primitive)
    {
        System.out.printf("Processing situation %s for %s...%n",
            situation, primitive.getSignature());

        final TestBaseQueryCreator queryCreator =
            new TestBaseQueryCreator(model.getName(), situation, primitive);

        final TestBaseQuery query = queryCreator.getQuery(); 
        System.out.println("Query to TestBase: " + query);

        final Map<String, UnknownValue> unknownValues = 
            queryCreator.getUnknownValues();
        System.out.println("Unknown values: " + unknownValues);

        return new TestResult(TestResult.Status.NODATA);
    }
}

final class TestBaseQueryCreator
{
    private final String processor;
    private final Situation situation;
    private final Primitive primitive;

    private boolean isCreated;
    private TestBaseQuery query;
    private Map<String, UnknownValue> unknownValues;

    public TestBaseQueryCreator(
        String processor, Situation situation, Primitive primitive)
    {
        this.processor = processor;
        this.situation = situation;
        this.primitive = primitive;

        this.isCreated = false;
        this.query = null;
        this.unknownValues = null;
    }

    public TestBaseQuery getQuery()
    {
        createQuery();

        if (null == query)
            throw new NullPointerException();

        return query;
    }

    public Map<String, UnknownValue> getUnknownValues()
    {
        createQuery();

        if (null == unknownValues)
            throw new NullPointerException();

        return unknownValues;
    }

    private void createQuery()
    {
        if (isCreated)
            return;

        final TestBaseQueryBuilder queryBuilder = 
            new TestBaseQueryBuilder();

        createContext(queryBuilder);
        createParameters(queryBuilder);

        final BindingBuilder bindingBuilder = 
            new BindingBuilder(queryBuilder, primitive);

        unknownValues = bindingBuilder.build();
        query = queryBuilder.build();

        isCreated = true;
    }

    private void createContext(TestBaseQueryBuilder queryBuilder)
    {
        queryBuilder.setContextAttribute(
            TestBaseContext.PROCESSOR, processor);

        queryBuilder.setContextAttribute(
            TestBaseContext.INSTRUCTION, primitive.getName());

        queryBuilder.setContextAttribute(
            TestBaseContext.TESTCASE, situation.getName());

        for (Argument arg : primitive.getArguments().values())
        {
            queryBuilder.setContextAttribute(arg.getName(), arg.getTypeName());
        }
    }

    private void createParameters(TestBaseQueryBuilder queryBuilder)
    {
        for (Map.Entry<String, Object> attrEntry :
            situation.getAttributes().entrySet())
        {
            queryBuilder.setParameter(
                attrEntry.getKey(), attrEntry.getValue().toString());
        }
    }

    private static final class BindingBuilder
    {
        private final TestBaseQueryBuilder queryBuilder;
        private final Map<String, UnknownValue> unknownValues;
        private final Primitive primitive;
        private boolean isBuilt;

        private BindingBuilder(
            TestBaseQueryBuilder queryBuilder,
            Primitive primitive
            )
        {
            if (null == queryBuilder)
                throw new NullPointerException();

            if (null == primitive)
                throw new NullPointerException();

            this.queryBuilder = queryBuilder;
            this.primitive = primitive;
            this.unknownValues = new HashMap<String, UnknownValue>();
            this.isBuilt = false;
        }

        public Map<String, UnknownValue> build()
        {
            if (isBuilt)
                throw new IllegalStateException();

            visit("", primitive);

            isBuilt = true;
            return unknownValues;
        }

        private void visit(String prefix, Primitive p)
        {
            if (p.getSituation() != null && !prefix.isEmpty())
                throw new IllegalArgumentException(String.format(
                    "Error: The %s argument (type %s) is an operation with " +
                    "test situation %s. The current version does not support " +
                    "nesting of test situations.", prefix, p.getTypeName()));

            for (Argument arg : p.getArguments().values())
            {
                final String argName = prefix.isEmpty() ?
                    arg.getName() : String.format("%s.%s", prefix, arg.getName());

                switch (arg.getKind())
                {
                case IMM:
                    queryBuilder.setBinding(argName,
                        NodeValue.newInteger((Integer) arg.getValue()));
                    break;

                case IMM_RANDOM:
                    queryBuilder.setBinding(argName,
                        NodeValue.newInteger(((RandomValue) arg.getValue()).getValue()));
                    break;

                case IMM_UNKNOWN:
                    queryBuilder.setBinding(argName,
                        new NodeVariable(new Variable(argName, DataType.INTEGER)));
                    unknownValues.put(argName, (UnknownValue) arg.getValue());
                    break;

                case MODE:
                case OP:
                    visit(argName, (Primitive) arg.getValue());
                    break;

                default:
                    throw new IllegalArgumentException(
                        "Illegal kind: " + arg.getKind());
                }
            }
        }
    }
}

/*
final ISituation situation =
    instruction.createSituation(situationName);

// This is needed for situations like random that do not have a signature 
// and generate values for any parameters the client code might request.
// Other situations may ignore these calls.

for (Argument argument : rootOperation.getArguments().values())
    situation.setOutput(argument.getName());

Map<String, Data> output = null;

try 
{
    output = situation.solve();
}
catch (ConfigurationException e)
{
    System.out.printf("Warning! Failed to generate test data for the %s situation.\nReason: %s.\n",
        situationName, e.getMessage());

    return;
}

for (Map.Entry<String, Data> entry : output.entrySet())
{
    final Argument argument = rootOperation.getArguments().get(entry.getKey());

    if (null == argument)
    {
        System.out.printf("Argument %s is not defined for instruction %s.%n",
           entry.getKey(), rootOperation.getName());
        continue;
    }

    insertInitializingCalls(argument, entry.getValue());
}
*/

/*
private void insertInitializingCalls(Argument argument, Data value) throws ConfigurationException
{
    final String argumentTypeName = argument.isImmediate() ?
        AddressingModeImm.NAME : ((Primitive) argument.getValue()).getName();
    
    System.out.printf(
        "Initializer: argument: %7s, mode: %10s, value: %s (%s) %n",
        argument.getName(),
        argumentTypeName,
        Integer.toHexString(value.getRawData().intValue()),
        value.getRawData().toBinString()
    );

    for(IInitializerGenerator ig : model.getInitializers())
    {
        if (ig.isCompatible(argument))
        {
            final List<ConcreteCall> calls = ig.createInitializingCode(argument, value);
            sequenceBuilder.addInitializingCalls(calls);
            return;
        }
    }

    System.out.println(
        String.format(
            "Error! Failed to find an initializer generator for argument %s (addressing mode: %s).",
             argument.getName(),
             argumentTypeName
        )
    );
}
*/
