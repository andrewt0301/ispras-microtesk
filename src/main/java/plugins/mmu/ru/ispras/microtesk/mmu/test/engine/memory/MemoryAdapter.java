/*
 * Copyright 2006-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.test.engine.memory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.MmuPlugin;
import ru.ispras.microtesk.mmu.basis.BufferAccessEvent;
import ru.ispras.microtesk.mmu.basis.DataType;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;
import ru.ispras.microtesk.mmu.basis.MemoryAccessStack;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAddressInstance;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuEntry;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;
import ru.ispras.microtesk.model.ConfigurationException;
import ru.ispras.microtesk.test.ConcreteSequence;
import ru.ispras.microtesk.test.engine.Adapter;
import ru.ispras.microtesk.test.engine.AdapterResult;
import ru.ispras.microtesk.test.engine.EngineContext;
import ru.ispras.microtesk.test.engine.utils.AddressingModeWrapper;
import ru.ispras.microtesk.test.engine.utils.EngineUtils;
import ru.ispras.microtesk.test.template.AbstractCall;
import ru.ispras.microtesk.test.template.AbstractSequence;
import ru.ispras.microtesk.test.template.BlockId;
import ru.ispras.microtesk.test.template.BufferPreparator;
import ru.ispras.microtesk.test.template.BufferPreparatorStore;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.DataDirectiveFactory;
import ru.ispras.microtesk.test.template.DataSectionBuilder;
import ru.ispras.microtesk.test.template.DataSectionBuilder.DataValueBuilder;
import ru.ispras.microtesk.test.template.MemoryPreparator;
import ru.ispras.microtesk.test.template.MemoryPreparatorStore;
import ru.ispras.microtesk.test.template.Primitive;
import ru.ispras.microtesk.test.template.Situation;
import ru.ispras.microtesk.test.testbase.AddressDataGenerator;

/**
 * {@link MemoryAdapter} implements the memory engine adapter.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MemoryAdapter implements Adapter {

  static final MemoryEngine.ParamPreparator PARAM_PREPARATOR = MemoryEngine.PARAM_PREPARATOR;
  private boolean isStaticPreparator = PARAM_PREPARATOR.getDefaultValue();

  private final Set<BigInteger> entriesInDataSection = new HashSet<>();

  @Override
  public void configure(final Map<String, Object> attributes) {
    InvariantChecks.checkNotNull(attributes);
    isStaticPreparator = PARAM_PREPARATOR.parse(attributes.get(PARAM_PREPARATOR.getName()));
  }

  @Override
  public AdapterResult adapt(
      final EngineContext engineContext,
      final AbstractSequence abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    final ConcreteSequence.Builder builder = new ConcreteSequence.Builder();

    // Write entries into the non-replaceable buffers.
    builder.addToPrologue(prepareEntries(engineContext, abstractSequence));
    // Load addresses and data into the registers.
    builder.addToPrologue(prepareAddresses(engineContext, abstractSequence));

    // Convert the abstract sequence into the concrete one.
    builder.add(prepareSequence(engineContext, abstractSequence));

    final ConcreteSequence sequence = builder.build();
    return new AdapterResult(sequence);
  }

  @Override
  public void onStartProgram() {
    entriesInDataSection.clear();
  }

  @Override
  public void onEndProgram() {
    Logger.debug("Allocated entries: %s", entriesInDataSection);
  }

  private List<ConcreteCall> prepareEntries(
      final EngineContext engineContext,
      final AbstractSequence abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    final List<ConcreteCall> sequence = new ArrayList<>(); 
    for (final AbstractCall abstractCall : abstractSequence.getSequence()) {
      final AddressObject addressObject = MemoryEngine.getAddressObject(abstractCall);
      final Map<MmuBufferAccess, EntryObject> entries = addressObject.getEntries();

      for (final Map.Entry<MmuBufferAccess, EntryObject> entry : entries.entrySet()) {
        final MmuBufferAccess bufferAccess = entry.getKey();
        final EntryObject entryObject = entry.getValue();

        sequence.addAll(
            prepareEntries(
                bufferAccess,
                entryObject,
                engineContext,
                entriesInDataSection
            )
        );
      }
    }

    return sequence;
  }

  private List<ConcreteCall> prepareEntries(
      final MmuBufferAccess bufferAccess,
      final EntryObject entryObject,
      final EngineContext engineContext,
      final Set<BigInteger> entriesInDataSection) {
    InvariantChecks.checkNotNull(bufferAccess);
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(entriesInDataSection);

    final MmuSubsystem memory = MmuPlugin.getSpecification();
    final MmuBuffer buffer = bufferAccess.getBuffer();

    final BlockId blockId = new BlockId();
    final DataDirectiveFactory dataDirectiveFactory = engineContext.getDataDirectiveFactory();
    InvariantChecks.checkNotNull(dataDirectiveFactory);

    final List<ConcreteCall> preparation = new ArrayList<>();

    final BigInteger index = entryObject.getId();
    final MmuEntry data = entryObject.getEntry();
    final BigInteger bufferAccessAddress = data.getAddress();

    Logger.debug("Entry preparation: index=0x%s, address=0x%s",
        index.toString(16), bufferAccessAddress.toString(16));

    final Map<String, BitVector> entryFieldValues = new LinkedHashMap<>();

    for (final IntegerVariable field : data.getVariables()) {
      final String entryFieldName = field.getName();
      final BigInteger entryFieldValue = data.getValue(field);

      entryFieldValues.put(entryFieldName, BitVector.valueOf(entryFieldValue, field.getWidth()));
    }

    final boolean isMemoryMapped = buffer.getKind() == MmuBuffer.Kind.MEMORY;
    final boolean isEntryInDataSection = entriesInDataSection.contains(bufferAccessAddress);

    final String comment = String.format("%s[0x%s]=%s",
        buffer.getName(), bufferAccessAddress.toString(16), data);

    final List<BitVector> fieldValues = new ArrayList<>(entryFieldValues.values());
    Collections.reverse(fieldValues);

    final BitVector entryValue =
        BitVector.newMapping(fieldValues.toArray(new BitVector[fieldValues.size()]));

    final int sizeInBits = entryValue.getBitSize();
    InvariantChecks.checkTrue((sizeInBits & 0x3) == 0);

    if (isMemoryMapped && isStaticPreparator && !isEntryInDataSection) {
      Logger.debug("Entries in data section: %s", entriesInDataSection);

      final DataSectionBuilder dataSectionBuilder = new DataSectionBuilder(
          blockId, dataDirectiveFactory, true /* Global section */, false /* Same file */);

      dataSectionBuilder.setVirtualAddress(bufferAccessAddress);
      dataSectionBuilder.addComment(comment);

      final int maxItemSizeInBits = dataDirectiveFactory.getMaxTypeBitSize();

      int itemSizeInBits = 8;
      while (itemSizeInBits < sizeInBits && itemSizeInBits < maxItemSizeInBits) {
        itemSizeInBits <<= 1;
      }

      final DataValueBuilder dataValueBuilder =
          dataSectionBuilder.addDataValuesForSize(itemSizeInBits);

      for (int i = 0; i < sizeInBits; i += itemSizeInBits) {
        final BitVector item = entryValue.field(i, i + itemSizeInBits - 1);
        dataValueBuilder.add(item.bigIntegerValue());

        // Static buffer initialization.
        entriesInDataSection.add(bufferAccessAddress.add(BigInteger.valueOf(i >>> 3)));
      }

      dataValueBuilder.build();

      final AbstractCall abstractCall = AbstractCall.newData(dataSectionBuilder.build());
      final ConcreteCall concreteCall = new ConcreteCall(abstractCall);

      preparation.add(concreteCall);
    } else {
      final List<ConcreteCall> initializer;

      if (isMemoryMapped) {
        // Memory-mapped buffer.
        final BitVector addressValue = BitVector.valueOf(bufferAccessAddress, Long.SIZE);
        initializer = prepareMemory(engineContext, addressValue, entryValue, sizeInBits);
      } else if (buffer == memory.getTargetBuffer()) {
        final BitVector addressValue = BitVector.valueOf(bufferAccessAddress, Long.SIZE);
        final MemoryAccessStack stack = bufferAccess.getContext().getMemoryAccessStack();

        if (stack.isEmpty()) {
          final Collection<AddressObject> addressObjects = entryObject.getAddrObjects();
          final AddressObject addressObject = addressObjects.iterator().next();

          final MemoryAccess access = addressObject.getAccess();

          final DataType dataType = access.getType().getDataType();
          final DataType entryType = DataType.type(entryValue.getByteSize());
          InvariantChecks.checkTrue(entryType.getSizeInBytes() >= dataType.getSizeInBytes());

          // Main memory.
          final int lower = dataType.getLowerAddressBit();
          final int upper = entryType.getLowerAddressBit() - 1;

          final int offset = lower > upper ? 0 : addressValue.field(lower, upper).intValue();

          final int dataSizeInBits = dataType.getSizeInBytes() << 3;

          final BitVector dataValue =
              entryValue.field(offset * dataSizeInBits, (offset + 1) * dataSizeInBits - 1);

          Logger.debug("Prepare memory: address=%s, data=%s", addressValue, dataValue);
          initializer = prepareMemory(engineContext, addressValue, dataValue, dataSizeInBits);
        } else {
          // Shadow of the memory-mapped buffer access.
          initializer = Collections.<ConcreteCall>emptyList();
        }
      } else {
        // Buffer.
        final BitVector indexValue = BitVector.valueOf(index, Long.SIZE);
        initializer = prepareBuffer(buffer, engineContext, indexValue, entryFieldValues);
      }

      InvariantChecks.checkNotNull(initializer);

      if (!initializer.isEmpty()) {
        preparation.add(ConcreteCall.newLine());
        preparation.add(ConcreteCall.newComment(comment));

        preparation.addAll(initializer);
      }
    }
    //}

    return preparation;
  }

  private List<ConcreteCall> prepareBuffer(
      final MmuBuffer buffer,
      final EngineContext engineContext,
      final BitVector address,
      final Map<String, BitVector> entry) {
    InvariantChecks.checkNotNull(buffer);
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(address);
    InvariantChecks.checkNotNull(entry);

    final BufferPreparatorStore preparators = engineContext.getBufferPreparators();
    InvariantChecks.checkNotNull(preparators, "Preparator store is null");

    final BufferPreparator preparator = preparators.getPreparatorFor(buffer.getName());
    InvariantChecks.checkNotNull(preparator, "Missing preparator for " + buffer.getName());

    final List<AbstractCall> abstractInitializer =
        preparator.makeInitializer(engineContext.getPreparators(), address, entry);
    InvariantChecks.checkNotNull(abstractInitializer, "Abstract initializer is null");

    final List<ConcreteCall> concreteCalls =
        prepareSequence(engineContext, new AbstractSequence(abstractInitializer));

    Logger.debug("Code:");
    for (final ConcreteCall concreteCall : concreteCalls) {
      Logger.debug(concreteCall.getText());
    }
    Logger.debug("");

    return concreteCalls;
  }

  private List<ConcreteCall> prepareMemory(
      final EngineContext engineContext,
      final BitVector address,
      final BitVector data,
      final int sizeInBits) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(address);

    final MemoryPreparatorStore preparators = engineContext.getMemoryPreparators();
    InvariantChecks.checkNotNull(preparators, "Preparator store is null");

    final MemoryPreparator preparator = preparators.getPreparatorFor(sizeInBits);
    InvariantChecks.checkNotNull(preparator, "Missing preparator for " + sizeInBits);

    final List<AbstractCall> abstractInitializer =
        preparator.makeInitializer(engineContext.getPreparators(), address, data);
    InvariantChecks.checkNotNull(abstractInitializer, "Abstract initializer is null");

    final List<ConcreteCall> concreteCalls =
        prepareSequence(engineContext, new AbstractSequence(abstractInitializer));

    Logger.debug("Code:");
    for (final ConcreteCall concreteCall : concreteCalls) {
      Logger.debug(concreteCall.getText());
    }
    Logger.debug("");

    return concreteCalls;
  }

  private List<ConcreteCall> prepareSequence(
      final EngineContext engineContext,
      final AbstractSequence abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    try {
      return EngineUtils.makeConcreteCalls(engineContext, abstractSequence.getSequence());
    } catch (final ConfigurationException e) {
      InvariantChecks.checkTrue(false, e.getMessage());
      return null;
    }
  }

  private List<ConcreteCall> prepareAddresses(
      final EngineContext engineContext,
      final AbstractSequence abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    final List<ConcreteCall> preparation = new ArrayList<>();
    final Set<AddressingModeWrapper> initializedModes = new HashSet<>();

    for (final AbstractCall abstractCall : abstractSequence.getSequence()) {
      if (!MemoryEngine.isSuitable(abstractCall)) {
        continue;
      }

      final AddressObject addressObject = MemoryEngine.getAddressObject(abstractCall);

      final List<ConcreteCall> initializer = prepareAddress(
          engineContext, abstractCall, addressObject, initializedModes);
      InvariantChecks.checkNotNull(initializer);

      Logger.debug("Call preparation: %s", initializer);

      if (!initializer.isEmpty()) {
        preparation.add(ConcreteCall.newLine());
        preparation.add(ConcreteCall.newComment(
            String.format("Initializing Instruction %s:", abstractCall.getText())));

        for (final String comment : getComments(addressObject)) {
          preparation.add(ConcreteCall.newComment(comment));
        }

        preparation.addAll(initializer);
      }
    }

    return preparation;
  }

  private List<ConcreteCall> prepareAddress(
      final EngineContext engineContext,
      final AbstractCall abstractCall,
      final AddressObject addressObject,
      final Set<AddressingModeWrapper> initializedModes) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractCall);
    InvariantChecks.checkNotNull(initializedModes);
    InvariantChecks.checkTrue(MemoryEngine.isSuitable(abstractCall));

    final MmuSubsystem memory = MmuPlugin.getSpecification();

    final Primitive primitive = abstractCall.getRootOperation();
    InvariantChecks.checkNotNull(primitive, "Primitive is null");

    final Situation situation = primitive.getSituation();
    InvariantChecks.checkNotNull(situation, "Situation is null");

    final Map<String, Object> attributes = situation.getAttributes();
    InvariantChecks.checkNotNull(attributes, "Attributes map is null");

    // Specify the situation's parameter (address value).
    final Map<String, Object> newAttributes = new HashMap<>(attributes);
    newAttributes.put(AddressDataGenerator.PARAM_ADDRESS_VALUE,
        addressObject.getAddress(memory.getVirtualAddress()));

    final Situation newSituation = new Situation(situation.getName(), newAttributes);

    try {
      final List<AbstractCall> abstractInitializer = EngineUtils.makeInitializer(
          engineContext, primitive, newSituation, initializedModes);
      InvariantChecks.checkNotNull(abstractInitializer, "Abstract initializer is null");

      return prepareSequence(engineContext, new AbstractSequence(abstractInitializer));
    } catch (final ConfigurationException e) {
      InvariantChecks.checkTrue(false, e.getMessage());
      return null;
    }
  }

  private String getMemoryAccessComment(final AddressObject addressObject) {
    InvariantChecks.checkNotNull(addressObject);

    final MmuSubsystem memory = MmuPlugin.getSpecification();

    final MmuAddressInstance virtualAddressType = memory.getVirtualAddress();
    final BigInteger virtualAddressValue = addressObject.getAddress(virtualAddressType);

    final IntegerVariable dataVariable = memory.getDataVariable();
    final BigInteger dataValue = addressObject.getData(dataVariable);

    return String.format("%s[0x%s]=[0x%s]",
        memory.getName(), virtualAddressValue.toString(16), dataValue.toString(16));
  }

  private String getBufferAccessComment(
      final AddressObject addressObject,
      final MmuBufferAccess bufferAccess) {
    InvariantChecks.checkNotNull(addressObject);
    InvariantChecks.checkNotNull(bufferAccess);

    final StringBuilder builder = new StringBuilder();

    final MmuBuffer buffer = bufferAccess.getBuffer();
    final BufferAccessEvent event = bufferAccess.getEvent();

    final MemoryAccessContext context = bufferAccess.getContext();
    final MemoryAccessStack stack = context.getMemoryAccessStack();

    final MmuAddressInstance addressType = bufferAccess.getAddress();
    final BigInteger addressValue = addressObject.getAddress(addressType);

    for (int i = 0; i <= stack.size(); i++) {
      builder.append("  ");
    }

    // The address may be undefined for buffer writes.
    builder.append(String.format("%-5s %s[%s]", event, buffer.getName(),
        addressValue != null ? String.format("0x%s", addressValue.toString(16)) : "<unknown>"));

    final EntryObject entryObject = addressObject.getEntry(bufferAccess);

    if (entryObject != null) {
      builder.append(String.format("=%s", entryObject.getEntry()));
    }

    return builder.toString();
  }

  private Collection<String> getComments(final AddressObject addressObject) {
    InvariantChecks.checkNotNull(addressObject);

    final Collection<String> comments = new ArrayList<>();
    comments.add(getMemoryAccessComment(addressObject));

    for (final MmuBufferAccess bufferAccess : addressObject.getAccess().getPath().getBufferAccesses()) {
      comments.add(getBufferAccessComment(addressObject, bufferAccess));
    }

    return comments;
  }
}