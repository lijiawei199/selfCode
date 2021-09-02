package com.doublefs.data.media.management.service.internal.baseinfo.impl;

import com.doublefs.data.media.management.model.rpc.dto.CheckImageResizeStatusMessage;
import com.doublefs.data.media.management.service.impl.v1.image.ValidateImgResizeStatusService;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wangky
 * @date 2021/03/15 11:03
 **/
public class ValidateImageResizeStatusCallback implements Callback {
    final private static Logger LOGGER = LoggerFactory.getLogger(KafkaServiceImpl.class);

    final ValidateImgResizeStatusService validateImgResizeStatusService;
    final CheckImageResizeStatusMessage message;

    public ValidateImageResizeStatusCallback(ValidateImgResizeStatusService validateImgResizeStatusService, CheckImageResizeStatusMessage message) {
        this.validateImgResizeStatusService = validateImgResizeStatusService;
        this.message = message;
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            LOGGER.error("KafkaServiceImpl sendValidateImageResizeStatusMsg 发送消息失败{}", e.getMessage());
        } else {
            LOGGER.info("KafkaServiceImpl sendValidateImageResizeStatusMsg 发送消息成功:{},接收到返回结果：{} ", message.toString(), recordMetadata);
        }
    }
}
