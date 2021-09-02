package com.doublefs.data.media.management.service.config.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化S3相关类
 *
 * @author wangky
 * @date 2021/03/08 17:03
 **/
@Configuration
public class AmazonS3Config {
    
    private final CnAmazonS3Properties cnAmazonS3Properties;
    
    private final UsAmazonS3Properties usAmazonS3Properties;

    @Autowired
    public AmazonS3Config(CnAmazonS3Properties cnAmazonS3Properties, UsAmazonS3Properties usAmazonS3Properties) {
        this.cnAmazonS3Properties = cnAmazonS3Properties;
        this.usAmazonS3Properties = usAmazonS3Properties;
    }

    @Bean(name = "goodscenterAawsStaticCredentialsProvider")
    public AWSStaticCredentialsProvider goodscenterAwsStaticCredentialsProvider(){
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(cnAmazonS3Properties.getAccessKeyId(), cnAmazonS3Properties.getSecretAccessKey());
        return new AWSStaticCredentialsProvider(awsCredentials);
    }
    
    @Bean(name = "mallProductAwsStaticCredentialsProvider")
    public AWSStaticCredentialsProvider mallProductAwsStaticCredentialsProvider(){
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(usAmazonS3Properties.getAccessKeyId(), usAmazonS3Properties.getSecretAccessKey());
        return new AWSStaticCredentialsProvider(awsCredentials);
    }
    

}
