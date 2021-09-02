package com.doublefs.data.media.management.service.config.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 国内s3属性
 *
 * @author wangky
 * @date 2021/03/11 11:07
 **/
@Component
@ConfigurationProperties(prefix = "aws.s3.goodscenter")
public class CnAmazonS3Properties extends AmazonS3BaseProperties {
    
}
