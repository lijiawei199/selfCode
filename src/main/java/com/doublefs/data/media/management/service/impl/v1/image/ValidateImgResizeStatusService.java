package com.doublefs.data.media.management.service.impl.v1.image;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.doublefs.data.media.management.model.rpc.dto.CheckImageResizeStatusMessage;
import com.doublefs.data.media.management.service.config.aws.UsAmazonS3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangky
 * @date 2021/03/08 15:49
 **/
@Service
public class ValidateImgResizeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateImgResizeStatusService.class);
    private UsAmazonS3Properties usAmazonS3Properties;
    @Resource(name = "mallProductAwsStaticCredentialsProvider")
    private AWSStaticCredentialsProvider mallProductAwsStaticCredentialsProvider;

    private static final String IMG_RESIZE_1400 = "-1400x";
    private static final String IMG_RESIZE_750 = "-750x";
    private static final String IMG_RESIZE_342 = "-342x";
    private static final String IMG_RESIZE_100 = "-100x";

    @Autowired
    public ValidateImgResizeStatusService(//AWSStaticCredentialsProvider awsStaticCredentialsProvider, 
                                          UsAmazonS3Properties usAmazonS3Properties) {
        this.usAmazonS3Properties = usAmazonS3Properties;
        //this.mallProductAwsStaticCredentialsProvider = awsStaticCredentialsProvider;
    }

    public boolean validateImgResizeStatus(CheckImageResizeStatusMessage message) {
        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(usAmazonS3Properties.getRegion())
                .withCredentials(mallProductAwsStaticCredentialsProvider)
                .build();
        try {
            String s3KeyName = message.getS3KeyName().substring(0, message.getS3KeyName().lastIndexOf("."));

            Thread.sleep(10000);
            ListObjectsV2Result listObjectsV2Result = s3Client.listObjectsV2(message.getBuckutName(), s3KeyName);
            if (!ObjectUtils.isEmpty(listObjectsV2Result)) {
                List<S3ObjectSummary> objectSummaries = listObjectsV2Result.getObjectSummaries();
                List<String> resizes = new ArrayList<>();
                for (S3ObjectSummary os : objectSummaries) {
                    resizes.add(os.getKey().substring(0,os.getKey().lastIndexOf(".")));
                }
                if (!CollectionUtils.isEmpty(resizes)) {
                    return resizes.contains(s3KeyName.concat(IMG_RESIZE_1400))
                            && resizes.contains(s3KeyName.concat(IMG_RESIZE_750))
                            && resizes.contains(s3KeyName.concat(IMG_RESIZE_342))
                            && resizes.contains(s3KeyName.concat(IMG_RESIZE_100));
                }
                return false;
            }

        } catch (AmazonServiceException e) {
            LOGGER.error("validateImgResizeStatus fail,{}", e.getErrorMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error("validateImgResizeStatus fail,{}", e.getMessage(), e);
        }
        return false;
    }
}