package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DG4FileTest {

	private List<IrisInfo> sampleIrisInfos() {
		List<IrisInfo> irisInfos = new ArrayList<IrisInfo>();
		irisInfos.add(LDSTestHelper.createTestIrisInfo());
		return irisInfos;
	}

	@Test
	public void testConstruct() {
		DG4File dg4 = new DG4File(sampleIrisInfos());
		assertEquals(LDSFile.EF_DG4_TAG, dg4.getTag());
		assertEquals(1, dg4.getIrisInfos().size());
		assertTrue(dg4.toString().startsWith("DG4File"));
	}

	@Test
	public void testRoundTrip() throws Exception {
		DG4File dg4 = new DG4File(sampleIrisInfos());
		byte[] encoded = dg4.getEncoded();
		DG4File copy = new DG4File(new ByteArrayInputStream(encoded));
		assertEquals(dg4, copy);
		assertEquals(dg4.hashCode(), copy.hashCode());
		assertEquals(1, copy.getIrisInfos().size());
	}

	@Test
	public void testAddRemove() {
		DG4File dg4 = new DG4File(new ArrayList<IrisInfo>(), false);
		dg4.addIrisInfo(LDSTestHelper.createTestIrisInfo());
		assertEquals(1, dg4.getIrisInfos().size());
		dg4.removeIrisInfo(0);
		assertEquals(0, dg4.getIrisInfos().size());
	}

	@Test
	public void testEmptyEncodesRandomData() throws Exception {
		DG4File withRandom = new DG4File(new ArrayList<IrisInfo>(), true);
		DG4File withoutRandom = new DG4File(new ArrayList<IrisInfo>(), false);
		assertTrue(withRandom.getLength() > withoutRandom.getLength());
	}

	@Test
	public void testEmptyRoundTrip() throws Exception {
		DG4File empty = new DG4File(new ArrayList<IrisInfo>(), true);
		DG4File copy = new DG4File(new ByteArrayInputStream(empty.getEncoded()));
		assertEquals(0, copy.getIrisInfos().size());
	}

	@Test
	public void testEquals() {
		DG4File dg4 = new DG4File(sampleIrisInfos());
		assertNotEquals(dg4, null);
		assertNotEquals(dg4, new Object());
		assertEquals(dg4, new DG4File(sampleIrisInfos()));
	}
}
