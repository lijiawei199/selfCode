package com.doublefs.data.media.management.service.enums;

import com.doublefs.data.common.exception.BaseError;

/**
 * @author wangkeyi
 * @description: 服务内部异常
 */
public enum GoodsCenterErrorCode implements BaseError {
    //上传文件的后缀扩展名称错误
    UPLOAD_FILE_EXPANDEDNAME_NULL(19001, "上传文件的扩展名称为空或者错误", "上传文件的扩展名称为空或者错误"),

    ;
    private final int errorCode;
    private final String message;
    private final String descInfo;

    GoodsCenterErrorCode(int errorCode, String message, String descInfo) {
        this.errorCode = errorCode;
        this.message = message;
        this.descInfo = descInfo;
    }

    @Override
    public int getErrorCode() {
        return this.errorCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getDescInfo() {
        return this.descInfo;
    }
}
