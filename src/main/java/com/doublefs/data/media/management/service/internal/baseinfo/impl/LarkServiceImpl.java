package com.doublefs.data.media.management.service.internal.baseinfo.impl;

import com.doublefs.data.common.log.constant.TraceLogConstants;
import com.doublefs.data.common.mvc.util.HttpUtil;
import com.doublefs.data.common.util.JsonUtil;
import com.doublefs.data.media.management.model.rpc.dto.CheckImageResizeStatusMessage;
import com.doublefs.data.media.management.model.rpc.vo.larkmessage.ChildrenContent;
import com.doublefs.data.media.management.model.rpc.vo.larkmessage.Content;
import com.doublefs.data.media.management.model.rpc.vo.larkmessage.LarkMessage;
import com.doublefs.data.media.management.model.rpc.vo.larkmessage.Post;
import com.doublefs.data.media.management.model.rpc.vo.larkmessage.ZhCn;
import com.doublefs.data.media.management.service.internal.baseinfo.LarkService;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.doublefs.data.media.management.common.constants.Constants.MALL_PRODUCT_IMG_BUCKET;
import static com.doublefs.data.media.management.common.constants.Constants.MALL_PRODUCT_IMG_RESIZED_BUCKET;

/**
 * 飞书通知
 *
 * @author wangky
 * @date 2021/03/15 15:47
 **/
