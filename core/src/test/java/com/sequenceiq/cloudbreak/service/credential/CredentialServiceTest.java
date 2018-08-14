package com.sequenceiq.cloudbreak.service.credential;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private static final String PLATFORM = "OPENSTACK";

    private static final String TEST_CREDENTIAL_NAME = "testCredentialName";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Mock
    private UserProfileHandler userProfileHandler;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackRepository stackRepository;

    @InjectMocks
    private final CredentialService credentialService = new CredentialService();

    private Credential credentialToModify;

    private IdentityUser user;

    private Topology originalTopology;

    private String originalDescription;

    private Json originalAttributes;

    private Credential testCredential;

    @Before
    public void init() throws Exception {
        doNothing().when(notificationSender).send(any());
        when(credentialAdapter.init(any(Credential.class))).then(invocation -> invocation.getArgument(0));

        credentialToModify = new Credential();
        credentialToModify.setId(1L);
        credentialToModify.setCloudPlatform(PLATFORM);
        originalTopology = new Topology();
        credentialToModify.setTopology(originalTopology);
        originalDescription = "orig-desc";
        credentialToModify.setDescription(originalDescription);
        originalAttributes = new Json("test");
        credentialToModify.setAttributes(originalAttributes);
        when(credentialRepository.findByNameInUser(nullable(String.class), nullable(String.class))).thenReturn(credentialToModify);
        when(credentialRepository.save(any(Credential.class))).then(invocation -> invocation.getArgument(0));
        user = new IdentityUser("asef", "asdf", "asdf", null, "ASdf", "asdf", new Date());
        testCredential = mock(Credential.class);
        when(testCredential.getName()).thenReturn(TEST_CREDENTIAL_NAME);
    }

    @Test
    public void testModifyMapAllField() throws Exception {
        Credential credential = new Credential();
        credential.setCloudPlatform(PLATFORM);
        credential.setTopology(new Topology());
        credential.setAttributes(new Json("other"));
        credential.setDescription("mod-desc");
        Credential modify = credentialService.modify(user, credential);
        assertEquals(credential.getTopology(), modify.getTopology());
        assertEquals(credential.getAttributes(), modify.getAttributes());
        assertEquals(credential.getDescription(), modify.getDescription());
        assertNotEquals(originalTopology, modify.getTopology());
        assertNotEquals(originalAttributes, modify.getAttributes());
        assertNotEquals(originalAttributes, modify.getDescription());
        assertEquals(credential.cloudPlatform(), modify.cloudPlatform());
    }

    @Test
    public void testModifyMapNone() {
        Credential credential = new Credential();
        credential.setCloudPlatform(PLATFORM);
        Credential modify = credentialService.modify(user, credential);
        assertEquals(credentialToModify.getTopology(), modify.getTopology());
        assertEquals(credentialToModify.getAttributes(), modify.getAttributes());
        assertEquals(credentialToModify.getDescription(), modify.getDescription());
        assertEquals(originalTopology, modify.getTopology());
        assertEquals(originalAttributes, modify.getAttributes());
        assertEquals(originalDescription, modify.getDescription());
        assertEquals(credential.cloudPlatform(), modify.cloudPlatform());
    }

    @Test
    public void testModifyDifferentPlatform() {
        Credential credential = new Credential();
        credential.setCloudPlatform("BAD");

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Modifying credential platform is forbidden");

        credentialService.modify(user, credential);
    }

    @Test
    public void testRetrieveAccountCredentialsWhenUserIsAdmin() {
        IdentityUser user = mock(IdentityUser.class);
        Set<String> platforms = Sets.newHashSet("AWS");
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");

        when(user.getRoles()).thenReturn(Collections.singletonList(IdentityUserRole.fromString("ADMIN")));
        when(user.getAccount()).thenReturn("account");
        when(accountPreferencesService.enabledPlatforms()).thenReturn(platforms);
        when(credentialRepository.findAllInAccountAndFilterByPlatforms(user.getAccount(), platforms)).thenReturn(Sets.newHashSet(credential));

        Set<Credential> actual = credentialService.retrieveAccountCredentials(user);

        assertEquals("AWS", actual.stream().findFirst().get().cloudPlatform());

        verify(credentialRepository, times(1)).findAllInAccountAndFilterByPlatforms("account", platforms);
    }

    @Test
    public void testRetrieveAccountCredentialsWhenUserIsNotAdmin() {
        IdentityUser user = mock(IdentityUser.class);
        Set<String> platforms = Sets.newHashSet("AWS");
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");

        when(user.getRoles()).thenReturn(Collections.singletonList(IdentityUserRole.fromString("USER")));
        when(user.getAccount()).thenReturn("account");
        when(user.getUserId()).thenReturn("userId");
        when(accountPreferencesService.enabledPlatforms()).thenReturn(platforms);
        when(credentialRepository.findPublicInAccountForUserFilterByPlatforms("userId", "account", platforms)).thenReturn(Sets.newHashSet(credential));

        Set<Credential> actual = credentialService.retrieveAccountCredentials(user);

        assertEquals("AWS", actual.stream().findFirst().get().cloudPlatform());

        verify(credentialRepository, times(1)).findPublicInAccountForUserFilterByPlatforms("userId", "account", platforms);
    }

    @Test
    public void testCanDeleteWhenCredentialIsNullThenNotFoundExceptionShouldCome() {
        thrown.expect(NotFoundException.class);

        credentialService.canDelete(null);

        verify(stackRepository, times(0)).findByCredential(any(Credential.class));
    }

    @Test
    public void testCanDeleteWhenStackRepositoryDoesNotFindAnyStackWithTheGivenCredentialThenTrueShouldCome() {
        when(stackRepository.findByCredential(testCredential)).thenReturn(Collections.emptySet());

        boolean result = credentialService.canDelete(testCredential);

        assertTrue(result);
        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testCanDeleteWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));

        credentialService.canDelete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testCanDeleteWhenMoreThaneOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stack1Name = "testStack1Name";
        String stack2Name = "testStack1Name";
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn(stack1Name);
        when(stack2.getName()).thenReturn(stack2Name);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, String.format("%s, %s", stack1Name, stack2Name)));

        credentialService.canDelete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testDeleteWhenCredentialIsNullThenNotFoundExceptionShouldCome() {
        thrown.expect(NotFoundException.class);

        credentialService.delete(null);

        verify(stackRepository, times(0)).findByCredential(any(Credential.class));
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));

        credentialService.delete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteWhenMoreThaneOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stack1Name = "testStack1Name";
        String stack2Name = "testStack1Name";
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn(stack1Name);
        when(stack2.getName()).thenReturn(stack2Name);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, String.format("%s, %s", stack1Name, stack2Name)));

        credentialService.delete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteWhenCredentialIsDeletableThenItWillBeArchivedProperly() {
        Credential credential = new Credential();
        credential.setName(TEST_CREDENTIAL_NAME);
        credential.setArchived(false);
        credential.setTopology(new Topology());
        when(stackRepository.findByCredential(credential)).thenReturn(Collections.emptySet());

        Credential deleted = credentialService.delete(credential);

        assertTrue(deleted.isArchived());
        assertNull(deleted.getTopology());
        assertNotEquals(TEST_CREDENTIAL_NAME, credential.getName());

        verify(stackRepository, times(1)).findByCredential(credential);
        verify(userProfileHandler, times(1)).destroyProfileCredentialPreparation(credential);
        verify(credentialRepository, times(1)).save(credential);
    }

}