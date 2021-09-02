package com.doublefs.data.media.management.service.impl.v1.image;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.doublefs.data.common.exception.ServiceException;
import com.doublefs.data.common.util.IdGeneratorUtil;
import com.doublefs.data.media.management.common.constants.Constants;
import com.doublefs.data.media.management.model.mysql.entity.Image;
import com.doublefs.data.media.management.model.mysql.entity.ImageExample;
import com.doublefs.data.media.management.model.mysql.mapper.ImageMapper;
import com.doublefs.data.media.management.model.rpc.dto.CheckImageResizeStatusMessage;
import com.doublefs.data.media.management.model.rpc.dto.UploadRepsResult;
import com.doublefs.data.media.management.model.rpc.dto.UploadResult;
import com.doublefs.data.media.management.model.rpc.dto.UploadResultAllUrl;
import com.doublefs.data.media.management.model.rpc.vo.image.FileUploadMetadataEntry;
import com.doublefs.data.media.management.model.rpc.vo.image.ImageReplaceUrlVO;
import com.doublefs.data.media.management.service.config.aws.CnAmazonS3Properties;
import com.doublefs.data.media.management.service.enums.GoodsCenterErrorCode;
import com.doublefs.data.media.management.service.impl.v1.util.AwsS3Util;
import com.doublefs.data.media.management.service.impl.v1.util.GeneratorUniqueKeyUtil;
import com.doublefs.data.media.management.service.impl.v1.util.ImageUtil;
import com.doublefs.data.media.management.service.internal.baseinfo.KafkaService;
import com.doublefs.data.media.management.service.internal.v1.image.AmazonS3ClientService;
import com.doublefs.data.media.management.service.internal.v1.image.ImageService;
import com.doublefs.data.media.management.service.utils.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.doublefs.data.media.management.common.constants.Constants.S3_CN_REGION;
import static com.doublefs.data.media.management.common.constants.Constants.S3_US_REGION;

/**
 * @author wangky
 * @date 2021/03/08 15:49
 **/
