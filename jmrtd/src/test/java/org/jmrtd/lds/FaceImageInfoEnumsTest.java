package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import org.jmrtd.lds.FaceImageInfo.EyeColor;
import org.jmrtd.lds.FaceImageInfo.Expression;
import org.jmrtd.lds.FaceImageInfo.FaceImageType;
import org.jmrtd.lds.FaceImageInfo.Features;
import org.jmrtd.lds.FaceImageInfo.HairColor;
import org.jmrtd.lds.FaceImageInfo.ImageColorSpace;
import org.jmrtd.lds.FaceImageInfo.ImageDataType;
import org.jmrtd.lds.FaceImageInfo.SourceType;
import org.junit.jupiter.api.Test;

class FaceImageInfoEnumsTest {

	@Test
	void testAllEnums() {
		
		assertTrue(EyeColor.values().length >= 8);
		assertTrue(HairColor.values().length >= 9);
		assertTrue(Features.values().length >= 10);
		assertTrue(Expression.values().length >= 7);
		assertTrue(FaceImageType.values().length >= 3);
		assertTrue(ImageDataType.values().length >= 2);
		assertTrue(ImageColorSpace.values().length >= 4);
		assertTrue(SourceType.values().length >= 6);


		for (EyeColor ec : EyeColor.values()) { assertEquals(ec, EyeColor.valueOf(ec.name())); }
		for (HairColor hc : HairColor.values()) { assertEquals(hc, HairColor.valueOf(hc.name())); }
		for (Features ft : Features.values()) { assertEquals(ft, Features.valueOf(ft.name())); }
		for (Expression ex : Expression.values()) { assertEquals(ex, Expression.valueOf(ex.name())); }
		for (FaceImageType ft : FaceImageType.values()) { assertEquals(ft, FaceImageType.valueOf(ft.name())); }
		for (ImageDataType dt : ImageDataType.values()) { assertEquals(dt, ImageDataType.valueOf(dt.name())); }
		for (ImageColorSpace cs : ImageColorSpace.values()) { assertEquals(cs, ImageColorSpace.valueOf(cs.name())); }
		for (SourceType st : SourceType.values()) { assertEquals(st, SourceType.valueOf(st.name())); }
	}
}
