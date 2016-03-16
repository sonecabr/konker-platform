package com.konkerlabs.platform.registry.test.business.services;

import static org.junit.rules.ExpectedException.none;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.konkerlabs.platform.registry.business.model.RestDestination;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.services.api.RestDestinationService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.test.base.BusinessLayerTestSupport;
import com.konkerlabs.platform.registry.test.base.BusinessTestConfiguration;
import com.konkerlabs.platform.registry.test.base.MongoTestConfiguration;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;

import static com.konkerlabs.platform.registry.test.base.matchers.ServiceResponseMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MongoTestConfiguration.class, BusinessTestConfiguration.class })
@UsingDataSet(locations = { "/fixtures/tenants.json", "/fixtures/rest-destinations.json" })
public class RestDestinationServiceTest extends BusinessLayerTestSupport {
    @Rule
    public ExpectedException thrown = none();

    @Autowired
    private RestDestinationService subject;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenant;
    private Tenant emptyTenant;
    private Tenant otherTenant;
    private Tenant inexistentTenant;
    private RestDestination newRestDestination;
    private RestDestination oldRestDestination;

    public static final String THE_DESTINATION_ID = "4e6c441c-eaf9-11e5-a33b-8374b127eaa8";
    public static final String THE_DESTINATION_GUID = "dda64780-eb81-11e5-958b-a73dab8b32ee";
    public static final String THE_DESTINATION_NAME = "a restful destination";
    public static final String OTHER_DESTINATION_NAME = "sjhsdf";
    public static final String OTHER_TENANT_DESTINATION_ID = "109cd550-eafb-11e5-b610-d3af18d1439d";
    public static final String OTHER_TENANT_DESTINATION_GUID = "faf3391a-eb81-11e5-8c84-33e0d9a91f0c";
    public static final String OTHER_TENANT_DESTINATION_NAME = "another tenant restful destination";
    public static final String INEXISTENT_DESTINATION_ID = UUID.randomUUID().toString();
    public static final String INEXISTENT_DESTINATION_GUID = UUID.randomUUID().toString();
    public static final String UPDATED_DESTINATION_NAME = "updated restful destination";

    @Before
    public void setUp() {
        emptyTenant = tenantRepository.findByName("EmptyTenant");
        tenant = tenantRepository.findByName("Konker");
        otherTenant = tenantRepository.findByName("InMetrics");
        inexistentTenant = Tenant.builder().domainName("someInexistentDomain")
                .id("e2bfa8b0-eaf5-11e5-8fd5-a755d49a5c5b").name("someInexistentName").build();

        newRestDestination = spy(
                RestDestination.builder().name("New Name").active(true).serviceURI("http://host.com/")
                        .serviceUsername("user").servicePassword("password").build());
        oldRestDestination = spy(RestDestination.builder().id(THE_DESTINATION_ID).name(THE_DESTINATION_NAME)
                .tenant(tenant).active(false).serviceURI("http://host.com/").serviceUsername("user")
                .servicePassword("password").build());

    }

    // ============================== findAll ==============================//
    @Test
    public void shouldReturnEmptyListIfDestinationsDoesNotExistWehnFindAll() {
        ServiceResponse<List<RestDestination>> response = subject.findAll(emptyTenant);
        assertThat(response, isResponseOk());
        assertThat(response.getResult(), empty());
    }

    @Test
    public void shouldReturnErrorMessageIfTenantDoesNotExistWhenFindAll() {
        ServiceResponse<List<RestDestination>> response = subject.findAll(inexistentTenant);
        assertThat(response, hasErrorMessage("Tenant does not exist"));
    }

    @Test
    public void shouldReturnErrorMessageIfTenantIsNullWhenFindAll() {
        ServiceResponse<List<RestDestination>> response = subject.findAll(null);
        assertThat(response, hasErrorMessage("Tenant cannot be null"));
    }

    @Test
    public void shouldReturnDestinationsWhenFindAll() {
        ServiceResponse<List<RestDestination>> response = subject.findAll(tenant);
        assertThat(response, isResponseOk());
        assertThat(response.getResult(), hasSize(greaterThan(1)));

        List<String> ids = response.getResult().stream().map(RestDestination::getId).collect(Collectors.toList());
        assertThat(ids, hasItem(THE_DESTINATION_ID));
        assertThat(ids, not(hasItem(OTHER_TENANT_DESTINATION_ID)));
    }