@Service
public class LarkServiceImpl implements LarkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LarkServiceImpl.class);

    private static final String IMG_RESIZE_1400 = "-1400x.jpg";
    private static final String IMG_RESIZE_750 = "-750x.jpg";
    private static final String IMG_RESIZE_342 = "-342x.jpg";
    private static final String IMG_RESIZE_100 = "-100x.jpg";
    
    @Value("${api.onlineRobot}")
    private String onlineRobot;
    @Override
    public void imgResizeFailNotify(CheckImageResizeStatusMessage checkImageResizeStatusMessage) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        
        Headers headers = null;
        Headers.Builder builder = new Headers.Builder();
        builder.add(TraceLogConstants.NGX_REQUEST_ID, "traceId");
        headers = builder.build();
        try {
            LarkMessage larkMessage = new LarkMessage();
            larkMessage.setMsg_type("post");
            Content content = new Content();
            Post post = new Post();
            ZhCn zhCn = new ZhCn();
            zhCn.setTitle("图片"+checkImageResizeStatusMessage.getImageId()+"上传10秒后,未检测到缩略图，如果以下缩略图可以访问，说明生成延迟，请忽略该通知");
            //设置桶名称
            List<List<ChildrenContent>> childrenContent = new ArrayList<>();
            setBucketContent(checkImageResizeStatusMessage, childrenContent);
            //设置国内图片地址
            setCnImgUrl(childrenContent, "S3国内原图地址：", checkImageResizeStatusMessage.getCnUrl());
            //设置国外图片地址
            setUsImgUrl(checkImageResizeStatusMessage, childrenContent);

            //设置resize1400x地址
            setUsResizeImg1400xUrl(checkImageResizeStatusMessage, childrenContent);

            //设置resize750x地址
            setUsResizeImg750xUrl(checkImageResizeStatusMessage, childrenContent);

            //设置resize342x地址
            setUsResizeImg342xUrl(checkImageResizeStatusMessage, childrenContent);

            //设置resize100x地址
            setUsResizeImg100xUrl(checkImageResizeStatusMessage, childrenContent);

            zhCn.setContent(childrenContent);
            post.setZh_cn(zhCn);
            
            content.setPost(post);
            larkMessage.setContent(content);
            Response response = HttpUtil.doPostJson(client, onlineRobot, JsonUtil.toJson(larkMessage), headers);
            System.out.println(response);
        } catch (IOException e) {
            LOGGER.error(" LarkServiceImpl onlineNotify 请求错误{}", e);
        }
    }

    private void setUsResizeImg100xUrl(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentUs342xFilePath = new ArrayList<>();
        ChildrenContent s3Us342xFilePathContentKey = new ChildrenContent();
        s3Us342xFilePathContentKey.setTag("text");
        s3Us342xFilePathContentKey.setUn_escape(true);
        s3Us342xFilePathContentKey.setText("S3国际100x地址：");
        childrenContentUs342xFilePath.add(s3Us342xFilePathContentKey);
        ChildrenContent s3Us342xFilePathContentValue = new ChildrenContent();
        s3Us342xFilePathContentValue.setTag("a");
        s3Us342xFilePathContentValue.setText(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_100).replace(".png",IMG_RESIZE_100).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        s3Us342xFilePathContentValue.setHref(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_100).replace(".png",IMG_RESIZE_100).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        childrenContentUs342xFilePath.add(s3Us342xFilePathContentValue);
        childrenContent.add(childrenContentUs342xFilePath);
    }
    private void setUsResizeImg342xUrl(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentUs342xFilePath = new ArrayList<>();
        ChildrenContent s3Us342xFilePathContentKey = new ChildrenContent();
        s3Us342xFilePathContentKey.setTag("text");
        s3Us342xFilePathContentKey.setUn_escape(true);
        s3Us342xFilePathContentKey.setText("S3国际342x地址：");
        childrenContentUs342xFilePath.add(s3Us342xFilePathContentKey);
        ChildrenContent s3Us342xFilePathContentValue = new ChildrenContent();
        s3Us342xFilePathContentValue.setTag("a");
        s3Us342xFilePathContentValue.setText(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_342).replace(".png",IMG_RESIZE_342).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        s3Us342xFilePathContentValue.setHref(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_342).replace(".png",IMG_RESIZE_342).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        childrenContentUs342xFilePath.add(s3Us342xFilePathContentValue);
        childrenContent.add(childrenContentUs342xFilePath);
    }

    private void setUsResizeImg750xUrl(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentUs750xFilePath = new ArrayList<>();
        ChildrenContent s3Us750xFilePathContentKey = new ChildrenContent();
        s3Us750xFilePathContentKey.setTag("text");
        s3Us750xFilePathContentKey.setUn_escape(true);
        s3Us750xFilePathContentKey.setText("S3国际750x地址：");
        childrenContentUs750xFilePath.add(s3Us750xFilePathContentKey);
        ChildrenContent s3Us750xFilePathContentValue = new ChildrenContent();
        s3Us750xFilePathContentValue.setTag("a");
        s3Us750xFilePathContentValue.setText(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_750).replace(".png",IMG_RESIZE_750).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        s3Us750xFilePathContentValue.setHref(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_750).replace(".png",IMG_RESIZE_750).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        childrenContentUs750xFilePath.add(s3Us750xFilePathContentValue);
        childrenContent.add(childrenContentUs750xFilePath);
    }

    private void setUsResizeImg1400xUrl(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentUs1400xFilePath = new ArrayList<>();
        ChildrenContent s3Us1400xFilePathContentKey = new ChildrenContent();
        s3Us1400xFilePathContentKey.setTag("text");
        s3Us1400xFilePathContentKey.setUn_escape(true);
        s3Us1400xFilePathContentKey.setText("S3国际1400x地址：");
        childrenContentUs1400xFilePath.add(s3Us1400xFilePathContentKey);
        ChildrenContent s3Us1400xFilePathContentValue = new ChildrenContent();
        s3Us1400xFilePathContentValue.setTag("a");
        s3Us1400xFilePathContentValue.setText(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_1400).replace(".png",IMG_RESIZE_1400).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        s3Us1400xFilePathContentValue.setHref(checkImageResizeStatusMessage.getUsUrl().replace(".jpg",IMG_RESIZE_1400).replace(".png",IMG_RESIZE_1400).replace(MALL_PRODUCT_IMG_BUCKET,MALL_PRODUCT_IMG_RESIZED_BUCKET));
        childrenContentUs1400xFilePath.add(s3Us1400xFilePathContentValue);
        childrenContent.add(childrenContentUs1400xFilePath);
    }

    private void setUsImgUrl(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentUsFilePath = new ArrayList<>();
        ChildrenContent s3UsFilePathContentKey = new ChildrenContent();
        s3UsFilePathContentKey.setTag("text");
        s3UsFilePathContentKey.setUn_escape(true);
        s3UsFilePathContentKey.setText("S3国际原图地址：");
        childrenContentUsFilePath.add(s3UsFilePathContentKey);
        ChildrenContent s3UsFilePathContentValue = new ChildrenContent();
        s3UsFilePathContentValue.setTag("a");
        s3UsFilePathContentValue.setText(checkImageResizeStatusMessage.getUsUrl());
        s3UsFilePathContentValue.setHref(checkImageResizeStatusMessage.getUsUrl());
        childrenContentUsFilePath.add(s3UsFilePathContentValue);
        childrenContent.add(childrenContentUsFilePath);
    }

    private void setCnImgUrl(List<List<ChildrenContent>> childrenContent, String s, String cnUrl) {
        List<ChildrenContent> childrenContentCnFilePath = new ArrayList<>();
        ChildrenContent s3CnFilePathContentKey = new ChildrenContent();
        s3CnFilePathContentKey.setTag("text");
        s3CnFilePathContentKey.setUn_escape(true);
        s3CnFilePathContentKey.setText(s);
        childrenContentCnFilePath.add(s3CnFilePathContentKey);
        ChildrenContent s3CnFilePathContentValue = new ChildrenContent();
        s3CnFilePathContentValue.setTag("a");
        s3CnFilePathContentValue.setText(cnUrl);
        s3CnFilePathContentValue.setHref(cnUrl);
        childrenContentCnFilePath.add(s3CnFilePathContentValue);
        childrenContent.add(childrenContentCnFilePath);
    }

    private void setBucketContent(CheckImageResizeStatusMessage checkImageResizeStatusMessage, List<List<ChildrenContent>> childrenContent) {
        List<ChildrenContent> childrenContentBucketName = new ArrayList<>();
        ChildrenContent bucketContentKey = new ChildrenContent();
        bucketContentKey.setTag("text");
        bucketContentKey.setUn_escape(true);
        bucketContentKey.setText("S3缩略图桶名：");
        childrenContentBucketName.add(bucketContentKey);
        ChildrenContent bucketContentValue = new ChildrenContent();
        bucketContentValue.setTag("text");
        bucketContentValue.setText(checkImageResizeStatusMessage.getBuckutName());
        childrenContentBucketName.add(bucketContentValue);
        childrenContent.add(childrenContentBucketName);
    }
}
