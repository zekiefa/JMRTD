/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id$
 */

package org.jmrtd.lds;

import static org.jmrtd.DataGroupEnum.EF_COM;
import static org.jmrtd.DataGroupEnum.EF_DG1;
import static org.jmrtd.DataGroupEnum.EF_DG10;
import static org.jmrtd.DataGroupEnum.EF_DG11;
import static org.jmrtd.DataGroupEnum.EF_DG12;
import static org.jmrtd.DataGroupEnum.EF_DG13;
import static org.jmrtd.DataGroupEnum.EF_DG14;
import static org.jmrtd.DataGroupEnum.EF_DG15;
import static org.jmrtd.DataGroupEnum.EF_DG16;
import static org.jmrtd.DataGroupEnum.EF_DG2;
import static org.jmrtd.DataGroupEnum.EF_DG3;
import static org.jmrtd.DataGroupEnum.EF_DG4;
import static org.jmrtd.DataGroupEnum.EF_DG5;
import static org.jmrtd.DataGroupEnum.EF_DG6;
import static org.jmrtd.DataGroupEnum.EF_DG7;
import static org.jmrtd.DataGroupEnum.EF_DG8;
import static org.jmrtd.DataGroupEnum.EF_DG9;
import static org.jmrtd.DataGroupEnum.EF_SOD;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.jmrtd.DataGroupEnum;

