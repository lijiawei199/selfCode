package com.doublefs.data.media.management.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @Description
 * @ClassName KafkaProperties
 * @Author User
 * @date 2020.12.17 14:49
 */
@ConfigurationProperties("kafka.producer")
@Component("KafkaProducerProperties")
public class KafkaProducerProperties extends HashMap<String, Object> {
}
