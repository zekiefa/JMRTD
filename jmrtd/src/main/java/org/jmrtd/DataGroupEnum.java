package org.jmrtd;

public enum DataGroupEnum {
    /** Data group 1 contains the MRZ. */
    EF_DG1((short)0x0101),
    /** Data group 2 contains face image data. */
    EF_DG2((short)0x0102),
    /** Data group 3 contains finger print data. */
    EF_DG3((short)0x0103),
    /** Data group 4 contains iris data. */
    EF_DG4((short)0x0104),
    /** Data group 5 contains displayed portrait. */
    EF_DG5((short)0x0105),
    /** Data group 6 is RFU. */
    EF_DG6((short)0x0106),
    /** Data group 7 contains displayed signature. */
    EF_DG7((short)0x0107),
    /** Data group 8 contains data features. */
    EF_DG8((short)0x0108),
    /** Data group 9 contains structure features. */
    EF_DG9((short)0x0109),
    /** Data group 10 contains substance features. */
    EF_DG10((short)0x010A),
    /** Data group 11 contains additional personal details. */
    EF_DG11((short)0x010B),
    /** Data group 12 contains additional document details. */
    EF_DG12((short)0x010C),
    /** Data group 13 contains optional details. */
    EF_DG13((short)0x010D),
    /** Data group 14 is RFU. */
    EF_DG14((short)0x010E),
    /** Data group 15 contains the public key used for Active Authentication. */
    EF_DG15((short)0x010F),
    /** Data group 16 contains person(s) to notify. */
    EF_DG16((short)0x0110F),
    EF_CARD_ACCESS((short)0x011C),
    /** The security document. */
    EF_SOD((short)0x011D),
    /** The data group presence list. */
    EF_COM((short)0x011E),
    /**
     * File with the EAC CVCA references. Note: this can be overridden by a file
     * identifier in the DG14 file (TerminalAuthenticationInfo). So check that
     * one first. Also, this file does not have a header tag, like the others.
     */
    EF_CVCA((short)0x011C);

    DataGroupEnum(final short dataGroup) {
        this.dataGroup = dataGroup;
    }

    private final short dataGroup;

    public short getDataGroup() {
        return dataGroup;
    }

    public static DataGroupEnum valueOf(final short dataGroup) {
        for (DataGroupEnum dg :  values() ) {
            if (dg.getDataGroup() == dataGroup)
                return dg;
        }

        return null;
    }

    public static DataGroupEnum valueOf(final int dataGroup) {
        for (DataGroupEnum dg :  values() ) {
            if (dg.getDataGroup() == (short)dataGroup)
                return dg;
        }

        return null;
    }
}
