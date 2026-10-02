package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DG3FileTest {

	private List<FingerInfo> sampleFingerInfos() {
		List<FingerInfo> fingerInfos = new ArrayList<FingerInfo>();
		fingerInfos.add(LDSTestHelper.createTestFingerInfo());
		return fingerInfos;
	}

	@Test
	public void testConstruct() {
		DG3File dg3 = new DG3File(sampleFingerInfos());
		assertEquals(LDSFile.EF_DG3_TAG, dg3.getTag());
		assertEquals(1, dg3.getFingerInfos().size());
		assertTrue(dg3.toString().startsWith("DG3File"));
	}

	@Test
	public void testRoundTrip() throws Exception {
		DG3File dg3 = new DG3File(sampleFingerInfos());
		byte[] encoded = dg3.getEncoded();
		DG3File copy = new DG3File(new ByteArrayInputStream(encoded));
		assertEquals(dg3, copy);
		assertEquals(dg3.hashCode(), copy.hashCode());
		assertEquals(1, copy.getFingerInfos().size());
	}

	@Test
	public void testAddRemove() {
		DG3File dg3 = new DG3File(new ArrayList<FingerInfo>(), false);
		dg3.addFingerInfo(LDSTestHelper.createTestFingerInfo());
		assertEquals(1, dg3.getFingerInfos().size());
		dg3.removeFingerInfo(0);
		assertEquals(0, dg3.getFingerInfos().size());
	}

	@Test
	public void testEmptyEncodesRandomData() throws Exception {
		/* With shouldAddRandomDataIfEmpty: content contains discretionary data tag. */
		DG3File withRandom = new DG3File(new ArrayList<FingerInfo>(), true);
		DG3File withoutRandom = new DG3File(new ArrayList<FingerInfo>(), false);
		assertTrue(withRandom.getLength() > withoutRandom.getLength());
		assertEquals(withRandom, new DG3File(new ArrayList<FingerInfo>(), false));
	}

	@Test
	public void testEmptyRoundTrip() throws Exception {
		DG3File empty = new DG3File(new ArrayList<FingerInfo>(), true);
		DG3File copy = new DG3File(new ByteArrayInputStream(empty.getEncoded()));
		assertEquals(0, copy.getFingerInfos().size());
	}

	@Test
	public void testEquals() {
		DG3File dg3 = new DG3File(sampleFingerInfos());
		assertNotEquals(dg3, null);
		assertNotEquals(dg3, new Object());
		assertEquals(dg3, new DG3File(sampleFingerInfos()));
	}
}
