/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.ldap.dao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.LdapName;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link GroupLdapRepository}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class GroupLdapRepositoryTest extends AbstractDataGeneratorTest {

	@Test
	public void addUser() {
		final Set<String> users = new HashSet<>();
		final GroupLdapRepository groupRepository = new GroupLdapRepository() {
			@Override
			public GroupOrg findById(final String name) {
				return new GroupOrg("dc=" + name, name, users);
			}

		};
		final LdapCacheRepository cacheRepository = Mockito.mock(LdapCacheRepository.class);
		groupRepository.setLdapCacheRepository(cacheRepository);
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		addUser(groupRepository);

		Mockito.verify(cacheRepository, VerificationModeFactory.times(1)).addUserToGroup(ArgumentMatchers.any(UserOrg.class),
				ArgumentMatchers.any(GroupOrg.class));
	}

	@Test
	public void addUserAlreadyMember() {
		final Set<String> users = new HashSet<>();
		users.add("flast1");
		final GroupLdapRepository groupRepository = new GroupLdapRepository() {
			@Override
			public GroupOrg findById(final String name) {
				return new GroupOrg("dc=" + name, name, users);
			}

		};
		final LdapCacheRepository cacheRepository = Mockito.mock(LdapCacheRepository.class);
		groupRepository.setLdapCacheRepository(cacheRepository);
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		addUser(groupRepository);

		Assertions.assertEquals(1, users.size());
		Assertions.assertTrue(users.contains("flast1"));
	}

	/**
	 * Mock a not managed LDAP desynchronization
	 */
	@Test
	public void addUserSyncError() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.AttributeInUseException(new AttributeInUseException("any"))).when(ldapTemplate)
				.modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());

		Assertions.assertThrows(org.springframework.ldap.AttributeInUseException.class, () -> {
			addUser(groupRepository);
		});
	}

	/**
	 * Mock a managed LDAP desynchronization
	 */
	@Test
	public void addUserSync1() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.AttributeInUseException(new AttributeInUseException("value #0 already exists")))
				.when(ldapTemplate).modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());

		addUser(groupRepository);
	}

	/**
	 * Mock a managed LDAP desynchronization
	 */
	@Test
	public void addUserSync2() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.AttributeInUseException(new AttributeInUseException("ATTRIBUTE_OR_VALUE_EXISTS")))
				.when(ldapTemplate).modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());

		addUser(groupRepository);
	}

	@Test
	public void removeUser() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		removeUser(groupRepository);
	}

	/**
	 * Mock a managed LDAP schema violation
	 */
	@Test
	public void removeUserNotMember() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.SchemaViolationException(new SchemaViolationException("any"))).when(ldapTemplate)
				.modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());
		removeUser(groupRepository);
	}

	/**
	 * Mock a managed LDAP schema violation
	 */
	@Test
	public void removeUserSchema() {
		final GroupLdapRepository groupRepository = new GroupLdapRepository() {
			@Override
			public GroupOrg findById(final String name) {
				// The group has only the user user we want to remove
				return new GroupOrg("dc=" + name, name, Collections.singleton("flast1"));
			}

		};
		groupRepository.setLdapCacheRepository(Mockito.mock(LdapCacheRepository.class));

		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.SchemaViolationException(new SchemaViolationException("any"))).when(ldapTemplate)
				.modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			removeUser(groupRepository);
		}), "groups", "last-member-of-group");
	}

	@Test
	public void removeGroup() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		groupRepository.removeGroup(new GroupOrg("any", "any", null), "DIG RHA");
	}

	@Test
	public void addGroup() {
		final GroupLdapRepository groupRepository = newGroupLdapRepository();
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		groupRepository.addGroup(new GroupOrg("dc=any", "any", null), "DIG RHA");
	}

	/**
	 * Mock a managed LDAP desynchronization
	 */
	@Test
	public void removeUserSync() {
		final GroupLdapRepository groupRepository = new GroupLdapRepository() {
			@Override
			public GroupOrg findById(final String name) {
				// The group has only the user user we want to remove
				return new GroupOrg("dc=" + name, name, Collections.singleton("flast1"));
			}

		};
		groupRepository.setLdapCacheRepository(Mockito.mock(LdapCacheRepository.class));
		final LdapTemplate ldapTemplate = Mockito.mock(LdapTemplate.class);
		groupRepository.setTemplate(ldapTemplate);
		Mockito.doThrow(new org.springframework.ldap.AttributeInUseException(new AttributeInUseException("any"))).when(ldapTemplate)
				.modifyAttributes(ArgumentMatchers.any(LdapName.class), ArgumentMatchers.any());
		removeUser(groupRepository);
	}

	private GroupLdapRepository newGroupLdapRepository() {
		final GroupLdapRepository groupRepository = new GroupLdapRepository() {
			@Override
			public GroupOrg findById(final String name) {
				return new GroupOrg("dc=" + name, name, new HashSet<>());
			}

		};
		groupRepository.setLdapCacheRepository(Mockito.mock(LdapCacheRepository.class));
		return groupRepository;
	}

	private void removeUser(final GroupLdapRepository groupRepository) {
		final UserOrg user = new UserOrg();
		user.setId("flast1");
		user.setDn("dc=com");
		user.setCompany("ing");
		groupRepository.removeUser(user, "DIG RHA");
	}

	private void addUser(final GroupLdapRepository groupRepository) {
		final UserOrg user = new UserOrg();
		user.setId("flast1");
		user.setDn("dc=com");
		user.setCompany("ing");
		groupRepository.addUser(user, "DIG RHA");
	}

}
