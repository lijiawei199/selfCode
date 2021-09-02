package com.doublefs.data.media.management.service.impl.v1.util;


import static com.doublefs.data.media.management.common.constants.Constants.S3_AMAZONAWS_DOMAIN_FORMAT;

public class AwsS3Util {
    
    /**
     * 拼接获取s3上的文件绝对路径
     *
     * @param bucket        桶名
     * @param domainSuffix  域名后缀，区分国内外地址
     * @param s3FileName    文件名key，包含路径+文件名
     * @return
     */
    public static String getAwsS3FilePath(String bucket, String region, String domainSuffix, String s3FileName) {

        return String.format(S3_AMAZONAWS_DOMAIN_FORMAT, bucket, region, domainSuffix, s3FileName);
    }
    
}
