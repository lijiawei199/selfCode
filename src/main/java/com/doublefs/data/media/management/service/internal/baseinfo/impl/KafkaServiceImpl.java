package com.doublefs.data.media.management.service.internal.baseinfo.impl;

import com.doublefs.data.common.enums.SystemError;
import com.doublefs.data.common.exception.ServiceException;
import com.doublefs.data.common.util.JsonUtil;
import com.doublefs.data.media.management.model.rpc.dto.CheckImageResizeStatusMessage;
import com.doublefs.data.media.management.service.impl.v1.image.ValidateImgResizeStatusService;
import com.doublefs.data.media.management.service.internal.baseinfo.KafkaService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * kafka消息发送消费服务实现
 *
 * @author wangky
 * @date 2021/03/15 10:50
 **/
@Service
public class KafkaServiceImpl implements KafkaService {


    private static Logger LOGGER = LoggerFactory.getLogger(KafkaServiceImpl.class);

    final KafkaProducer producer;
    final ValidateImgResizeStatusService validateImgResizeStatusService;

    public KafkaServiceImpl(KafkaProducer producer, ValidateImgResizeStatusService validateImgResizeStatusService) {
        this.producer = producer;
        this.validateImgResizeStatusService = validateImgResizeStatusService;
    }

    @Value("${kafka.topic.check_img_resized_status}")
    String topic;

    @Override
    public void sendCheckImgResizedStatusMessage(CheckImageResizeStatusMessage message) {
        // 异步发送消息
        String jsonMsg = null;
        try {
            jsonMsg = JsonUtil.toJson(message);
        } catch (IOException e) {
            LOGGER.error("json 转换异常{}", e);
            throw new ServiceException(SystemError.InternalError.getErrorCode(), SystemError.InternalError.getMessage());
        }
        producer.send(new ProducerRecord(topic, jsonMsg), new ValidateImageResizeStatusCallback(validateImgResizeStatusService, message));
    }
   
}
