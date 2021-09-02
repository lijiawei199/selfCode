package com.doublefs.data.media.management.service.impl.v1.image;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.doublefs.data.common.mvc.model.response.BaseResponseVO;
import com.doublefs.data.common.mvc.model.response.DataVO;
import com.doublefs.data.common.util.JacksonMapperUtil;
import com.doublefs.data.media.management.model.rpc.vo.image.ImageDownloadRequestVO;
import com.doublefs.data.media.management.service.impl.v1.util.ImageUtil;
import com.doublefs.data.media.management.service.internal.v1.image.ImageDownloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.amazonaws.services.s3.model.Permission.Read;

@Service
public class ImageDownloadServiceImpl implements ImageDownloadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDownloadServiceImpl.class);

    @Value(value = "${aws.s3.region}")
    private String awsS3Region;

    @Value(value = "${aws.s3.accessKeyId}")
    private String awsS3AccessKeyId;

    @Value(value = "${aws.s3.secretAccessKey}")
    private String awsSecretAccessKey;

    @Value(value = "${aws.s3.bucket}")
    private String awsS3Bucket;
    /**
     * 上传到文件返回一个文件储存后的路径
     * @param multipartFile
     * @return
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile multipartFile)  {
        if (multipartFile.isEmpty()) {
            return "文件为空";
        }
        Date date = new Date();
        SimpleDateFormat formatter_yyyy = new SimpleDateFormat("yyyy");
        SimpleDateFormat formatter_MM = new SimpleDateFormat("MM");
        SimpleDateFormat formatter_DD = new SimpleDateFormat("dd");
        SimpleDateFormat formatter_HH = new SimpleDateFormat("HH24");
        //在随机名前加上年月
        String s3FilePath ="upload" + "/" +formatter_yyyy.format(date) + "/" + formatter_MM.format(date) + "/" + multipartFile.getOriginalFilename();
        ObjectMetadata metadata  = new ObjectMetadata();
        metadata.setContentType(multipartFile.getContentType());
        metadata.setContentLength(multipartFile.getSize());
        try {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                    awsS3AccessKeyId, awsSecretAccessKey);

            AmazonS3 s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(awsS3Region)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .build();
            //开始上传文件
            PutObjectResult putObjectResult=s3Client.putObject(awsS3Bucket, s3FilePath, multipartFile.getInputStream(), metadata);
            System.err.println("上传完成__文件位置为" + putObjectResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回文件位置
        return s3FilePath;
    }

    
    @Override
    public BaseResponseVO<ObjectNode> downloadToS3(ImageDownloadRequestVO requestVO) throws Exception {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                awsS3AccessKeyId, awsSecretAccessKey);

        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(awsS3Region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();

        ConcurrentHashMap<String, String> urlMap = new ConcurrentHashMap<>();

        requestVO.getUrlList().parallelStream().forEach(
                imageUrl -> {
                    LOGGER.info("start upload: {}", imageUrl);

                    Path originImageFile = null;
                    Path targetImageFile = null;
                    try {
                        originImageFile = Files.createTempFile(UUID.randomUUID().toString(), null);
                        targetImageFile = Files.createTempFile(UUID.randomUUID().toString(), null);

                        // download to local file
                        URL url = new URL(imageUrl);

                        try (InputStream stream = url.openStream()) {
                            Files.copy(stream, originImageFile, StandardCopyOption.REPLACE_EXISTING);
                        }

                        // image type to png
                        ImageUtil.imageTypeConvert(
                                originImageFile.toFile(),
                                targetImageFile.toFile(), "png");

                        String s3Key = ImageUtil.createS3Key(
                                "upload",
                                requestVO.getUpdatedDateTime(),
                                imageUrl,
                                "png");

                        // upload to s3
                        PutObjectRequest putObjectRequest = new PutObjectRequest(this.awsS3Bucket, s3Key, targetImageFile.toFile());

                        AccessControlList accessControlList = new AccessControlList();
                        //all users or authenticated
                        accessControlList.grantPermission(GroupGrantee.AllUsers, Read);

                        putObjectRequest.setAccessControlList(accessControlList);

                        // 访问访问自动变成下载链接
                        ObjectMetadata objectMetadata = new ObjectMetadata();
                        objectMetadata.setHeader("Content-Type", "image/png");

                        putObjectRequest.setMetadata(objectMetadata);

                        s3Client.putObject(putObjectRequest);

                        // s3 key -> s3 url
                        String s3Url = ImageUtil.createS3Url(this.awsS3Region, this.awsS3Bucket, s3Key);

                        urlMap.put(imageUrl, s3Url);

                        LOGGER.info("finish upload: {}", s3Url);
                    } catch (Exception e) {
                        LOGGER.error("upload failed: {}, {}", imageUrl, e);
                    } finally {
                        // clean tmp file
                        if (originImageFile != null) {
                            try {
                                Files.delete(originImageFile);
                            } catch (IOException e) {
                                LOGGER.error("delete tmp file failed: {}", originImageFile);
                            }
                        }

                        if (targetImageFile != null) {
                            try {
                                Files.delete(targetImageFile);
                            } catch (IOException e) {
                                LOGGER.error("delete tmp file failed: {}", targetImageFile);
                            }
                        }
                    }
                });

        List<ObjectNode> result = new ArrayList<>();

        ObjectMapper objectMapper = JacksonMapperUtil.getInstance().getObjectMapper();
        result.add(objectMapper.valueToTree(urlMap));

        return BaseResponseVO.succeed(new DataVO<>((long) urlMap.size(), result));
    }
}
