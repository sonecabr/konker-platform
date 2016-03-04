package com.konkerlabs.platform.registry.test.web.controllers;

import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.EventRule;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.model.behaviors.SmsURIDealer;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.rules.api.EventRuleService;
import com.konkerlabs.platform.registry.config.WebMvcConfig;
import com.konkerlabs.platform.registry.test.base.SecurityTestConfiguration;
import com.konkerlabs.platform.registry.test.base.WebLayerTestContext;
import com.konkerlabs.platform.registry.test.base.WebTestConfiguration;
import com.konkerlabs.platform.registry.web.forms.EventRuleForm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    WebMvcConfig.class,
    WebTestConfiguration.class,
    SecurityTestConfiguration.class,
    EventRuleControllerTest.EventRuleTestContextConfig.class
})
public class EventRuleControllerTest extends WebLayerTestContext {

    @Autowired
    private EventRuleService eventRuleService;
    @Autowired
    private Tenant tenant;

    private Device incomingDevice;
    private Device outgoingDevice;
    private EventRule rule;
    private List<EventRule> registeredRules;
    private ServiceResponse<EventRule> response;
    private MultiValueMap<String,String> ruleData;
    private EventRuleForm ruleForm;

    private String ruleId = "71fb0d48-674b-4f64-a3e5-0256ff3a63af";

    @Before
    public void setUp() throws Exception {
        incomingDevice = Device.builder().deviceId("0000000000000004").build();
        outgoingDevice = Device.builder().deviceId("0000000000000005").build();

        ruleForm = new EventRuleForm();
        ruleForm.setName("Rule name");
        ruleForm.setDescription("Rule description");
        ruleForm.setIncomingAuthority(incomingDevice.getDeviceId());
        ruleForm.setIncomingChannel("command");
        ruleForm.setOutgoingScheme("device");
        ruleForm.setOutgoingDeviceAuthority(outgoingDevice.getDeviceId());
        ruleForm.setOutgoingDeviceChannel("in");
        ruleForm.setFilterClause("LEDSwitch");
        ruleForm.setActive(true);

        ruleForm.setAdditionalSupplier(() -> tenant.getDomainName());

        ruleData = new LinkedMultiValueMap<>();
        ruleData.add("name",ruleForm.getName());
        ruleData.add("description",ruleForm.getDescription());
        ruleData.add("incomingAuthority",ruleForm.getIncomingAuthority());
        ruleData.add("incomingChannel", ruleForm.getIncomingChannel());
        ruleData.add("outgoingScheme", ruleForm.getOutgoingScheme());
        ruleData.add("outgoingDeviceAuthority",ruleForm.getOutgoingDeviceAuthority());
        ruleData.add("outgoingDeviceChannel", ruleForm.getOutgoingDeviceChannel());
        ruleData.add("filterClause",ruleForm.getFilterClause());
        ruleData.add("active","true");

        EventRule.RuleTransformation contentMatchTransformation = new EventRule.RuleTransformation("EXPRESSION_LANGUAGE");
        contentMatchTransformation.getData().put("value",ruleForm.getFilterClause());

        DeviceURIDealer deviceUriDealer = new DeviceURIDealer() {};
        SmsURIDealer smsURIDealer = new SmsURIDealer() {};

        Supplier<URI> outgoingUriSupplier = () -> {
            switch (ruleForm.getOutgoingScheme()) {
                case DeviceURIDealer.DEVICE_URI_SCHEME:
                    return deviceUriDealer.toDeviceRuleURI(tenant.getDomainName(), ruleForm.getOutgoingDeviceAuthority());
                case SmsURIDealer.SMS_URI_SCHEME:
                    return smsURIDealer.toSmsURI(ruleForm.getOutgoingSmsPhoneNumber());
                default: return null;
            }
        };

        rule = EventRule.builder()
            .name(ruleForm.getName())
            .description(ruleForm.getDescription())
            .incoming(new EventRule.RuleActor(
                deviceUriDealer.toDeviceRuleURI(tenant.getDomainName(),ruleForm.getIncomingAuthority())
            ))
            .outgoing(new EventRule.RuleActor(
                outgoingUriSupplier.get()
            ))
            .transformations(Arrays.asList(new EventRule.RuleTransformation[]{contentMatchTransformation}))
            .active(ruleForm.isActive())
            .build();

        rule.getIncoming().getData().put("channel", ruleForm.getIncomingChannel());
        rule.getOutgoing().getData().put("channel", ruleForm.getOutgoingDeviceChannel());

        registeredRules = new ArrayList<EventRule>(Arrays.asList(new EventRule[]{rule}));
    }

    @After
    public void tearDown() {
        Mockito.reset(eventRuleService);
    }

    @Test
    public void shouldListAllRegisteredRules() throws Exception {
        when(eventRuleService.getAll(eq(tenant))).thenReturn(registeredRules);

        getMockMvc().perform(get("/rules")).andExpect(model().attribute("rules", equalTo(registeredRules)))
                .andExpect(view().name("rules/index"));
    }

    @Test
    public void shouldShowCreationForm() throws Exception {
        getMockMvc().perform(get("/rules/new"))
            .andExpect(view().name("rules/form"))
            .andExpect(model().attribute("rule",new EventRuleForm()))
            .andExpect(model().attribute("action","/rules/save"));
    }

