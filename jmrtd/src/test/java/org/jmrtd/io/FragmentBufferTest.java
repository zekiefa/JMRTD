package org.jmrtd.io;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import org.jmrtd.io.FragmentBuffer.Fragment;
import org.junit.jupiter.api.Test;

class FragmentBufferTest {

	@Test
	void testDefaultConstructor() {
		FragmentBuffer fb = new FragmentBuffer();
		assertEquals(1024, fb.getLength());
		assertEquals(0, fb.getBytesBuffered());
		assertEquals(0, fb.getPosition());
	}

	@Test
	void testSizedConstructor() {
		FragmentBuffer fb = new FragmentBuffer(16);
		assertEquals(16, fb.getLength());
	}

	@Test
	void testAddFragmentArray() {
		FragmentBuffer fb = new FragmentBuffer(8);
		fb.addFragment(2, new byte[] { 1, 2, 3 });
		assertTrue(fb.isCoveredByFragment(2));
		assertTrue(fb.isCoveredByFragment(2, 3));
		assertFalse(fb.isCoveredByFragment(1));
		assertFalse(fb.isCoveredByFragment(2, 4));
		assertEquals(3, fb.getBytesBuffered());
		assertEquals(5, fb.getPosition());
		byte[] buf = fb.getBuffer();
		assertEquals(1, buf[2]);
		assertEquals(3, buf[4]);
	}

	@Test
	void testAddFragmentByteAndAutoGrow() {
		FragmentBuffer fb = new FragmentBuffer(4);
		fb.addFragment(10, (byte) 0xAB);
		assertTrue(fb.getLength() >= 11);
		assertTrue(fb.isCoveredByFragment(10));
		assertEquals((byte) 0xAB, fb.getBuffer()[10]);
	}

	@Test
	void testAddFragmentContainedInOther() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(2, new byte[] { 1, 2, 3, 4, 5 });
		fb.addFragment(3, new byte[] { 9, 9 }); // fully contained
		assertEquals(1, fb.getFragments().size());
		assertEquals(5, fb.getBytesBuffered());
	}

	@Test
	void testAddFragmentRightOverlap() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(0, new byte[] { 1, 2, 3, 4 });
		fb.addFragment(3, new byte[] { 5, 6, 7 });
		assertEquals(1, fb.getFragments().size());
		assertEquals(6, fb.getBytesBuffered());
		assertEquals(0, fb.getFragments().iterator().next().getOffset());
	}

	@Test
	void testAddFragmentLeftOverlap() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(4, new byte[] { 1, 2, 3, 4 });
		fb.addFragment(2, new byte[] { 5, 6, 7 });
		assertEquals(1, fb.getFragments().size());
		assertTrue(fb.isCoveredByFragment(2, 6));
	}

	@Test
	void testAddFragmentContainsOther() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(4, new byte[] { 1, 2 });
		fb.addFragment(2, new byte[] { 1, 2, 3, 4, 5, 6 });
		assertEquals(1, fb.getFragments().size());
		assertTrue(fb.isCoveredByFragment(2, 6));
	}

	@Test
	void testGetBufferedLength() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(5, new byte[] { 1, 2, 3 });
		assertEquals(3, fb.getBufferedLength(5));
		assertEquals(2, fb.getBufferedLength(6));
		assertEquals(0, fb.getBufferedLength(8));
		assertEquals(0, fb.getBufferedLength(100));
	}

	@Test
	void testGetSmallestUnbufferedFragmentFullyBuffered() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(0, new byte[8]);
		Fragment f = fb.getSmallestUnbufferedFragment(2, 4);
		assertEquals(0, f.getLength());
	}

	@Test
	void testGetSmallestUnbufferedFragmentTrailing() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(0, new byte[4]);
		Fragment f = fb.getSmallestUnbufferedFragment(2, 6);
		assertEquals(4, f.getOffset());
		assertEquals(4, f.getLength());
	}

	@Test
	void testGetSmallestUnbufferedFragmentLeading() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(5, new byte[4]);
		Fragment f = fb.getSmallestUnbufferedFragment(0, 8);
		assertEquals(0, f.getOffset());
		assertEquals(5, f.getLength());
	}

	@Test
	void testGetSmallestUnbufferedFragmentContaining() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(5, new byte[2]);
		Fragment f = fb.getSmallestUnbufferedFragment(3, 6);
		assertEquals(3, f.getOffset());
		assertEquals(6, f.getLength());
	}

	@Test
	void testUpdateFrom() {
		FragmentBuffer fb = new FragmentBuffer(16);
		fb.addFragment(0, new byte[] { 1, 2 });
		FragmentBuffer other = new FragmentBuffer(16);
		other.addFragment(4, new byte[] { 9, 9 });
		fb.updateFrom(other);
		assertTrue(fb.isCoveredByFragment(0, 2));
		assertTrue(fb.isCoveredByFragment(4, 2));
		assertEquals(2, fb.getFragments().size());
	}

	@Test
	void testToString() {
		FragmentBuffer fb = new FragmentBuffer(4);
		fb.addFragment(0, new byte[] { 1 });
		String s = fb.toString();
		assertTrue(s.startsWith("FragmentBuffer [4,"));
	}

	@Test
	void testFragmentClass() {
		Fragment f = Fragment.getInstance(2, 3);
		assertEquals(2, f.getOffset());
		assertEquals(3, f.getLength());
		assertEquals("[2 .. 4 (3)]", f.toString());

		Fragment same = Fragment.getInstance(2, 3);
		Fragment diff = Fragment.getInstance(2, 4);
		assertEquals(f, f);
		assertEquals(f, same);
		assertEquals(f.hashCode(), same.hashCode());
		assertNotEquals(f, diff);
		assertNotEquals(f, null);
		assertNotEquals(f, "not a fragment");
	}

	@Test
	void testEqualsHashCode() {
		FragmentBuffer a = new FragmentBuffer(8);
		a.addFragment(0, new byte[] { 1, 2 });
		FragmentBuffer b = new FragmentBuffer(8);
		b.addFragment(0, new byte[] { 1, 2 });
		FragmentBuffer c = new FragmentBuffer(8);
		c.addFragment(0, new byte[] { 9, 9 });

		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a, null);
		assertNotEquals(a, "nope");

		Collection<Fragment> frags = a.getFragments();
		assertNotNull(frags);
		assertEquals(1, frags.size());
	}
}
