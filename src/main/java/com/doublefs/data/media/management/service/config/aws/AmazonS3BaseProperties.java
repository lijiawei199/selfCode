package com.doublefs.data.media.management.service.config.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3的基础信息
 *
 * @author wangky
 * @date 2021/03/08 16:06
 **/
public class AmazonS3BaseProperties {
    private String region;
    private String accessKeyId;
    private String secretAccessKey;

    public String getRegion() {
        return region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}
