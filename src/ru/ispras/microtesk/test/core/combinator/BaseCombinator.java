/*
 * Copyright 2013 ISP RAS (http://www.ispras.ru), UniTESK Lab (http://www.unitesk.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ispras.microtesk.test.core.combinator;

import java.util.ArrayList;
import java.util.List;

import ru.ispras.microtesk.test.core.iterator.IIterator;

/**
 * This class is a basic combinator of iterators. It takes several iterators
 * and produces different combinations of their results.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public abstract class BaseCombinator<T> implements IIterator<List<T>>
{
    /// The iterators to be combined.
    protected ArrayList<IIterator<T>> iterators = new ArrayList<IIterator<T>>();

    /**
     * Constructs a compositor with the empty list of iterators.
     */
    public BaseCombinator()
    {
    }
    
    /**
     * Constructs a compositor with the given list of iterators.
     *
     * @param iterators the list of iterators to be composed.
     */
    public BaseCombinator(final List<IIterator<T>> iterators)
    {
        this.iterators.addAll(iterators);
    }
    
    /**
     * Adds the iterator into the compositor's list.
     *
     * @param iterator the iterator to be added to the compositor's list.
     */
    public void addIterator(final IIterator<T> iterator)
    {
        iterators.add(iterator);
    }

    /**
     * Removes the i-th iterator from the compositor's list.
     *
     * @param i the index of the iterator to be removed from the compositor's list.
     */
    public void removeIterator(int i)
    {
        iterators.remove(i);
    }
    
    /**
     * Returns the number of iterators in the compositor's list.
     *
     * @return the size of the compositor's list.
     */
    public int size()
    {
        return iterators.size();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Iterator methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public abstract void init();
    
    @Override
    public abstract boolean hasValue();

    @Override
    public abstract List<T> value();

    @Override
    public abstract void next();
}