    @Test
    public void shouldReturnDestinationsWhenOtherTenantFindAll() {
        ServiceResponse<List<RestDestination>> response = subject.findAll(otherTenant);
        assertThat(response, isResponseOk());
        assertThat(response.getResult(), not(empty()));

        List<String> ids = response.getResult().stream().map(RestDestination::getId).collect(Collectors.toList());
        assertThat(ids, not(hasItem(THE_DESTINATION_ID)));
        assertThat(ids, hasItem(OTHER_TENANT_DESTINATION_ID));
    }

    // ============================== getByID ==============================//

    @Test
    public void shouldReturnDestinationIfExistsWithinTenantWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(tenant, THE_DESTINATION_GUID);
        assertThat(response, isResponseOk());
        assertThat(response.getResult().getName(), equalTo(THE_DESTINATION_NAME));
    }

    @Test
    public void shouldReturnOtherDestinationIfExistsWithinOtherTenantWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(otherTenant, OTHER_TENANT_DESTINATION_GUID);
        assertThat(response, isResponseOk());
        assertThat(response.getResult().getName(), equalTo(OTHER_TENANT_DESTINATION_NAME));
    }

    @Test
    public void shouldReturnErrorIfDestinationIsOwnedByAnotherTenantWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(tenant, OTHER_TENANT_DESTINATION_ID);
        assertThat(response, hasErrorMessage("REST Destination does not exist"));
    }

    @Test
    public void shouldReturnErrorIfDestinationDoesNotExistWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(tenant, INEXISTENT_DESTINATION_ID);
        assertThat(response, hasErrorMessage("REST Destination does not exist"));
    }

    @Test
    public void shouldReturnErrorIfTenantIsNullWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(null, THE_DESTINATION_ID);
        assertThat(response, hasErrorMessage("Tenant cannot be null"));
    }

    @Test
    public void shouldReturnErrorIfIDIsNullWhenGetByID() {
        ServiceResponse<RestDestination> response = subject.getByGUID(tenant, null);
        assertThat(response, hasErrorMessage("REST Destination ID cannot be null"));
    }

    // ============================== register ==============================//

    @Test
    public void shouldRegisterIfEverythingIsOkWhenRegister() {
        assertThat(newRestDestination.getId(), nullValue());
        ServiceResponse<RestDestination> response = subject.register(tenant, newRestDestination);
        assertThat(response, isResponseOk());
        assertThat(response.getResult().getId(), not(nullValue()));
        assertThat(response.getResult().getTenant(), equalTo(tenant));
        assertThat(response.getResult().isActive(), equalTo(Boolean.TRUE));
    }

    @Test
    public void shouldReturnErrorIfValidationsFailWhenRegister() {
        when(newRestDestination.applyValidations()).thenReturn(Collections.singletonList("Error Message"));
        ServiceResponse<RestDestination> response = subject.register(tenant, newRestDestination);
        assertThat(response, hasErrorMessage("Error Message"));
        assertThat(newRestDestination.getId(), nullValue());
    }

    @Test
    public void shouldReturnErrorIfTenantIsNullWhenRegister() {
        ServiceResponse<RestDestination> response = subject.register(null, newRestDestination);
        assertThat(response, hasErrorMessage("Tenant cannot be null"));
        assertThat(newRestDestination.getId(), nullValue());
    }

    @Test
    public void shouldReturnErrorIfTenantInexistentWhenRegister() {
        ServiceResponse<RestDestination> response = subject.register(inexistentTenant, newRestDestination);
        assertThat(response, hasErrorMessage("Tenant does not exist"));
        assertThat(newRestDestination.getId(), nullValue());
    }

    @Test
    public void shouldReturnErrorIfDestinatioIsNullWhenRegister() {
        ServiceResponse<RestDestination> response = subject.register(inexistentTenant, null);
        assertThat(response, hasErrorMessage("REST Destination cannot be null"));
        assertThat(newRestDestination.getId(), nullValue());
    }

    @Test
    public void shouldReturnErrorIfDestinationExistsWhenRegister() {
        newRestDestination.setName(THE_DESTINATION_NAME);
        ServiceResponse<RestDestination> response = subject.register(tenant, newRestDestination);
        assertThat(response, hasErrorMessage("Name already exists"));
        assertThat(newRestDestination.getId(), nullValue());
    }

    @Test
    public void shouldGenerateNewIdIfIDAlreadyExistsWhenRegister() {
        newRestDestination.setId(THE_DESTINATION_ID);
        ServiceResponse<RestDestination> response = subject.register(tenant, newRestDestination);
        assertThat(response, isResponseOk());
        assertThat(response.getResult().getId(), not(equalTo(THE_DESTINATION_ID)));
    }

    @Test
    public void shouldAssociateToNewTenantIfIDAlreadyExistsWhenRegister() {
        newRestDestination.setTenant(otherTenant);
        ServiceResponse<RestDestination> response = subject.register(tenant, newRestDestination);
        assertThat(response, isResponseOk());
        assertThat(response.getResult().getTenant(), equalTo(tenant));
        assertThat(response.getResult().getId(), not(nullValue()));
        assertThat(response.getResult().getGuid(), not(nullValue()));
        assertThat(subject.getByGUID(otherTenant, response.getResult().getGuid()),
                hasErrorMessage("REST Destination does not exist"));
    }

    // ============================== update ==============================//
    @Test
    public void shouldSaveIfEverythingIsOkWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(tenant, THE_DESTINATION_GUID, oldRestDestination);
        RestDestination returned = response.getResult();
        assertThat(response, isResponseOk());
        assertThat(returned.getId(), equalTo(THE_DESTINATION_ID));
        assertThat(returned.getTenant(), equalTo(tenant));
        assertThat(returned.getName(), equalTo(UPDATED_DESTINATION_NAME));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), equalTo(UPDATED_DESTINATION_NAME));
    }

  
    @Test
    public void shouldIgnoreGUIDTenantAndIDInsideDataObjectWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        oldRestDestination.setId(INEXISTENT_DESTINATION_ID);
        oldRestDestination.setGuid(INEXISTENT_DESTINATION_GUID);
        oldRestDestination.setTenant(otherTenant);
            
        ServiceResponse<RestDestination> response = subject.update(tenant, THE_DESTINATION_GUID, oldRestDestination);
        RestDestination returned = response.getResult();
        assertThat(response, isResponseOk());
        assertThat(returned.getId(), equalTo(THE_DESTINATION_ID));
        assertThat(returned.getGuid(), equalTo(THE_DESTINATION_GUID));
        assertThat(returned.getTenant(), equalTo(tenant));
        assertThat(returned.getName(), equalTo(UPDATED_DESTINATION_NAME));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), equalTo(UPDATED_DESTINATION_NAME));
    }

    @Test
    public void shouldReturnErrorIfOwnedByOtherTenantWhenUpdate() {
        RestDestination before = subject.getByGUID(otherTenant, OTHER_TENANT_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setId(OTHER_TENANT_DESTINATION_ID);
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(tenant, OTHER_TENANT_DESTINATION_GUID, oldRestDestination);
        assertThat(response, hasErrorMessage("REST Destination does not exist"));

        RestDestination after = subject.getByGUID(otherTenant, OTHER_TENANT_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }

    @Test
    public void shouldReturnErrorIfHasValidationErrorsWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        when(oldRestDestination.applyValidations()).thenReturn(Collections.singletonList("My Error"));
        
        ServiceResponse<RestDestination> response = subject.update(tenant, THE_DESTINATION_GUID, oldRestDestination);
        assertThat(response, hasErrorMessage("My Error"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }

    @Test
    public void shouldReturnErrorIfTenantDoesNotExistWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(inexistentTenant, THE_DESTINATION_GUID, oldRestDestination);
        assertThat(response, hasErrorMessage("Tenant does not exist"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }

    @Test
    public void shouldReturnErrorIfTenantIsNullWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(null, THE_DESTINATION_GUID, oldRestDestination);
        assertThat(response, hasErrorMessage("Tenant cannot be null"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }

    @Test
    public void shouldReturnErrorIfIDIsNullWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(tenant, null, oldRestDestination);
        assertThat(response, hasErrorMessage("REST Destination ID cannot be null"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }


    @Test
    public void shouldReturnErrorIfIDDoesNotExistWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
       
        oldRestDestination.setName(UPDATED_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(tenant, INEXISTENT_DESTINATION_ID, oldRestDestination);
        assertThat(response, hasErrorMessage("REST Destination does not exist"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(UPDATED_DESTINATION_NAME)));
    }

    @Test
    public void shouldReturnErrorIfNameIsDuplicateWhenUpdate() {
        RestDestination before = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(before.getName(), not(equalTo(OTHER_DESTINATION_NAME)));
       
        oldRestDestination.setName(OTHER_DESTINATION_NAME);
        
        ServiceResponse<RestDestination> response = subject.update(tenant, THE_DESTINATION_ID, oldRestDestination);
        assertThat(response, hasErrorMessage("REST Destination Name already exists"));

        RestDestination after = subject.getByGUID(tenant, THE_DESTINATION_GUID).getResult();
        assertThat(after.getName(), not(equalTo(OTHER_DESTINATION_NAME)));
    }


}
