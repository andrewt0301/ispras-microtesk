/*
 * Copyright 2008-2013 ISP RAS (http://www.ispras.ru), UniTESK Lab (http://www.unitesk.com)
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

package ru.ispras.microtesk.test.core.iterator;

/**
 * This is a generic iterator interface.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public interface IIterator<T>
{
    /** Initializes the iterator. */
    public void init();

    /**
     * Checks whether the iterator is not exhausted (value is available).
     * 
     * @return <code>true</code> if the iterator is not exhausted;
     *         <code>false</code> otherwise.
     */
    public boolean hasValue();
        
    /**
     * Returns the current value of the iterator.
     * 
     * @return the current value of the iterator.
     */
    public T value();
    
    /** Makes the iteration. */
    public void next();

    /** Stops the iteration. */
    public void stop();
}