    @Test
    public void shouldRenderDeviceOutgoingViewFragment() throws Exception {
        getMockMvc().perform(get("/rules/outgoing/{0}","device"))
                .andExpect(view().name("rules/device-outgoing"))
                .andExpect(model().attribute("rule",new EventRuleForm()));
    }

    @Test
    public void shouldRenderSmsViewFragment() throws Exception {
        getMockMvc().perform(get("/rules/outgoing/{0}","sms"))
                .andExpect(view().name("rules/sms-outgoing"))
                .andExpect(model().attribute("rule",new EventRuleForm()));
    }

    @Test
    public void shouldRenderEmptyBodyWhenSchemeIsUnknown() throws Exception {
        getMockMvc().perform(get("/rules/outgoing/{0}","unknown_scheme"))
                .andExpect(view().name("common/empty"));
    }

    @Test
    public void shouldBindErrorMessagesWhenRegistrationFailsAndGoBackToCreationForm() throws Exception {
        response = ServiceResponse.<EventRule>builder().responseMessages(Arrays.asList(new String[] { "Some error" }))
                .status(ServiceResponse.Status.ERROR).<EventRule>build();

        when(eventRuleService.save(eq(tenant),eq(rule))).thenReturn(response);

        getMockMvc().perform(post("/rules/save").params(ruleData))
                .andExpect(model().attribute("errors", equalTo(response.getResponseMessages())))
                .andExpect(model().attribute("rule", equalTo(ruleForm))).andExpect(view().name("rules/form"));

        verify(eventRuleService).save(eq(tenant),eq(rule));
    }

    @Test
    public void shouldBindBusinessExceptionMessageWhenRegistrationFailsAndGoBackToCreationForm() throws Exception {
        String exceptionMessage = "Some business exception message";

        when(eventRuleService.save(eq(tenant),eq(rule))).thenThrow(new BusinessException(exceptionMessage));

        getMockMvc().perform(post("/rules/save").params(ruleData))
                .andExpect(model().attribute("errors", equalTo(Arrays.asList(new String[] { exceptionMessage }))))
                .andExpect(model().attribute("rule", equalTo(ruleForm)))
                .andExpect(view().name("rules/form"));

        verify(eventRuleService).save(eq(tenant),eq(rule));
    }

    @Test
    public void shouldRedirectToShowAfterSuccessfulRuleCreation() throws Exception {
        response = spy(ServiceResponse.<EventRule>builder()
                .status(ServiceResponse.Status.OK)
                .result(rule)
                .<EventRule>build());

        when(eventRuleService.save(eq(tenant),eq(rule))).thenReturn(response);

        getMockMvc().perform(post("/rules/save").params(ruleData))
                .andExpect(flash().attribute("message", "Rule registered successfully"))
                .andExpect(redirectedUrl(MessageFormat.format("/rules/{0}",rule.getId())));

        verify(eventRuleService).save(eq(tenant),eq(rule));
    }

    @Test
    public void shouldShowEditForm() throws Exception {
        ruleForm.setAdditionalSupplier(null);

        when(eventRuleService.getById(tenant, ruleId)).thenReturn(
                ServiceResponse.<EventRule>builder().result(rule).status(ServiceResponse.Status.OK).<EventRule>build());

        getMockMvc().perform(get(MessageFormat.format("/rules/{0}/edit", ruleId)))
                .andExpect(model().attribute("rule", equalTo(ruleForm)))
                .andExpect(model().attribute("action", MessageFormat.format("/rules/{0}",ruleId)))
                .andExpect(view().name("rules/form"));
    }

    @Test
    public void shouldRedirectToShowAfterSuccessfulRuleEdit() throws Exception {
        rule.setId(ruleId);
        response = spy(ServiceResponse.<EventRule>builder()
                .status(ServiceResponse.Status.OK)
                .result(rule)
                .<EventRule>build());

        when(eventRuleService.save(eq(tenant),eq(rule))).thenReturn(response);

        getMockMvc().perform(post("/rules/{0}",rule.getId()).params(ruleData))
                .andExpect(flash().attribute("message", "Rule registered successfully"))
                .andExpect(redirectedUrl(MessageFormat.format("/rules/{0}",rule.getId())));

        verify(eventRuleService).save(eq(tenant),eq(rule));
    }

    @Test
    public void shouldShowRuleDetails() throws Exception {
        ruleForm.setAdditionalSupplier(null);

        ruleForm.setId(ruleId);
        rule.setId(ruleId);
        when(eventRuleService.getById(tenant, rule.getId())).thenReturn(
                ServiceResponse.<EventRule>builder().result(rule).status(ServiceResponse.Status.OK).<EventRule>build());

        getMockMvc().perform(
            get("/rules/{0}",rule.getId())
        ).andExpect(model().attribute("rule",equalTo(ruleForm)))
         .andExpect(view().name("rules/show"));

        verify(eventRuleService).getById(tenant, rule.getId());
    }

    @Configuration
    static class EventRuleTestContextConfig {
        @Bean
        public EventRuleService eventRuleService() {
            return Mockito.mock(EventRuleService.class);
        }
        @Bean
        public DeviceRegisterService deviceRegisterService() {
            return mock(DeviceRegisterService.class);
        }
    }
}
