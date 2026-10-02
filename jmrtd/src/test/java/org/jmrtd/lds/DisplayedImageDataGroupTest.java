package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.scuba.tlv.TLVOutputStream;

import org.junit.jupiter.api.Test;

/**
 * Tests DG5File, DG6File, DG7File and the shared DisplayedImageDataGroup
 * logic. Also covers DataGroup read/write plumbing via subclasses.
 */
public class DisplayedImageDataGroupTest {

	private static final byte[] IMAGE_BYTES = { 1, 2, 3, 4, 5, 6, 7 };

	private static List<DisplayedImageInfo> portraitInfos() {
		List<DisplayedImageInfo> infos = new ArrayList<DisplayedImageInfo>();
		infos.add(new DisplayedImageInfo(ImageInfo.TYPE_PORTRAIT, IMAGE_BYTES));
		return infos;
	}

	private static List<DisplayedImageInfo> signatureInfos() {
		List<DisplayedImageInfo> infos = new ArrayList<DisplayedImageInfo>();
		infos.add(new DisplayedImageInfo(ImageInfo.TYPE_SIGNATURE_OR_MARK, IMAGE_BYTES));
		return infos;
	}

	@Test
	public void testDG5Constructors() throws Exception {
		DG5File dg5 = new DG5File(portraitInfos());
		assertEquals(LDSFile.EF_DG5_TAG, dg5.getTag());
		assertEquals(1, dg5.getImages().size());
		assertTrue(dg5.toString().startsWith("DG5File"));

		DG5File copy = new DG5File(new ByteArrayInputStream(dg5.getEncoded()));
		assertEquals(dg5, copy);
		assertEquals(dg5.hashCode(), copy.hashCode());
		assertEquals(IMAGE_BYTES.length, copy.getImages().get(0).getImageLength());
		byte[] readBack = new byte[copy.getImages().get(0).getImageLength()];
		copy.getImages().get(0).getImageInputStream().read(readBack);
		assertArrayEquals(IMAGE_BYTES, readBack);
	}

	@Test
	public void testDG7Constructors() throws Exception {
		DG7File dg7 = new DG7File(signatureInfos());
		assertEquals(LDSFile.EF_DG7_TAG, dg7.getTag());
		assertEquals(1, dg7.getImages().size());
		assertTrue(dg7.toString().startsWith("DG7File"));

		DG7File copy = new DG7File(new ByteArrayInputStream(dg7.getEncoded()));
		assertEquals(dg7, copy);
		assertEquals(dg7.hashCode(), copy.hashCode());
	}

	@Test
	public void testDG6Constructor() throws Exception {
		/* DG6 has no constructing-from-list constructor; build bytes by patching a DG6-like structure. */
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(0x02);
		tlvOut.writeValue(new byte[] { 1 });
		DisplayedImageInfo info = new DisplayedImageInfo(ImageInfo.TYPE_SIGNATURE_OR_MARK, IMAGE_BYTES);
		info.writeObject(tlvOut);
		tlvOut.flush();
		byte[] value = content.toByteArray();

		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG6_TAG);
		fullTLV.writeValue(value);
		fullTLV.flush();

		DG6File dg6 = new DG6File(new ByteArrayInputStream(full.toByteArray()));
		assertEquals(LDSFile.EF_DG6_TAG, dg6.getTag());
		assertEquals(1, dg6.getImages().size());
		assertTrue(dg6.toString().startsWith("DG6File"));

		DG6File copy = new DG6File(new ByteArrayInputStream(dg6.getEncoded()));
		assertEquals(dg6, copy);
	}

	@Test
	public void testTypesConsistentWithTag() {
		assertThrows(IllegalArgumentException.class, () -> new DG5File(signatureInfos()));
		assertThrows(IllegalArgumentException.class, () -> new DG7File(portraitInfos()));
	}

	@Test
	public void testNullImageInfos() {
		assertThrows(IllegalArgumentException.class, () -> new DG5File((java.util.List<DisplayedImageInfo>) null));
	}

	@Test
	public void testReadWrongCountTag() {
		byte[] bad = new byte[] { (byte) LDSFile.EF_DG5_TAG, 0x03, 0x03, 0x01, 0x01 };
		ByteArrayInputStream in = new ByteArrayInputStream(bad);
		assertThrows(Exception.class, () -> new DG5File(in));
	}

	@Test
	public void testReadWrongCountLength() {
		byte[] bad = new byte[] { (byte) LDSFile.EF_DG5_TAG, 0x04, 0x02, 0x02, 0x01, 0x01 };
		ByteArrayInputStream in = new ByteArrayInputStream(bad);
		assertThrows(Exception.class, () -> new DG5File(in));
	}

	@Test
	public void testMixedDisplayedImageTagsInStream() throws Exception {
		/* Two images with different displayed image tags inside one DG. */
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(0x02);
		tlvOut.writeValue(new byte[] { 2 });
		new DisplayedImageInfo(ImageInfo.TYPE_PORTRAIT, IMAGE_BYTES).writeObject(tlvOut);
		new DisplayedImageInfo(ImageInfo.TYPE_SIGNATURE_OR_MARK, IMAGE_BYTES).writeObject(tlvOut);
		tlvOut.flush();
		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG5_TAG);
		fullTLV.writeValue(content.toByteArray());
		fullTLV.flush();
		assertThrows(Exception.class, () -> new DG5File(new ByteArrayInputStream(full.toByteArray())));
	}

	@Test
	public void testWrongDataGroupTag() {
		DG5File dg5 = new DG5File(portraitInfos());
		/* read as DG7: tag mismatch */
		assertThrows(Exception.class, () -> new DG7File(new ByteArrayInputStream(dg5.getEncoded())));
	}

	@Test
	public void testMultipleImages() throws Exception {
		List<DisplayedImageInfo> infos = new ArrayList<DisplayedImageInfo>();
		infos.add(new DisplayedImageInfo(ImageInfo.TYPE_PORTRAIT, IMAGE_BYTES));
		infos.add(new DisplayedImageInfo(ImageInfo.TYPE_PORTRAIT, new byte[] { 9 }));
		DG5File dg5 = new DG5File(infos);
		DG5File copy = new DG5File(new ByteArrayInputStream(dg5.getEncoded()));
		assertEquals(2, copy.getImages().size());
		assertEquals(dg5, copy);
		assertTrue(dg5.toString().contains(","));
	}

	@Test
	public void testEqualsEdgeCases() {
		DG5File dg5 = new DG5File(portraitInfos());
		assertEquals(dg5, dg5);
		assertNotEquals(dg5, null);
		assertNotEquals(dg5, new Object());
		assertNotEquals(dg5, new DG5File(Arrays.asList(new DisplayedImageInfo(ImageInfo.TYPE_PORTRAIT, new byte[] { 1 }))));
		getExtras(dg5);
	}

	private static void getExtras(DG5File dg5) {
		assertTrue(dg5.getImages() != null);
	}
}
