package org.jmrtd.cbeff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.jmrtd.cbeff.CBEFFTestUtil.TestBiometricDataBlock;
import org.junit.jupiter.api.Test;

public class CBEFFInfoTest {

	@Test
	public void testSimpleCBEFFInfo() {
		TestBiometricDataBlock bdb = new CBEFFTestUtil.TestBiometricDataBlock(
				CBEFFTestUtil.createSBH(), new byte[] { 1, 2, 3 });
		SimpleCBEFFInfo<TestBiometricDataBlock> info = new SimpleCBEFFInfo<TestBiometricDataBlock>(bdb);
		assertSame(bdb, info.getBiometricDataBlock());
	}

	@Test
	public void testComplexCBEFFInfoAddAndGet() {
		ComplexCBEFFInfo complex = new ComplexCBEFFInfo();
		assertTrue(complex.getSubRecords().isEmpty());

		SimpleCBEFFInfo<BiometricDataBlock> r1 = new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1 }));
		SimpleCBEFFInfo<BiometricDataBlock> r2 = new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 2 }));

		complex.add(r1);
		List<CBEFFInfo> list = Arrays.<CBEFFInfo>asList(r2);
		complex.addAll(list);

		assertEquals(2, complex.getSubRecords().size());
		assertSame(r1, complex.getSubRecords().get(0));
		assertSame(r2, complex.getSubRecords().get(1));
	}

	@Test
	public void testComplexCBEFFInfoRemove() {
		ComplexCBEFFInfo complex = new ComplexCBEFFInfo();
		SimpleCBEFFInfo<BiometricDataBlock> r1 = new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1 }));
		SimpleCBEFFInfo<BiometricDataBlock> r2 = new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 2 }));
		complex.add(r1);
		complex.add(r2);

		complex.remove(0);
		assertEquals(1, complex.getSubRecords().size());
		assertSame(r2, complex.getSubRecords().get(0));
	}

	@Test
	public void testComplexCBEFFInfoRemoveOnEmpty() {
		ComplexCBEFFInfo complex = new ComplexCBEFFInfo();
		assertThrows(IndexOutOfBoundsException.class, () -> complex.remove(0));
	}

	@Test
	public void testComplexCBEFFInfoEqualsAndHashCode() {
		SimpleCBEFFInfo<BiometricDataBlock> shared = new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1 }));

		ComplexCBEFFInfo c1 = new ComplexCBEFFInfo();
		c1.add(shared);

		ComplexCBEFFInfo c2 = new ComplexCBEFFInfo();
		c2.add(shared);

		ComplexCBEFFInfo c3 = new ComplexCBEFFInfo();

		assertEquals(c1, c1);
		assertEquals(c1, c2);
		assertEquals(c1.hashCode(), c2.hashCode());
		assertNotEquals(c1, null);
		assertNotEquals(c1, "notACBEFFInfo");
		assertNotEquals(c1, c3);

		/* SimpleCBEFFInfo uses identity equality (no equals override). */
		ComplexCBEFFInfo c4 = new ComplexCBEFFInfo();
		c4.add(new SimpleCBEFFInfo<BiometricDataBlock>(
				new CBEFFTestUtil.TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1 })));
		assertNotEquals(c1, c4);
	}

	@Test
	public void testComplexCBEFFInfoEqualsEmpty() {
		ComplexCBEFFInfo c1 = new ComplexCBEFFInfo();
		ComplexCBEFFInfo c2 = new ComplexCBEFFInfo();
		assertEquals(c1, c2);
		assertEquals(c1.hashCode(), c2.hashCode());
	}
}
