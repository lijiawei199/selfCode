package com.doublefs.data.media.management.service.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @Description
 * @ClassName KafkaConfig
 * @Author lijiawei
 * @date 2020.12.16 17:11
 */
@Configuration
public class KafkaConfig {

    @Autowired
    KafkaProducerProperties kafkaProducerProperties;

    @Bean
    public KafkaProducer kafkaProducer() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.get("bootstrapServers"));
        // key.deserializer 消息key序列化方式
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerProperties.get("keySerializer"));
        // value.deserializer 消息体序列化方式
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerProperties.get("valueSerializer"));
        properties.put(ProducerConfig.ACKS_CONFIG, kafkaProducerProperties.get("acks").toString());
        properties.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerProperties.get("retries"));
        // 2. 创建生产者实例
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        return producer;
    }
}
