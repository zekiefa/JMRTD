package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class COMFileTest {

	private static final int[] TAGS = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG };

	private COMFile createSample() {
		return new COMFile("1.7", "4.0.0", new int[] { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG });
	}

	@Test
	public void testConstructorsAndAccessors() {
		COMFile com = createSample();
		assertEquals("1.7", com.getLDSVersion());
		assertEquals("4.0.0", com.getUnicodeVersion());
		assertArrayEquals(TAGS, com.getTagList());
		assertTrue(com.toString().startsWith("COMFile"));
		assertTrue(com.toString().contains("DG1"));
		assertTrue(com.toString().contains("DG2"));
	}

	@Test
	public void testFullFieldConstructor() {
		COMFile com = new COMFile("01", "07", "04", "00", "00", new int[] { 0x61 });
		assertEquals("1.7", com.getLDSVersion());
		assertEquals("4.0.0", com.getUnicodeVersion());
		assertArrayEquals(new int[] { 0x61 }, com.getTagList());
	}

	@Test
	public void testNonNumericVersionRoundTrip() throws Exception {
		/* Not exactly spec, but versions are only checked for length. */
		COMFile com = new COMFile("AB", "CD", "EF", "GH", "IJ", new int[] { 0x61 });
		assertEquals("AB.CD", com.getLDSVersion());
		assertEquals("EF.GH.IJ", com.getUnicodeVersion());
		COMFile copy = new COMFile(new ByteArrayInputStream(com.getEncoded()));
		assertEquals(com, copy);
	}

	@Test
	public void testConstructorFailures() {
		assertThrows(IllegalArgumentException.class, () -> new COMFile(null, "4.0.0", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1.7", null, TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1", "4.0.0", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1.7.1", "4.0.0", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1.7", "4.0", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("x.y", "4.0.0", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1.7", "a.b.c", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1.7", "4.0.0", null));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("1", "07", "04", "00", "00", TAGS));
		assertThrows(IllegalArgumentException.class, () -> new COMFile("01", "07", "04", "00", "00", null));
	}

	@Test
	public void testInsertTag() {
		COMFile com = createSample();
		com.insertTag(LDSFile.EF_DG11_TAG);
		int[] tagList = com.getTagList();
		assertEquals(3, tagList.length);
		assertTrue(tagList[0] <= tagList[1]);
		assertArrayEquals(new int[] { LDSFile.EF_DG1_TAG, LDSFile.EF_DG11_TAG, LDSFile.EF_DG2_TAG }, tagList);
		/* inserting existing tag should be a no-op */
		com.insertTag(LDSFile.EF_DG11_TAG);
		assertEquals(3, com.getTagList().length);
	}

	@Test
	public void testRoundTrip() throws Exception {
		COMFile com = createSample();
		byte[] encoded = com.getEncoded();
		assertNotNull(encoded);
		COMFile copy = new COMFile(new ByteArrayInputStream(encoded));
		assertEquals(com, copy);
		assertEquals(com.hashCode(), copy.hashCode());
		assertEquals(com.getTag(), copy.getTag());
		assertEquals(com.getLength(), copy.getLength());
		assertTrue(copy.toString().contains("DG"));
	}

	@Test
	public void testReadErrors() {
		/* Wrong first tag. */
		byte[] bad = new byte[] { 0x60, 0x05, 0x5C, 0x01, 0x61, 0x00, 0x00 };
		assertThrows(Exception.class, () -> new COMFile(new ByteArrayInputStream(bad)));
		/* Truncated. */
		byte[] truncated = new byte[] { 0x60, 0x01 };
		assertThrows(Exception.class, () -> new COMFile(new ByteArrayInputStream(truncated)));
	}

	@Test
	public void testEqualsHashCode() {
		COMFile com = createSample();
		COMFile same = createSample();
		COMFile different = new COMFile("1.8", "4.0.0", TAGS);
		assertEquals(com, com);
		assertEquals(com, same);
		assertNotEquals(com, different);
		assertNotEquals(com, null);
		assertNotEquals(com, new Object());
		assertEquals(com.hashCode(), same.hashCode());
	}
}
