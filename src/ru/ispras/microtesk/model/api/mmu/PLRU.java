package ru.ispras.microtesk.model.api.mmu;

public class PLRU extends Policy
{
    public int bits;

    public PLRU(int associativity)
    {
        super(associativity);

        assert associativity > 0;
        assert associativity <= Integer.SIZE - 1;

        resetBits();
    }

    private void resetBits()
    {
        this.bits = 0;
    }

    /**
     * If bit accesses to i_th cell, then i_th bit is set to 1.
     * 
     * If all bits equal to 1, then all of them are set to zero.
     * 
     */

    private void setBit(int bitIndex)
    {
        bits |= (1 << bitIndex);

        if (bits == ((1 << getAssociativity()) - 1))
            resetBits();
    }

    public void accessLine(int index)
    {
        setBit(index);
    }

    /**
     * If miss happened the index of first nonzero bit is look for.
     */

    public int choseVictim()
    {
        for (int index = 0; index < getAssociativity(); ++index)
        {
            if ((bits & (1 << index)) == 0) 
            {
                setBit(index);
                return index;
            }
        }

        assert false : "Incorrect state: all bits are set to 1";
        return -1;
    }
}
