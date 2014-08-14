/*
 * Copyright (c) 2014 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * LabelManagerTestCase.java, Aug 14, 2014 3:40:58 PM Andrei Tatarnikov
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

package ru.ispras.microtesk.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ru.ispras.microtesk.test.LabelManager.Target;
import ru.ispras.microtesk.test.template.BlockId;
import ru.ispras.microtesk.test.template.Label;

/**
 * This is a unit test to check the mechanism of resolving label references.
 * Test cases found different labels located in different places of the block
 * tree below being references from different blocks. 
 * 
 * <p><i>Block structure:</i> 
 * <pre>
 *                                       root
 *                 _______________________|_____________________                   
 *                /                       |                     \
 *           child1                    child2                   child3
 *        _____|_____                 ____|____                ____|____
 *       /     |     \               /    |    \              /    |    \ 
 * child11  child12  child13   child21 child22  child23 child31 child32 child33
 *    |        |        |        |        |        |      |        |        | 
 * child111 child121 child131 child211 child221 child231 child311 child321 child331
 * </pre>
 * 
 * @author Andrei Tatarnikov 
 */

public class LabelManagerTestCase
{
    private final BlockId root;

    private final BlockId child1;
    private final BlockId child2;
    private final BlockId child3;

    private final BlockId child11;
    private final BlockId child12;
    private final BlockId child13;

    private final BlockId child21;
    private final BlockId child22;
    private final BlockId child23;

    private final BlockId child31;
    private final BlockId child32;
    private final BlockId child33;
    
    private final BlockId child111;
    private final BlockId child121;
    private final BlockId child131;

    private final BlockId child211;
    private final BlockId child221;
    private final BlockId child231;

    private final BlockId child311;
    private final BlockId child321;
    private final BlockId child331;

    public LabelManagerTestCase()
    {
        this.root = new BlockId();

        this.child1 = root.nextChildId();
        this.child2 = root.nextChildId();
        this.child3 = root.nextChildId();

        this.child11 = child1.nextChildId();
        this.child12 = child1.nextChildId();
        this.child13 = child1.nextChildId();

        this.child21 = child2.nextChildId();
        this.child22 = child2.nextChildId();
        this.child23 = child2.nextChildId();

        this.child31 = child3.nextChildId();
        this.child32 = child3.nextChildId();
        this.child33 = child3.nextChildId();

        this.child111 = child11.nextChildId();
        this.child121 = child12.nextChildId();
        this.child131 = child13.nextChildId();

        this.child211 = child21.nextChildId();
        this.child221 = child22.nextChildId();
        this.child231 = child23.nextChildId();

        this.child311 = child31.nextChildId();
        this.child321 = child32.nextChildId();
        this.child331 = child33.nextChildId();
    }

    @Test
    public void testNoChoice()
    {
        final LabelManager labelManager = new LabelManager();

        labelManager.addLabel(new Label("x", child111), 10);
        labelManager.addLabel(new Label("y", child311), 20);

        final Target target = labelManager.resolve(new Label("z", root));
        assertNull(target);
    }

    @Test
    public void testSingleChoice()
    {
        final LabelManager labelManager = new LabelManager();

        final Target targetX = new Target(new Label("x", child111), 10);
        labelManager.addLabel(targetX.getLabel(), targetX.getPosition());

        final Target targetY = new Target(new Label("y", child311), 20);
        labelManager.addLabel(targetY.getLabel(), targetY.getPosition());

        assertEquals(targetX, labelManager.resolve(new Label("x", root)));
        assertEquals(targetY, labelManager.resolve(new Label("y", root)));

        assertEquals(targetX, labelManager.resolve(new Label("x", child21)));
        assertEquals(targetY, labelManager.resolve(new Label("y", child21)));

        assertEquals(targetX, labelManager.resolve(new Label("x", child211)));
        assertEquals(targetY, labelManager.resolve(new Label("y", child211)));
    }

    @Test
    public void testChooseCurrent()
    {
        final LabelManager labelManager = new LabelManager();

        final Target targetXRoot = new Target(new Label("x", root), 10);
        labelManager.addLabel(
            targetXRoot.getLabel(), targetXRoot.getPosition());

        final Target targetXChild2 = new Target(new Label("x", child2), 20);
        labelManager.addLabel(
            targetXChild2.getLabel(), targetXChild2.getPosition());

        final Target targetXChild21 = new Target(new Label("x", child21), 30);
        labelManager.addLabel(
            targetXChild21.getLabel(), targetXChild21.getPosition());

        final Target targetXChild211 = new Target(new Label("x", child211), 40);
        labelManager.addLabel(
            targetXChild211.getLabel(), targetXChild211.getPosition());

        assertEquals(targetXRoot,     labelManager.resolve(new Label("x", root)));
        assertEquals(targetXChild2,   labelManager.resolve(new Label("x", child2)));
        assertEquals(targetXChild21,  labelManager.resolve(new Label("x", child21)));
        assertEquals(targetXChild211, labelManager.resolve(new Label("x", child211)));
    }
}
