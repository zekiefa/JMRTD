package org.jmrtd.cbeff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;

public class StandardBiometricHeaderTest {

	@Test
	public void testConstructorAndGetElements() {
		Map<Integer, byte[]> elements = new HashMap<Integer, byte[]>();
		elements.put(0x88, new byte[] { 0x00, 0x07 });
		elements.put(0x81, new byte[] { 0x08 });
		elements.put(0x87, new byte[] { 0x01, 0x01 });

		StandardBiometricHeader sbh = new StandardBiometricHeader(elements);
		SortedMap<Integer, byte[]> result = sbh.getElements();

		assertEquals(3, result.size());
		/* Sorted by tag. */
		assertEquals(Integer.valueOf(0x81), result.firstKey());
		assertEquals(Integer.valueOf(0x88), result.lastKey());
		assertArrayEquals(new byte[] { 0x08 }, result.get(0x81));
		assertArrayEquals(new byte[] { 0x01, 0x01 }, result.get(0x87));
		assertArrayEquals(new byte[] { 0x00, 0x07 }, result.get(0x88));
	}

	@Test
	public void testGetElementsReturnsCopy() {
		Map<Integer, byte[]> elements = new HashMap<Integer, byte[]>();
		elements.put(0x81, new byte[] { 0x08 });
		StandardBiometricHeader sbh = new StandardBiometricHeader(elements);

		SortedMap<Integer, byte[]> copy = sbh.getElements();
		copy.put(0x82, new byte[] { 0x00 });
		assertEquals(1, sbh.getElements().size());
	}

	@Test
	public void testEmptyHeader() {
		StandardBiometricHeader sbh = new StandardBiometricHeader(new HashMap<Integer, byte[]>());
		assertTrue(sbh.getElements().isEmpty());
	}
}
