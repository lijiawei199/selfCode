package com.doublefs.data.media.management.service.impl.v1.image;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.doublefs.data.media.management.common.constants.Constants;
import com.doublefs.data.media.management.model.rpc.dto.UploadRepsResult;
import com.doublefs.data.media.management.model.rpc.dto.UploadResult;
import com.doublefs.data.media.management.model.rpc.dto.UploadResultAllUrl;
import com.doublefs.data.media.management.model.rpc.vo.image.FileUploadMetadataEntry;
import com.doublefs.data.media.management.model.rpc.vo.image.ImageReplaceUrlVO;
import com.doublefs.data.media.management.service.config.aws.UsAmazonS3Properties;
import com.doublefs.data.media.management.service.impl.v1.util.AwsS3Util;
import com.doublefs.data.media.management.service.impl.v1.util.ImageUtil;
import com.doublefs.data.media.management.service.internal.v1.image.AmazonS3ClientService;
import com.doublefs.data.media.management.service.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.doublefs.data.media.management.common.constants.Constants.S3_CN_REGION;
import static com.doublefs.data.media.management.common.constants.Constants.S3_US_REGION;
import static com.doublefs.data.media.management.common.constants.Constants.UPLOAD;

/**
 * @author wangky
 * @date 2021/03/08 15:49
 **/
