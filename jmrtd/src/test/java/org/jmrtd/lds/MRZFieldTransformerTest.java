package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * MRZFieldTransformer is an interface; this test exercises the documented
 * contract using a trivial reference implementation.
 */
public class MRZFieldTransformerTest {

	private static final MRZFieldTransformer TRANSFORMER = new MRZFieldTransformer() {
		public String[] truncateNames(String primaryIdentifier, String secondaryIdentifier, int length) {
			String result = primaryIdentifier + "<<" + secondaryIdentifier;
			while (result.length() > length && !secondaryIdentifier.isEmpty()) {
				secondaryIdentifier = secondaryIdentifier.substring(0, secondaryIdentifier.length() - 1);
				result = primaryIdentifier + "<<" + secondaryIdentifier;
			}
			if (result.length() > length) {
				primaryIdentifier = primaryIdentifier.substring(0, length - 2 - secondaryIdentifier.length());
			}
			return new String[] { primaryIdentifier, secondaryIdentifier };
		}

		public String transliterate(String text) {
			return text == null ? null : text.replace('\u00C4', 'A').replace('\u00D6', 'O');
		}
	};

	@Test
	public void testTruncateNamesContract() {
		String[] names = TRANSFORMER.truncateNames("ERIKSSON", "ANNA<MARIA", 30);
		assertEquals(2, names.length);
		String combined = names[0] + "<<" + names[1];
		assertTrue(combined.length() <= 30);
		assertEquals("ERIKSSON", names[0]);
	}

	@Test
	public void testTruncateNamesLongName() {
		String[] names = TRANSFORMER.truncateNames("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BBBBBBBBBBBB", 20);
		String combined = names[0] + "<<" + names[1];
		assertTrue(combined.length() <= 20);
	}

	@Test
	public void testTransliterate() {
		assertEquals("AO", TRANSFORMER.transliterate("\u00C4\u00D6"));
		assertEquals("ANNAMARIA", TRANSFORMER.transliterate("ANNAMARIA"));
	}
}
