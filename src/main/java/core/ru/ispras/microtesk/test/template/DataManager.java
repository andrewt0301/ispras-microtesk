/*
 * Copyright 2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.template;

import static ru.ispras.microtesk.utils.InvariantChecks.checkNotNull;
import static ru.ispras.microtesk.utils.InvariantChecks.checkGreaterThanZero;
import static ru.ispras.microtesk.utils.PrintingUtils.trace;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.microtesk.model.api.memory.Memory;
import ru.ispras.microtesk.model.api.memory.MemoryAllocator;
import ru.ispras.microtesk.model.api.type.Type;

public final class DataManager {
  private static interface DataDeclItem {
    String getText();
  }

  private static class DetaDeclText implements DataDeclItem {
    private final String text;

    DetaDeclText(String text) {
      this.text = text;
    }

    @Override
    public String getText() {
      return text;
    }
  }

  private static final class DetaDeclLabel extends DetaDeclText {
    DetaDeclLabel(String text) {
      super(text);
    }

    @Override
    public String getText() {
      return super.text + ":";
    }
  }

  private static final class DetaDeclSpace extends DetaDeclText {
    private final int count;
    
    DetaDeclSpace(String text, int count) {
      super(text);
      this.count = count;
    }

    @Override
    public String getText() {
      return String.format("%s %d", super.getText(), count);
    }
  }
  
  private static final class DetaDeclStrings extends DetaDeclText {
    final String[] strings;

    DetaDeclStrings(String text, String[] strings) {
      super(text);
      this.strings = strings;
    }

    @Override
    public String getText() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.getText());

      for (int i = 0; i < strings.length; i++) {
        if (i > 0) {
          sb.append(",");
        }

        sb.append(String.format(" \"%s\"", strings[i]));
      }

      return sb.toString();
    }
  }
  
  private static final class DetaDecl extends DetaDeclText {
    final BigInteger[] values;

    DetaDecl(String text, BigInteger[] values) {
      super(text);
      this.values = values;
    }

    @Override
    public String getText() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.getText());

      for (int i = 0; i < values.length; i++) {
        if (i > 0) {
          sb.append(",");
        }

        sb.append(String.format(" 0x%x", values[i]));
      }

      return sb.toString();
    }
  }

  private final MemoryMap memoryMap;
  private final List<DataDeclItem> dataDecls;

  private MemoryAllocator allocator;
  private List<String> labels;

  private String spaceText;
  private BitVector spaceData;
  private String ztermStrText;
  private String nztermStrText;

  private final Map<String, TypeInfo> typeMap;
  final static class TypeInfo {
    final Type type;
    final String text;

    public TypeInfo(Type type, String text) {
      this.type = type;
      this.text = text;
    }
  }

  DataManager(MemoryMap memoryMap) {
    checkNotNull(memoryMap);

    this.memoryMap = memoryMap;
    this.dataDecls = new ArrayList<>();

    this.allocator = null;
    this.labels = null;

    this.spaceText = null;
    this.spaceData = null;
    this.ztermStrText = null;
    this.nztermStrText = null;

    this.typeMap = new HashMap<>(); 
  }

  public void init(final String text, final String target, final int addressableSize) {
    checkNotNull(text);
    checkNotNull(target);

    if (isInitialized()) {
      throw new IllegalStateException("DataManager is already initialized!");
    }

    final Memory memory = Memory.getMemory(target);
    allocator = memory.newAllocator(addressableSize);

    dataDecls.add(new DetaDeclText(text)); 
  }

  public boolean isInitialized() {
    return allocator != null;
  }

  public String getDataDeclText() {
    if (!isInitialized()) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    for (DataDeclItem item : dataDecls) {
      sb.append(item.getText());
      sb.append("\r\n");
    }

    return sb.toString();
  }

  public void defineType(String id, String text, String typeName, int[] typeArgs) {
    checkNotNull(id);
    checkNotNull(text);
    checkNotNull(typeName);
    checkNotNull(typeArgs);

    checkInitialized();

    final Type type = Type.typeOf(typeName, typeArgs);
    trace("Defining %s as %s ('%s')...", type, id, text);

    typeMap.put(id, new TypeInfo(type, text));
  }

  public void defineSpace(String id, String text, BigInteger fillWith) {
    checkNotNull(id);
    checkNotNull(text);
    checkNotNull(fillWith);

    checkInitialized();
    trace("Defining space as %s ('%s') filled with %x...", id, text, fillWith);

    spaceText = text;
    spaceData = BitVector.valueOf(fillWith, allocator.getAddressableUnitBitSize());
  }

  public void defineAsciiString(String id, String text, boolean zeroTerm) {
    checkNotNull(id);
    checkNotNull(text);
    checkInitialized();

    trace("Defining %snull-terminated ASCII string as %s ('%s')...", zeroTerm ? "" : "not ", id, text);
    
    if (zeroTerm) {
      ztermStrText = text;
    } else {
      nztermStrText = text;
    }
  }

  public void addLabel(String id) {
    checkNotNull(id);
    checkInitialized();

    trace("Label %s", id);

    if (null == labels) {
      labels = new ArrayList<>();
    }

    labels.add(id);
    dataDecls.add(new DetaDeclLabel(id));
  }

  private void setAllLabelsToAddress(int address) {
    checkInitialized();

    if (null != labels) {
      for (String label : labels) {
        memoryMap.addLabel(label, address);
      }
      labels = null;
    }
  }

  public void addData(String id, BigInteger[] values) {
    checkNotNull(id);
    checkNotNull(values);
    checkGreaterThanZero(values.length);

    checkInitialized();

    final TypeInfo typeInfo = typeMap.get(id);
    if (null == typeInfo) {
      throw new IllegalStateException();
    }

    final int address = allocator.allocate(
        BitVector.valueOf(values[0], typeInfo.type.getBitSize()));

    for (int i = 1; i < values.length; i++) {
      allocator.allocate(
          BitVector.valueOf(values[i], typeInfo.type.getBitSize()));
    }

    setAllLabelsToAddress(address);
    dataDecls.add(new DetaDecl(typeInfo.text, values));
  }

  public void addSpace(int length) {
    checkGreaterThanZero(length);
    checkInitialized();

    if (null == spaceData) {
      throw new IllegalStateException();
    }

    final int address = allocator.allocate(spaceData, length);

    setAllLabelsToAddress(address);
    dataDecls.add(new DetaDeclSpace(spaceText, length));
  }

  public void addAsciiStrings(boolean zeroTerm, String[] strings) {
    checkNotNull(strings);
    checkGreaterThanZero(strings.length);
    checkInitialized();

    if (zeroTerm && (null == ztermStrText)) {
      throw new IllegalStateException();
    }

    if (!zeroTerm && (null == nztermStrText)) {
      throw new IllegalStateException();
    }

    final int address = allocator.allocateAsciiString(strings[0], zeroTerm);
    for (int index = 1; index < strings.length; index++) {
      allocator.allocateAsciiString(strings[index], zeroTerm);
    }

    setAllLabelsToAddress(address);
    dataDecls.add(new DetaDeclStrings((zeroTerm ? ztermStrText : nztermStrText), strings));
  }

  private void checkInitialized() {
    if (!isInitialized()) {
      throw new IllegalStateException("DataManager is not initialized!");
    }
  }
}