@Service
public class UsAmazonS3ClientServiceImpl implements AmazonS3ClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsAmazonS3ClientServiceImpl.class);

    private static final String IMG_RESIZE_1400 = "-1400x";
    private static final String IMG_RESIZE_750 = "-750x";
    private static final String IMG_RESIZE_342 = "-342x";

    private UsAmazonS3Properties usAmazonS3Properties;
    @Resource(name = "mallProductAwsStaticCredentialsProvider")
    private AWSStaticCredentialsProvider mallProductAwsStaticCredentialsProvider;

    @Autowired
    public UsAmazonS3ClientServiceImpl(//AWSStaticCredentialsProvider mallProductAwsStaticCredentialsProvider, 
                                       UsAmazonS3Properties usAmazonS3Properties) {
        this.usAmazonS3Properties = usAmazonS3Properties;
        //this.mallProductAwsStaticCredentialsProvider = mallProductAwsStaticCredentialsProvider;
    }

    @Override
    public List<UploadRepsResult> uploadFileToS3(MultipartFile multipartFile) throws IOException {

        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(usAmazonS3Properties.getRegion())
                .withCredentials(mallProductAwsStaticCredentialsProvider)
                .build();
        byte[] bytesByStream = FileUtils.getBytesByStream(multipartFile.getInputStream());
        String fileType = FileUtils.getFileTypeByStream(bytesByStream);

        //生成随机文件名
        String s3KeyName = ImageUtil.createS3FileKey(UPLOAD, fileType, null, null);
        //设置文件上传对象
        PutObjectRequest request = null;
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(multipartFile.getContentType());
            objectMetadata.setContentLength(multipartFile.getSize());
            request = new PutObjectRequest(Constants.MALL_PRODUCT_IMG_BUCKET,
                    s3KeyName,
                    multipartFile.getInputStream(),
                    objectMetadata);

        } catch (IOException | AmazonServiceException ex) {
            LOGGER.error("uploading file with key: {} message:{}", multipartFile.getOriginalFilename(), ex.getMessage());
        }
        // 设置公共读取
        request.withCannedAcl(CannedAccessControlList.PublicRead);
        // 上传文件
        PutObjectResult putObjectResult = s3Client.putObject(request);
        List<UploadRepsResult> uploadRepsResults = new ArrayList<>();
        if (null != putObjectResult) {
            LOGGER.info("uploading file successfully, key: {} ", s3KeyName);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            UploadResult uploadResult = new UploadResult();
            String domainPath = AwsS3Util.getAwsS3FilePath(Constants.MALL_PRODUCT_IMG_BUCKET, usAmazonS3Properties.getRegion(), "com", s3KeyName);
            uploadResult.setUrl(domainPath);
            UploadRepsResult uploadRepsResult = new UploadRepsResult();
            uploadRepsResult.setUploadRegion(S3_CN_REGION);
            uploadRepsResult.setUploadResult(uploadResult);
            //validateImgResizeStatus(Constants.MALL_PRODUCT_IMG_RESIZED_BUCKET, s3KeyName.substring(0,s3KeyName.lastIndexOf(".")));
        }

        return uploadRepsResults;
    }

    @Override
    public UploadResult uploadFileToS3ByEditor(String region, MultipartFile multipartFile, List<FileUploadMetadataEntry> metadata) {
        return null;
    }

    @Override
    public UploadResultAllUrl uploadFileToS3ByEditor2(MultipartFile multipartFile, Long productSpuId, String title) throws IOException {
        return null;
    }

    public UploadResult uploadFileToS3US(byte[] bytes, String s3KeyName, ObjectMetadata objectMetadata) throws IOException {
        AmazonS3 s3Client = getAmazonS3();
        //设置文件上传对象
        PutObjectRequest request = null;
        try {
            request = new PutObjectRequest(Constants.MALL_PRODUCT_IMG_BUCKET,
                    s3KeyName,
                    FileUtils.getInputStreamByBytes(bytes),
                    objectMetadata);

        } catch (IOException | AmazonServiceException ex) {
            LOGGER.error("uploading file with key: {} message:{}", s3KeyName, ex.getMessage());
        }
        // 设置公共读取
        request.withCannedAcl(CannedAccessControlList.PublicRead);
        // 上传文件
        PutObjectResult putObjectResult = s3Client.putObject(request);
        UploadRepsResult uploadRepsResult = new UploadRepsResult();
        UploadResult uploadResult = new UploadResult();
        if (null != putObjectResult) {
            LOGGER.info("uploading file successfully to International station , key: {} ", s3KeyName);
            String domainPath = AwsS3Util.getAwsS3FilePath(Constants.MALL_PRODUCT_IMG_BUCKET, usAmazonS3Properties.getRegion(), "com", s3KeyName);
            uploadResult.setUrl(domainPath);
            UploadResult imageInfo = ImageUtil.getImageInfo(bytes);
            uploadResult.setHeight(imageInfo.getHeight());
            uploadResult.setWidth(imageInfo.getWidth());
            uploadResult.setSize(imageInfo.getSize());
            uploadResult.setType(imageInfo.getType());
            uploadResult.setUploaded("1");
            uploadRepsResult.setUploadRegion(S3_US_REGION);
            return uploadResult;
        }
        return uploadResult;
    }

    private AmazonS3 getAmazonS3() {
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(usAmazonS3Properties.getRegion())
                .withCredentials(mallProductAwsStaticCredentialsProvider)
                .build();
    }

    @Override
    public S3Object downloadFileFromS3(String bucketName, String s3KeyName) {

        if (StringUtils.isEmpty(bucketName)) {
            bucketName = Constants.MALL_PRODUCT_IMG_BUCKET;
        }
        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(usAmazonS3Properties.getRegion())
                .withCredentials(mallProductAwsStaticCredentialsProvider)
                .build();

        System.out.format("Downloading %s from S3 bucket %s...\n", s3KeyName, bucketName);
        S3ObjectInputStream s3is = null;
        try {
            S3Object s3Object = s3Client.getObject(bucketName, s3KeyName);
            return s3Object;
        } catch (AmazonServiceException e) {
            LOGGER.error("download file fail,{}", e.getErrorMessage(), e);
        }
        return null;
    }

    @Override
    public boolean reUploadImageToUs(List<String> urls) {
        return false;
    }

    @Override
    public UploadResultAllUrl replaceUrl(ImageReplaceUrlVO replaceUrlVO) {
        return null;
    }

    @Override
    public Boolean copyImg(String sourceKey, String destinationKey) {
        AmazonS3 amazonS3 = getAmazonS3();
        CopyObjectRequest copyObjRequest = new CopyObjectRequest(Constants.MALL_PRODUCT_IMG_BUCKET, sourceKey, Constants.MALL_PRODUCT_IMG_BUCKET, destinationKey);
        copyObjRequest.withCannedAccessControlList(CannedAccessControlList.PublicRead);
        CopyObjectResult copyObjectResult = amazonS3.copyObject(copyObjRequest);
        return Boolean.TRUE;
    }

    private boolean validateImgResizeStatus(String bucketName, String s3KeyName) {
        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(usAmazonS3Properties.getRegion())
                .withCredentials(mallProductAwsStaticCredentialsProvider)
                .build();
        try {
            ListObjectsV2Result listObjectsV2Result = s3Client.listObjectsV2(bucketName, s3KeyName);
            if (!ObjectUtils.isEmpty(listObjectsV2Result)) {
                List<S3ObjectSummary> objectSummaries = listObjectsV2Result.getObjectSummaries();
                List<String> resizes = new ArrayList<>();
                for (S3ObjectSummary os : objectSummaries) {
                    resizes.add(os.getKey());
                }
                if (!CollectionUtils.isEmpty(resizes)) {
                    return resizes.contains(s3KeyName.concat(IMG_RESIZE_1400))
                            && resizes.contains(s3KeyName.concat(IMG_RESIZE_750))
                            && resizes.contains(s3KeyName.concat(IMG_RESIZE_342));
                }
                return false;
            }
        } catch (AmazonServiceException e) {
            LOGGER.error("download file fail,{}", e.getErrorMessage(), e);
        }
        return false;
    }

}