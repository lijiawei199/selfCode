package com.doublefs.data.media.management.service.impl.v1.util;

import com.doublefs.data.media.management.model.mysql.entity.Image;
import com.doublefs.data.media.management.model.rpc.vo.image.ImageDmmVO;
import org.mapstruct.Mapper;

import java.util.List;


/**
 * @author wangky
 */
@Mapper(componentModel = "spring")
public interface ImageConvertUtil {
    
    ImageDmmVO toImageDmmVO(Image image);
    
    List<ImageDmmVO> toImageVOList(List<Image> tagList);
    
}
