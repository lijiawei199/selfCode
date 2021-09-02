package com.doublefs.data.media.management.service.impl.v1.util;

import com.doublefs.data.common.exception.ServiceException;
import com.doublefs.data.media.management.model.rpc.dto.UploadResult;
import com.doublefs.data.media.management.service.enums.GoodsCenterErrorCode;
import com.doublefs.data.media.management.service.utils.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * @author wangky
 */
public class ImageUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "HH/dd/MM/yyyy", Locale.ENGLISH);

    public static String createS3Key(String prefix, ZonedDateTime imageCreatedDateTime, String url, String imageType) throws NoSuchAlgorithmException {
        return String.format("%s/%s/%s.%s",
                prefix,
                DATE_FORMATTER.format(imageCreatedDateTime),
                HashUtil.md5(url),
                imageType);
    }

    public static String createS3Url(String region, String bucket, String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com.cn/%s", bucket, region, s3Key);
    }

    public static void imageTypeConvert(File infile, File outfile, String outType) throws IOException {
        // detect image type
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(infile);
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);

        String inType = null;
        while (imageReaders.hasNext()) {
            ImageReader reader = imageReaders.next();
            inType = reader.getFormatName();
        }
        imageInputStream.close();

        // parse input
        FileImageInputStream inputStream = new FileImageInputStream(infile);

        ImageReader inputReader = null;
        Iterator<ImageReader> inputIterator = ImageIO.getImageReadersByFormatName(inType);
        if (inputIterator.hasNext()) {
            inputReader = inputIterator.next();
        }
        inputReader.setInput(inputStream);

        // output
        ImageWriter writer = null;
        Iterator<ImageWriter> writerIterator = ImageIO.getImageWritersByFormatName(outType);
        if (writerIterator.hasNext()) {
            writer = writerIterator.next();
        }

        FileImageOutputStream outputStream = new FileImageOutputStream(outfile);
        writer.setOutput(outputStream);
        BufferedImage br = inputReader.read(0);
        writer.write(br);

        inputStream.close();
        outputStream.close();
    }

    public static String createS3FileKey(String uploadPath, String fileType, Long productSpuId, String title) {
        if (StringUtils.isEmpty(fileType)) {
            throw new ServiceException(GoodsCenterErrorCode.UPLOAD_FILE_EXPANDEDNAME_NULL.getErrorCode(), GoodsCenterErrorCode.UPLOAD_FILE_EXPANDEDNAME_NULL.getMessage());
        }
        StringBuilder key = new StringBuilder();

        //设置文件目录，即文件key，规则：前缀+年+月+日+时+文件名+文件后缀
        if (StringUtils.isEmpty(uploadPath)) {
            uploadPath = "upload";
        }
        String s3FilePathPrefix = "/".equals(uploadPath.substring(uploadPath.length() - 1)) ? uploadPath : uploadPath + "/";
        key.append(s3FilePathPrefix);
        Date now = new Date();
        SimpleDateFormat simpleDateFormatYYYY = new SimpleDateFormat("yy");
        SimpleDateFormat simpleDateFormatMM = new SimpleDateFormat("MM");
        SimpleDateFormat simpleDateFormatDD = new SimpleDateFormat("dd");
        SimpleDateFormat simpleDateFormatHH = new SimpleDateFormat("HH");
        SimpleDateFormat simpleDateFormatmm = new SimpleDateFormat("mm");
        key.append(simpleDateFormatmm.format(now)).append("/");
        key.append(simpleDateFormatHH.format(now)).append("/");
        key.append(simpleDateFormatDD.format(now)).append("/");
        key.append(simpleDateFormatMM.format(now)).append("/");
        key.append(simpleDateFormatYYYY.format(now)).append("/");

        if (null != title) {
            title = title.replace(" ", "-");
            key.append(title);
        }

        if (null != productSpuId) {
            key.append("_" + productSpuId);
        }

        key.append("_" + RandomStringUtils.randomNumeric(10));
        key.append("." + fileType);
        return key.toString();
    }

    public static UploadResult getImageInfo(InputStream inputStream, String fileType, long length) {
        UploadResult uploadResult = new UploadResult();
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) { //如果image=null 表示上传的不是图片格式
                return null;
            }
            uploadResult.setHeight(image.getHeight());
            uploadResult.setWidth(image.getWidth());
            uploadResult.setSize(length);
            uploadResult.setType(fileType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uploadResult;
    }

    public static UploadResult getImageInfo(byte[] bytes) throws IOException {
        String fileTypeByStream = FileUtils.getFileTypeByStream(bytes);
        ByteArrayInputStream inputStreamByBytes = FileUtils.getInputStreamByBytes(bytes);
        return getImageInfo(inputStreamByBytes, fileTypeByStream, bytes.length);
    }
}