/**
 * Static LDS file methods.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class LDSFileUtil {

	/**
	 * Factory method for creating LDS files for a given input stream.
	 *
	 * @param fid file identifier
	 * @param inputStream a given input stream
	 *
	 * @return a specific file
	 *
	 * @throws IOException on reading error from the input stream
	 */
	public static AbstractLDSFile getLDSFile(short fid, InputStream inputStream) throws IOException {
		switch (DataGroupEnum.valueOf(fid)) {
		case EF_COM: return new COMFile(inputStream);
		case EF_DG1: return new DG1File(inputStream);
		case EF_DG2: return new DG2File(inputStream);
		case EF_DG3: return new DG3File(inputStream);
		case EF_DG4: return new DG4File(inputStream);
		case EF_DG5: return new DG5File(inputStream);
		case EF_DG6: return new DG6File(inputStream);
		case EF_DG7: return new DG7File(inputStream);
		case EF_DG8: throw new IllegalArgumentException("DG8 files are not yet supported");
		case EF_DG9: throw new IllegalArgumentException("DG9 files are not yet supported");
		case EF_DG10: throw new IllegalArgumentException("DG10 files are not yet supported");
		case EF_DG11: return new DG11File(inputStream);
		case EF_DG12: return new DG12File(inputStream);
		case EF_DG13: throw new IllegalArgumentException("DG13 files are not yet supported");
		case EF_DG14: return new DG14File(inputStream);
		case EF_DG15: return new DG15File(inputStream);
		case EF_DG16: throw new IllegalArgumentException("DG16 files are not yet supported");
		case EF_SOD: return new SODFile(inputStream);
		case EF_CVCA: return new CVCAFile(inputStream);
		default:
			BufferedInputStream bufferedIn = new BufferedInputStream(inputStream, 37);
			try {
				bufferedIn.mark(37);
				/* Just try, will read 36 bytes at most, and we can reset bufferedIn. */
				return new CVCAFile(fid, bufferedIn);
			} catch (Exception e) {
				bufferedIn.reset();
				throw new NumberFormatException("Unknown file " + Integer.toHexString(fid));   
			}
		}
	}

	/**
	 * Finds a file identifier for an ICAO tag.
	 * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
	 *
	 * @param tag an ICAO tag (the first byte of the EF)
	 *
	 * @return a file identifier.
	 */
	public static short lookupFIDByTag(int tag) {
		switch(tag) {
		case LDSFile.EF_COM_TAG: return EF_COM.getDataGroup();
		case LDSFile.EF_DG1_TAG: return EF_DG1.getDataGroup();
		case LDSFile.EF_DG2_TAG: return EF_DG2.getDataGroup();
		case LDSFile.EF_DG3_TAG: return EF_DG3.getDataGroup();
		case LDSFile.EF_DG4_TAG: return EF_DG4.getDataGroup();
		case LDSFile.EF_DG5_TAG: return EF_DG5.getDataGroup();
		case LDSFile.EF_DG6_TAG: return EF_DG6.getDataGroup();
		case LDSFile.EF_DG7_TAG: return EF_DG7.getDataGroup();
		case LDSFile.EF_DG8_TAG: return EF_DG8.getDataGroup();
		case LDSFile.EF_DG9_TAG: return EF_DG9.getDataGroup();
		case LDSFile.EF_DG10_TAG: return EF_DG10.getDataGroup();
		case LDSFile.EF_DG11_TAG: return EF_DG11.getDataGroup();
		case LDSFile.EF_DG12_TAG: return EF_DG12.getDataGroup();
		case LDSFile.EF_DG13_TAG: return EF_DG13.getDataGroup();
		case LDSFile.EF_DG14_TAG: return EF_DG14.getDataGroup();
		case LDSFile.EF_DG15_TAG: return EF_DG15.getDataGroup();
		case LDSFile.EF_DG16_TAG: return EF_DG16.getDataGroup();
		case LDSFile.EF_SOD_TAG: return EF_SOD.getDataGroup();
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));
		}
	}

	/**
	 * Finds a data group number for an ICAO tag.
	 * 
	 * @param tag an ICAO tag (the first byte of the EF)
	 * 
	 * @return a data group number (1-16)
	 */
	public static int lookupDataGroupNumberByTag(int tag) {
		switch (tag) {
		case LDSFile.EF_DG1_TAG: return 1;
		case LDSFile.EF_DG2_TAG: return 2;
		case LDSFile.EF_DG3_TAG: return 3;
		case LDSFile.EF_DG4_TAG: return 4;
		case LDSFile.EF_DG5_TAG: return 5;
		case LDSFile.EF_DG6_TAG: return 6;
		case LDSFile.EF_DG7_TAG: return 7;
		case LDSFile.EF_DG8_TAG: return 8;
		case LDSFile.EF_DG9_TAG: return 9;
		case LDSFile.EF_DG10_TAG: return 10;
		case LDSFile.EF_DG11_TAG: return 11;
		case LDSFile.EF_DG12_TAG: return 12;
		case LDSFile.EF_DG13_TAG: return 13;
		case LDSFile.EF_DG14_TAG: return 14;
		case LDSFile.EF_DG15_TAG: return 15;
		case LDSFile.EF_DG16_TAG: return 16;
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));   
		}
	}

	/**
	 * Finds an ICAO tag for a data group number.
	 *
	 * @param number a data group number (1-16)
	 *
	 * @return an ICAO tag (the first byte of the EF)
	 */
	public static int lookupTagByDataGroupNumber(int number) {
		switch (number) {
		case 1: return LDSFile.EF_DG1_TAG;
		case 2: return LDSFile.EF_DG2_TAG;
		case 3: return LDSFile.EF_DG3_TAG;
		case 4: return LDSFile.EF_DG4_TAG;
		case 5: return LDSFile.EF_DG5_TAG;
		case 6: return LDSFile.EF_DG6_TAG;
		case 7: return LDSFile.EF_DG7_TAG;
		case 8: return LDSFile.EF_DG8_TAG;
		case 9: return LDSFile.EF_DG9_TAG;
		case 10: return LDSFile.EF_DG10_TAG;
		case 11: return LDSFile.EF_DG11_TAG;
		case 12: return LDSFile.EF_DG12_TAG;
		case 13: return LDSFile.EF_DG13_TAG;
		case 14: return LDSFile.EF_DG14_TAG;
		case 15: return LDSFile.EF_DG15_TAG;
		case 16: return LDSFile.EF_DG16_TAG;
		default:
			throw new NumberFormatException("Unknown number " + number);   
		}
	}

	/**
	 * Finds an ICAO tag for a data group number.
	 *
	 * @param number a data group number (1-16)
	 *
	 * @return a file identifier
	 */
	public static short lookupFIDByDataGroupNumber(int number) {
		switch (number) {
		case 1: return EF_DG1.getDataGroup();
		case 2: return EF_DG2.getDataGroup();
		case 3: return EF_DG3.getDataGroup();
		case 4: return EF_DG4.getDataGroup();
		case 5: return EF_DG5.getDataGroup();
		case 6: return EF_DG6.getDataGroup();
		case 7: return EF_DG7.getDataGroup();
		case 8: return EF_DG8.getDataGroup();
		case 9: return EF_DG9.getDataGroup();
		case 10: return EF_DG10.getDataGroup();
		case 11: return EF_DG11.getDataGroup();
		case 12: return EF_DG12.getDataGroup();
		case 13: return EF_DG13.getDataGroup();
		case 14: return EF_DG14.getDataGroup();
		case 15: return EF_DG15.getDataGroup();
		case 16: return EF_DG16.getDataGroup();
		default:
			throw new NumberFormatException("Unknown number " + number);   
		}
	}

	/**
	 * Finds an ICAO tag for a file identifier.
	 * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
	 *
	 * @param fid a file identifier
	 *
	 * @return a an ICAO tag (first byte of EF)
	 */
	public static short lookupTagByFID(short fid) {
		switch(Objects.requireNonNull(DataGroupEnum.valueOf(fid))) {
		case EF_COM: return LDSFile.EF_COM_TAG;
		case EF_DG1: return LDSFile.EF_DG1_TAG;
		case EF_DG2: return LDSFile.EF_DG2_TAG;
		case EF_DG3: return LDSFile.EF_DG3_TAG;
		case EF_DG4: return LDSFile.EF_DG4_TAG;
		case EF_DG5: return LDSFile.EF_DG5_TAG;
		case EF_DG6: return LDSFile.EF_DG6_TAG;
		case EF_DG7: return LDSFile.EF_DG7_TAG;
		case EF_DG8: return LDSFile.EF_DG8_TAG;
		case EF_DG9: return LDSFile.EF_DG9_TAG;
		case EF_DG10: return LDSFile.EF_DG10_TAG;
		case EF_DG11: return LDSFile.EF_DG11_TAG;
		case EF_DG12: return LDSFile.EF_DG12_TAG;
		case EF_DG13: return LDSFile.EF_DG13_TAG;
		case EF_DG14: return LDSFile.EF_DG14_TAG;
		case EF_DG15: return LDSFile.EF_DG15_TAG;
		case EF_DG16: return LDSFile.EF_DG16_TAG;
		case EF_SOD: return LDSFile.EF_SOD_TAG;
		default:
			throw new NumberFormatException("Unknown fid " + Integer.toHexString(fid));
		}
	}

	/**
	 * Finds a data group number by file identifier.
	 * 
	 * @param fid a file id
	 * 
	 * @return a data group number
	 */
	public static short lookupDataGroupNumberByFID(short fid) {
		switch(Objects.requireNonNull(DataGroupEnum.valueOf(fid))) {
		case EF_DG1: return 1;
		case EF_DG2: return 2;
		case EF_DG3: return 3;
		case EF_DG4: return 4;
		case EF_DG5: return 5;
		case EF_DG6: return 6;
		case EF_DG7: return 7;
		case EF_DG8: return 8;
		case EF_DG9: return 9;
		case EF_DG10: return 10;
		case EF_DG11: return 11;
		case EF_DG12: return 12;
		case EF_DG13: return 13;
		case EF_DG14: return 14;
		case EF_DG15: return 15;
		case EF_DG16: return 16;
		default:
			throw new NumberFormatException("Unknown fid " + Integer.toHexString(fid));
		}
	}

	/**
	 * Returns a mnemonic name corresponding to the file represented by the
	 * given ICAO tag, such as "EF_COM", "EF_SOD", or "EF_DG1".
	 *
	 * @param tag an ICAO tag (the first byte of the EF)
	 *
	 * @return a mnemonic name corresponding to the file represented by the given ICAO tag
	 */
	public static String lookupFileNameByTag(int tag) {
		switch (tag) {
		case LDSFile.EF_COM_TAG: return "EF_COM";
		case LDSFile.EF_DG1_TAG: return "EF_DG1";
		case LDSFile.EF_DG2_TAG: return "EF_DG2";
		case LDSFile.EF_DG3_TAG: return "EF_DG3";
		case LDSFile.EF_DG4_TAG: return "EF_DG4";
		case LDSFile.EF_DG5_TAG: return "EF_DG5";
		case LDSFile.EF_DG6_TAG: return "EF_DG6";
		case LDSFile.EF_DG7_TAG: return "EF_DG7";
		case LDSFile.EF_DG8_TAG: return "EF_DG8";
		case LDSFile.EF_DG9_TAG: return "EF_DG9";
		case LDSFile.EF_DG10_TAG: return "EF_DG10";
		case LDSFile.EF_DG11_TAG: return "EF_DG11";
		case LDSFile.EF_DG12_TAG: return "EF_DG12";
		case LDSFile.EF_DG13_TAG: return "EF_DG13";
		case LDSFile.EF_DG14_TAG: return "EF_DG14";
		case LDSFile.EF_DG15_TAG: return "EF_DG15";
		case LDSFile.EF_DG16_TAG: return "EF_DG16";
		case LDSFile.EF_SOD_TAG: return "EF_SOD";
		default: return "File with tag 0x" + Integer.toHexString(tag);
		}
	}

	/**
	 * Returns a mnemonic name corresponding to the file represented by the
	 * given file identifier, such as "EF_COM", "EF_SOD", or "EF_DG1".
	 * 
	 * @param fid an LDS file identifiers
	 * 
	 * @return a mnemonic name corresponding to the file represented by the given ICAO tag
	 */
	public static String lookupFileNameByFID(int fid) {
		switch (Objects.requireNonNull(DataGroupEnum.valueOf(fid))) {
		case EF_COM: return "EF_COM";
		case EF_DG1: return "EF_DG1";
		case EF_DG2: return "EF_DG2";
		case EF_DG3: return "EF_DG3";
		case EF_DG4: return "EF_DG4";
		case EF_DG5: return "EF_DG5";
		case EF_DG6: return "EF_DG6";
		case EF_DG7: return "EF_DG7";
		case EF_DG8: return "EF_DG8";
		case EF_DG9: return "EF_DG9";
		case EF_DG10: return "EF_DG10";
		case EF_DG11: return "EF_DG11";
		case EF_DG12: return "EF_DG12";
		case EF_DG13: return "EF_DG13";
		case EF_DG14: return "EF_DG14";
		case EF_DG15: return "EF_DG15";
		case EF_DG16: return "EF_DG16";
		case EF_SOD: return "EF_SOD";
		default: return "File with FID 0x" + Integer.toHexString(fid);
		}
	}
}