@Service
public class CnAmazonS3ClientServiceImpl implements AmazonS3ClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CnAmazonS3ClientServiceImpl.class);

    private static final String REGION_US = "us";
    private static final String REGION_CN = "cn";

    @Value(value = "${image.prifex.upload}")
    String UPLOAD_PATH_PRIFEX;
    private CnAmazonS3Properties cnAmazonS3Properties;
    @Resource(name = "goodscenterAawsStaticCredentialsProvider")
    private AWSStaticCredentialsProvider goodscenterAawsStaticCredentialsProvider;
    private UsAmazonS3ClientServiceImpl mallProductAmazonS3ClientService;
    @Autowired
    ImageMapper imageMapper;

    final ImageService imageService;
    final KafkaService kafkaService;


    @Autowired
    public CnAmazonS3ClientServiceImpl(//AWSStaticCredentialsProvider goodscenterAawsStaticCredentialsProvider, 
                                       CnAmazonS3Properties cnAmazonS3Properties,
                                       UsAmazonS3ClientServiceImpl mallProductAmazonS3ClientService, ImageService imageService, KafkaService kafkaService) {
        this.cnAmazonS3Properties = cnAmazonS3Properties;
        //this.goodscenterAawsStaticCredentialsProvider = goodscenterAawsStaticCredentialsProvider;
        this.mallProductAmazonS3ClientService = mallProductAmazonS3ClientService;
        this.imageService = imageService;
        this.kafkaService = kafkaService;
    }

    @Override
    public List<UploadRepsResult> uploadFileToS3(MultipartFile multipartFile) throws IOException {
        byte[] bytes = FileUtils.getBytesByStream(multipartFile.getInputStream());
        String fileType = FileUtils.getFileTypeByStream(bytes);

        //生成随机文件名
        String s3KeyName = ImageUtil.createS3FileKey(UPLOAD_PATH_PRIFEX, fileType, null, null);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(multipartFile.getContentType());
        objectMetadata.setContentLength(multipartFile.getSize());

        //上传文件
        PutObjectResult putObjectResult = uploadFileToS3Cn(s3KeyName, bytes, objectMetadata);
        List<UploadRepsResult> uploadRepsResults = new ArrayList<>();
        if (null != putObjectResult) {
            LOGGER.info("uploading file successfully, key: {} ", s3KeyName);
            UploadResult cnUploadResult = new UploadResult();
            //cnUploadResult.setImgRegion(S3_INTERNAL_REGION);
            String domainPath = AwsS3Util.getAwsS3FilePath(Constants.GOODS_CENTER_BUCKET, cnAmazonS3Properties.getRegion(), "com.cn", s3KeyName);
            cnUploadResult.setUrl(domainPath);
            UploadResult imageInfo = ImageUtil.getImageInfo(FileUtils.getInputStreamByBytes(bytes), fileType, bytes.length);
            cnUploadResult.setHeight(imageInfo.getHeight());
            cnUploadResult.setWidth(imageInfo.getWidth());
            cnUploadResult.setSize(imageInfo.getSize());
            cnUploadResult.setType(imageInfo.getType());
            cnUploadResult.setUploaded("1");
            UploadRepsResult cnUploadRepsResult = new UploadRepsResult();
            cnUploadRepsResult.setUploadRegion(S3_CN_REGION);
            cnUploadRepsResult.setUploadResult(cnUploadResult);
            uploadRepsResults.add(cnUploadRepsResult);

            //上传文件到国际站
            UploadResult usUploadResult = mallProductAmazonS3ClientService.uploadFileToS3US(bytes, s3KeyName, objectMetadata);
            UploadRepsResult usUploadRepsResult = new UploadRepsResult();
            usUploadRepsResult.setUploadRegion(S3_US_REGION);
            usUploadRepsResult.setUploadResult(usUploadResult);
            uploadRepsResults.add(usUploadRepsResult);

            //保存图片地址
            insertImage(bytes, usUploadResult, domainPath);
        }
        return uploadRepsResults;
    }

    private PutObjectResult uploadFileToS3Cn(String s3KeyName, byte[] bytes, ObjectMetadata objectMetadata) {
        AmazonS3 s3Client = getAmazonS3();
        //设置文件上传对象
        PutObjectRequest request = null;
        try {
            request = new PutObjectRequest(Constants.GOODS_CENTER_BUCKET,
                    s3KeyName,
                    FileUtils.getInputStreamByBytes(bytes),
                    objectMetadata);

        } catch (AmazonServiceException | IOException ex) {
            LOGGER.error("uploading file with key: {} message:{}", s3KeyName, ex.getMessage());
        }
        // 设置公共读取
        request.withCannedAcl(CannedAccessControlList.PublicRead);
        // 上传文件
        PutObjectResult putObjectResult = s3Client.putObject(request);
        return putObjectResult;
    }

    @Override
    public UploadResult uploadFileToS3ByEditor(String region, MultipartFile multipartFile, List<FileUploadMetadataEntry> metadata) throws IOException {
        byte[] bytes = FileUtils.getBytesByStream(multipartFile.getInputStream());
        String fileType = FileUtils.getFileTypeByStream(bytes);

        //生成随机文件名
        String s3KeyName = ImageUtil.createS3FileKey(UPLOAD_PATH_PRIFEX, fileType, null, null);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(multipartFile.getContentType());
        objectMetadata.setContentLength(multipartFile.getSize());

        //上传文件
        PutObjectResult putObjectResult = uploadFileToS3Cn(s3KeyName, bytes, objectMetadata);

        if (null != putObjectResult) {
            LOGGER.info("uploading file successfully, key: {} ", s3KeyName);
            UploadResult cnUploadResult = new UploadResult();
            //cnUploadResult.setImgRegion(S3_INTERNAL_REGION);
            String domainPath = AwsS3Util.getAwsS3FilePath(Constants.GOODS_CENTER_BUCKET, cnAmazonS3Properties.getRegion(), "com.cn", s3KeyName);
            cnUploadResult.setUrl(domainPath);
            UploadResult imageInfo = ImageUtil.getImageInfo(FileUtils.getInputStreamByBytes(bytes), fileType, bytes.length);
            cnUploadResult.setHeight(imageInfo.getHeight());
            cnUploadResult.setWidth(imageInfo.getWidth());
            cnUploadResult.setSize(imageInfo.getSize());
            cnUploadResult.setType(imageInfo.getType());
            cnUploadResult.setUploaded("1");
            UploadRepsResult cnUploadRepsResult = new UploadRepsResult();
            cnUploadRepsResult.setUploadRegion(S3_CN_REGION);
            cnUploadRepsResult.setUploadResult(cnUploadResult);

            //上传文件到国际站
            UploadResult usUploadResult = mallProductAmazonS3ClientService.uploadFileToS3US(bytes, s3KeyName, objectMetadata);

            Image image = insertImage(bytes, usUploadResult, cnUploadResult.getUrl());

            CheckImageResizeStatusMessage message = new CheckImageResizeStatusMessage();
            message.setImageId(image.getImageId());
            message.setCnUrl(image.getCnUrl());
            message.setUsUrl(image.getUsUrl());
            message.setBuckutName(Constants.MALL_PRODUCT_IMG_RESIZED_BUCKET);
            message.setS3KeyName(s3KeyName);
            kafkaService.sendCheckImgResizedStatusMessage(message);

            if (REGION_US.equals(region)) {
                return usUploadResult;
            }
            if (REGION_CN.equals(region)) {
                return cnUploadResult;
            }
            if (ObjectUtils.isEmpty(region)) {
                return usUploadResult;
            }
        }
        return new UploadResult();
    }

    @Override
    public UploadResultAllUrl uploadFileToS3ByEditor2(MultipartFile multipartFile, Long productSpuId, String title) throws IOException {
        byte[] bytes = FileUtils.getBytesByStream(multipartFile.getInputStream());
        String fileType = FileUtils.getFileTypeByStream(bytes);

        //生成随机文件名
        String s3KeyName = ImageUtil.createS3FileKey(UPLOAD_PATH_PRIFEX, fileType, productSpuId, title);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(multipartFile.getContentType());
        objectMetadata.setContentLength(multipartFile.getSize());

        //上传文件
        PutObjectResult putObjectResult = uploadFileToS3Cn(s3KeyName, bytes, objectMetadata);
        UploadResultAllUrl uploadResultAllUrl = new UploadResultAllUrl();

        if (null != putObjectResult) {
            LOGGER.info("uploading file successfully, key: {} ", s3KeyName);
            //cnUploadResult.setImgRegion(S3_INTERNAL_REGION);
            String domainPath = AwsS3Util.getAwsS3FilePath(Constants.GOODS_CENTER_BUCKET, cnAmazonS3Properties.getRegion(), "com.cn", s3KeyName);
            uploadResultAllUrl.setUrl(domainPath);
            UploadResult imageInfo = ImageUtil.getImageInfo(FileUtils.getInputStreamByBytes(bytes), fileType, bytes.length);
            uploadResultAllUrl.setHeight(imageInfo.getHeight());
            uploadResultAllUrl.setWidth(imageInfo.getWidth());
            uploadResultAllUrl.setSize(imageInfo.getSize());
            uploadResultAllUrl.setType(imageInfo.getType());
            uploadResultAllUrl.setUploaded("1");

            //上传文件到国际站
            UploadResult usUploadResult = mallProductAmazonS3ClientService.uploadFileToS3US(bytes, s3KeyName, objectMetadata);

            Image image = insertImage(bytes, usUploadResult, uploadResultAllUrl.getUrl());

            CheckImageResizeStatusMessage message = new CheckImageResizeStatusMessage();
            message.setImageId(image.getImageId());
            message.setCnUrl(image.getCnUrl());
            message.setUsUrl(image.getUsUrl());
            message.setBuckutName(Constants.MALL_PRODUCT_IMG_RESIZED_BUCKET);
            message.setS3KeyName(s3KeyName);
            kafkaService.sendCheckImgResizedStatusMessage(message);
            //返回的us url替换为cdn地址
            String replaceUsUrl = usUploadResult.getUrl().replace("https://mall-product-img.s3.us-west-2.amazonaws.com/", "https://mpi.halaracdn.com/");
            uploadResultAllUrl.setUsUrl(replaceUsUrl);
        }
        return uploadResultAllUrl;
    }

    private Image insertImage(byte[] bytes, UploadResult usUploadResult, String url) throws IOException {
        //保存图片地址
        UploadResult imageInfo = ImageUtil.getImageInfo(bytes);
        Image image = new Image();
        Date date = new Date();
        image.setCnUrl(url);
        image.setUsUrl(usUploadResult.getUrl());
        image.setHeight(imageInfo.getHeight());
        image.setWidth(imageInfo.getWidth());
        image.setResizeStatus((byte) 2);
        String uniqueKey = GeneratorUniqueKeyUtil.md5(image.getCnUrl(), image.getUsUrl());
        image.setUniqueKey(uniqueKey);
        image.setCreatedAt(date);
        image.setUpdatedAt(date);
        image.setStatus((byte) 2);
        imageService.insert(image);
        return image;
    }

    private AmazonS3 getAmazonS3() {
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(cnAmazonS3Properties.getRegion())
                .withCredentials(goodscenterAawsStaticCredentialsProvider)
                .build();
    }

    @Override
    public S3Object downloadFileFromS3(String bucketName, String s3KeyName) {

        if (StringUtils.isEmpty(bucketName)) {
            bucketName = Constants.GOODS_CENTER_BUCKET;
        }
        AmazonS3 s3Client = getAmazonS3();

        LOGGER.info("Downloading %s from S3 bucket %s...\n", s3KeyName, bucketName);
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
        String title = replaceUrlVO.getTitle();
        Long productSpuId = replaceUrlVO.getProductSpuId();
        String fileName = title.replace(" ", "-") + "_" + productSpuId + "_" + RandomStringUtils.randomNumeric(10);
        ;

        // 1 copy cn bucket

        String cnUrl = replaceUrlVO.getUrl();

        String copyCnUrl = copyCnBucket(cnUrl, fileName);
        LOGGER.info("cn bucket copy success copyCnUrl{}", copyCnUrl);

        // 2 copy us bucket
        String usUrl = replaceUrlVO.getUsUrl();
        String copyUsUrl = copyUsBucket(usUrl, fileName);
        LOGGER.info("us bucket copy success copyUsUrl{}", copyUsUrl);


        try {
            // 3 send kafka msg
            String uniqueKey = GeneratorUniqueKeyUtil.md5(cnUrl, usUrl);
            ImageExample imageExample = new ImageExample();
            imageExample.createCriteria()
                    .andUniqueKeyEqualTo(uniqueKey);
            List<Image> images = imageMapper.selectByExample(imageExample);
            if (images.size() > 0) {
                Image image = images.get(0);
                CheckImageResizeStatusMessage message = new CheckImageResizeStatusMessage();
                message.setImageId(image.getImageId());
                message.setCnUrl(image.getCnUrl());
                message.setUsUrl(image.getUsUrl());
                message.setBuckutName(Constants.MALL_PRODUCT_IMG_RESIZED_BUCKET);
                String usDestinationKey = copyUsUrl.substring(usUrl.indexOf(".com")).substring(usUrl.substring(usUrl.indexOf(".com")).indexOf("/") + 1);

                message.setS3KeyName(usDestinationKey);
                kafkaService.sendCheckImgResizedStatusMessage(message);

                // 4 insert record
                image.setImageId(null);
                image.setCnUrl(copyCnUrl);
                image.setUsUrl(copyUsUrl);

                image.setUniqueKey(GeneratorUniqueKeyUtil.md5(copyCnUrl, copyUsUrl));
                int insert = imageMapper.insert(image);

            }
        } catch (DuplicateKeyException e) {
            LOGGER.error("uniqueKey conflict url{} usUrl{}", copyCnUrl, copyUsUrl);
        } finally {
            // finally return cnUrl
            UploadResultAllUrl uploadResultAllUrl = new UploadResultAllUrl();
            uploadResultAllUrl.setUrl(copyCnUrl);
            String replaceUsUrl = copyUsUrl.replace("https://mall-product-img.s3.us-west-2.amazonaws.com/", "https://mpi.halaracdn.com/");
            uploadResultAllUrl.setUsUrl(replaceUsUrl);

            return uploadResultAllUrl;
        }

    }

    private String copyCnBucket(String url, String fileName) {
        String cnSourceKey = url.substring(url.indexOf(".com")).substring(url.substring(url.indexOf(".com")).indexOf("/") + 1);
        String urlPrefix = url.substring(0, url.indexOf(cnSourceKey));
        String cnExpandedName = cnSourceKey.substring(cnSourceKey.lastIndexOf("."));
        String prefix = cnSourceKey.substring(0, cnSourceKey.lastIndexOf("/") + 1);
        String cnDestinationKey = prefix + fileName + cnExpandedName;
        this.copyImg(cnSourceKey, cnDestinationKey);
        String copyCnUrl = urlPrefix + cnDestinationKey;
        return copyCnUrl;
    }

    private String copyUsBucket(String usUrl, String fileName) {
        String usSourceKey = usUrl.substring(usUrl.indexOf(".com")).substring(usUrl.substring(usUrl.indexOf(".com")).indexOf("/") + 1);
        String urlPrefix = usUrl.substring(0, usUrl.indexOf(usSourceKey));
        String cnExpandedName = usSourceKey.substring(usSourceKey.lastIndexOf("."));
        String prefix = usSourceKey.substring(0, usSourceKey.lastIndexOf("/") + 1);
        String cnDestinationKey = prefix + fileName + cnExpandedName;
        mallProductAmazonS3ClientService.copyImg(usSourceKey, cnDestinationKey);
        String copyCnUrl = urlPrefix + cnDestinationKey;
        return copyCnUrl;
    }

    @Override
    public Boolean copyImg(String sourceKey, String destinationKey) {
        AmazonS3 amazonS3 = getAmazonS3();
        CopyObjectRequest copyObjRequest = new CopyObjectRequest(Constants.GOODS_CENTER_BUCKET, sourceKey, Constants.GOODS_CENTER_BUCKET, destinationKey);
        copyObjRequest.withCannedAccessControlList(CannedAccessControlList.PublicRead);
        CopyObjectResult copyObjectResult = amazonS3.copyObject(copyObjRequest);
        return Boolean.TRUE;
    }

    private String s3FileKey(String uploadPath, String originalFilename) {
        if (StringUtils.isEmpty(originalFilename) || !originalFilename.contains(".")) {
            throw new ServiceException(GoodsCenterErrorCode.UPLOAD_FILE_EXPANDEDNAME_NULL.getErrorCode(), GoodsCenterErrorCode.UPLOAD_FILE_EXPANDEDNAME_NULL.getMessage());
        }
        StringBuilder key = new StringBuilder();

        //设置文件目录，即文件key，规则：前缀+年+月+日+时+文件名+文件后缀
        if (StringUtils.isEmpty(uploadPath)) {
            uploadPath = UPLOAD_PATH_PRIFEX;
        }
        String s3FilePathPrefix = "/".equals(uploadPath.substring(uploadPath.length() - 1)) ? uploadPath : uploadPath + "/";
        key.append(s3FilePathPrefix);
        Date now = new Date();
        SimpleDateFormat simpleDateFormatYYYY = new SimpleDateFormat("yy");
        SimpleDateFormat simpleDateFormatMM = new SimpleDateFormat("MM");
        SimpleDateFormat simpleDateFormatDD = new SimpleDateFormat("dd");
        SimpleDateFormat simpleDateFormatHH = new SimpleDateFormat("HH");
        key.append(simpleDateFormatYYYY.format(now)).append("/");
        key.append(simpleDateFormatMM.format(now)).append("/");
        key.append(simpleDateFormatDD.format(now)).append("/");
        key.append(simpleDateFormatHH.format(now)).append("/");
        String expandedName = originalFilename.substring(originalFilename.lastIndexOf("."));
        key.append(IdGeneratorUtil.nextId());
        key.append(expandedName);
        return key.toString();
    }
}