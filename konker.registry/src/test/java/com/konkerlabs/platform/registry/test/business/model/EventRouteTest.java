package com.konkerlabs.platform.registry.test.business.model;

import com.konkerlabs.platform.registry.business.model.EventRoute;
import com.konkerlabs.platform.registry.business.model.EventRoute.RouteActor;
import com.konkerlabs.platform.registry.business.model.RestTransformationStep;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.Transformation;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

public class EventRouteTest {

    private EventRoute subject;
    private String incomingAuthority = "0000000000000004";
    private String outgoingAuthority = "0000000000000005";

    @Before
    public void setUp() throws Exception {

        Tenant tenant = Tenant.builder().name("Konker").build();

        subject = EventRoute.builder()
            .tenant(tenant)
            .name("Route name")
            .description("Description")
            .incoming(RouteActor.builder().uri(new URI("device",incomingAuthority,null,null,null)).build())
            .outgoing(RouteActor.builder().uri(new URI("device",outgoingAuthority,null,null,null)).build())
//            .transformations(Arrays.asList(new EventRoute.RuleTransformation[]{
//                    new EventRoute.RuleTransformation(EventRouteExecutorImpl.RuleTransformationType.EXPRESSION_LANGUAGE.name())
//            }))
            .filteringExpression("#command.type == 'ButtonPressed'")
            .transformation(
                Transformation.builder()
                    .id("id")
                    .name("Name")
                    .description("Description")
                    .step(
                        RestTransformationStep.builder().build()
                    ).build()
            )
            .active(true)
            .build();
    }

    @Test
    public void shouldReturnAValidationMessageIfTenantIsNull() throws Exception {
        subject.setTenant(null);

        String expectedMessage = "Tenant cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfNameIsNull() throws Exception {
        subject.setName(null);

        String expectedMessage = "Name cannot be null or empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfNameIsEmpty() throws Exception {
        subject.setName("");

        String expectedMessage = "Name cannot be null or empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfIncomingIsNull() throws Exception {
        subject.setIncoming(null);

        String expectedMessage = "Incoming actor cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfIncomingURIIsNull() throws Exception {
        subject.getIncoming().setUri(null);

        String expectedMessage = "Incoming actor URI cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfIncomingURIIsEmpty() throws Exception {
        subject.getIncoming().setUri(new URI(null,null,null,null,null));

        String expectedMessage = "Incoming actor's URI cannot be empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfOutgoingIsNull() throws Exception {
        subject.setOutgoing(null);

        String expectedMessage = "Outgoing actor cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfOutgoingURIIsNull() throws Exception {
        subject.getOutgoing().setUri(null);

        String expectedMessage = "Outgoing actor URI cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfOutgoingURIIsEmpty() throws Exception {
        subject.getOutgoing().setUri(new URI(null,null,null,null,null));

        String expectedMessage = "Outgoing actor's URI cannot be empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldHaveNoValidationMessagesIfRecordIsValid() throws Exception {
        assertThat(subject.applyValidations(), nullValue());
    }
}
