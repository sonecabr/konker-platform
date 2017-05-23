package com.konkerlabs.platform.registry.api.model;

import com.konkerlabs.platform.registry.api.model.core.SerializableVO;
import com.konkerlabs.platform.registry.business.model.SilenceTrigger;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@ApiModel(
		value = "Silence Trigger",
		discriminator = "com.konkerlabs.platform.registry.api.model")
public class SilenceTriggerVO
                    extends SilenceTriggerInputVO
                    implements SerializableVO<SilenceTrigger, SilenceTriggerVO> {

    @ApiModelProperty(value = "the trigger guid", example = "39a35764-5134-4003-8f1e-400959631618", position = 0)
    private String guid;
    @ApiModelProperty(value = "the device model name of device", example = "PresenceSensor", position = 1)
    protected String deviceModelName;
    @ApiModelProperty(value = "the location name of device", example = "br_sp", position = 2)
    protected String locationName;

    public SilenceTriggerVO(SilenceTrigger silenceTrigger) {
        this.guid = silenceTrigger.getGuid();
        this.deviceModelName  = silenceTrigger.getDeviceModel().getName();
        this.locationName = silenceTrigger.getLocation().getName();
        this.minutes = silenceTrigger.getMinutes();
    }

    public SilenceTriggerVO apply(SilenceTrigger t) {
        return new SilenceTriggerVO(t);
    }

    @Override
    public SilenceTrigger patchDB(SilenceTrigger model) {
        model.setGuid(this.getGuid());
        model.setMinutes(this.getMinutes());

        return model;
    }

}
