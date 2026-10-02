package org.jmrtd.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.junit.jupiter.api.Test;

public class CVCAuthorizationTemplateTest {

	@Test
	public void testRoleValues() {
		assertEquals((byte) 0xC0, Role.CVCA.getValue());
		assertEquals((byte) 0x80, Role.DV_D.getValue());
		assertEquals((byte) 0x40, Role.DV_F.getValue());
		assertEquals((byte) 0x00, Role.IS.getValue());
	}

	@Test
	public void testPermissionValues() {
		assertEquals((byte) 0x00, Permission.READ_ACCESS_NONE.getValue());
		assertEquals((byte) 0x01, Permission.READ_ACCESS_DG3.getValue());
		assertEquals((byte) 0x02, Permission.READ_ACCESS_DG4.getValue());
		assertEquals((byte) 0x03, Permission.READ_ACCESS_DG3_AND_DG4.getValue());
	}

	@Test
	public void testPermissionImplies() {
		assertTrue(Permission.READ_ACCESS_NONE.implies(Permission.READ_ACCESS_NONE));
		assertFalse(Permission.READ_ACCESS_NONE.implies(Permission.READ_ACCESS_DG3));
		assertFalse(Permission.READ_ACCESS_NONE.implies(Permission.READ_ACCESS_DG4));
		assertFalse(Permission.READ_ACCESS_NONE.implies(Permission.READ_ACCESS_DG3_AND_DG4));

		assertFalse(Permission.READ_ACCESS_DG3.implies(Permission.READ_ACCESS_NONE));
		assertTrue(Permission.READ_ACCESS_DG3.implies(Permission.READ_ACCESS_DG3));
		assertFalse(Permission.READ_ACCESS_DG3.implies(Permission.READ_ACCESS_DG4));
		assertFalse(Permission.READ_ACCESS_DG3.implies(Permission.READ_ACCESS_DG3_AND_DG4));

		assertFalse(Permission.READ_ACCESS_DG4.implies(Permission.READ_ACCESS_NONE));
		assertFalse(Permission.READ_ACCESS_DG4.implies(Permission.READ_ACCESS_DG3));
		assertTrue(Permission.READ_ACCESS_DG4.implies(Permission.READ_ACCESS_DG4));
		assertFalse(Permission.READ_ACCESS_DG4.implies(Permission.READ_ACCESS_DG3_AND_DG4));

		assertFalse(Permission.READ_ACCESS_DG3_AND_DG4.implies(Permission.READ_ACCESS_NONE));
		assertTrue(Permission.READ_ACCESS_DG3_AND_DG4.implies(Permission.READ_ACCESS_DG3));
		assertTrue(Permission.READ_ACCESS_DG3_AND_DG4.implies(Permission.READ_ACCESS_DG4));
		assertTrue(Permission.READ_ACCESS_DG3_AND_DG4.implies(Permission.READ_ACCESS_DG3_AND_DG4));
	}

	@Test
	public void testConstructorAndGetters() {
		CVCAuthorizationTemplate template =
				new CVCAuthorizationTemplate(Role.DV_D, Permission.READ_ACCESS_DG3);
		assertEquals(Role.DV_D, template.getRole());
		assertEquals(Permission.READ_ACCESS_DG3, template.getAccessRight());
	}

	@Test
	public void testToString() {
		CVCAuthorizationTemplate template =
				new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_DG3_AND_DG4);
		assertEquals("ISREAD_ACCESS_DG3_AND_DG4", template.toString());
	}

	@Test
	public void testEqualsAndHashCode() {
		CVCAuthorizationTemplate t1 = new CVCAuthorizationTemplate(Role.CVCA, Permission.READ_ACCESS_NONE);
		CVCAuthorizationTemplate t2 = new CVCAuthorizationTemplate(Role.CVCA, Permission.READ_ACCESS_NONE);
		CVCAuthorizationTemplate t3 = new CVCAuthorizationTemplate(Role.DV_F, Permission.READ_ACCESS_NONE);
		CVCAuthorizationTemplate t4 = new CVCAuthorizationTemplate(Role.CVCA, Permission.READ_ACCESS_DG4);

		assertEquals(t1, t1);
		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
		assertNotEquals(t1, null);
		assertNotEquals(t1, "notATemplate");
		assertNotEquals(t1, t3);
		assertNotEquals(t1, t4);
	}

	@Test
	public void testFromRoleConversion() {
		for (Role role: Role.values()) {
			org.ejbca.cvc.AuthorizationRoleEnum converted = CVCAuthorizationTemplate.fromRole(role);
			assertEquals(role.name(), converted.name());
		}
	}

	@Test
	public void testFromPermissionConversion() {
		for (Permission permission: Permission.values()) {
			org.ejbca.cvc.AccessRightEnum converted = CVCAuthorizationTemplate.fromPermission(permission);
			assertEquals(permission.name(), converted.name());
		}
	}

	@Test
	public void testWrapEjbcaTemplate() throws Exception {
		for (Role role: Role.values()) {
			for (Permission permission: Permission.values()) {
				org.ejbca.cvc.CVCAuthorizationTemplate ejbcaTemplate =
						new org.ejbca.cvc.CVCAuthorizationTemplate(
								CVCAuthorizationTemplate.fromRole(role),
								CVCAuthorizationTemplate.fromPermission(permission));
				CVCAuthorizationTemplate template = new CVCAuthorizationTemplate(ejbcaTemplate);
				assertEquals(role, template.getRole());
				assertEquals(permission, template.getAccessRight());
			}
		}
	}
}
