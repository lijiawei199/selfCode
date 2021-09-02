package com.doublefs.data.media.management.service.impl.v1.image;

import com.doublefs.data.media.management.model.mysql.entity.Image;
import com.doublefs.data.media.management.model.mysql.entity.ImageExample;
import com.doublefs.data.media.management.model.mysql.mapper.ImageMapper;
import com.doublefs.data.media.management.model.rpc.vo.image.ImageDmmVO;
import com.doublefs.data.media.management.service.impl.v1.util.ImageConvertUtil;
import com.doublefs.data.media.management.service.internal.v1.image.ImageService;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片表的增删改成
 *
 * @author wangky
 * @date 2021/03/14 17:37
 **/
@Service
public class ImageServiceImpl implements ImageService {
    final ImageMapper imageMapper;
    final ImageConvertUtil imageConvertUtil;

    public ImageServiceImpl(ImageMapper imageMapper, ImageConvertUtil imageConvertUtil) {
        this.imageMapper = imageMapper;
        this.imageConvertUtil = imageConvertUtil;
    }

    @Override
    public int insert(Image image) {
        return imageMapper.insert(image);
    }

    @Override
    public int updateResizeStatus(Long imageId, byte resizeStatus) {
        Image image = new Image();
        image.setResizeStatus(resizeStatus);
        image.setImageId(imageId);
        return imageMapper.updateByPrimaryKeySelective(image);
    }

    @Override
    public List<ImageDmmVO> getImages(List<ImageDmmVO> urls) {
        List<Image> images = getImagesByUrls(urls);
        return CollectionUtils.isEmpty(images) ? null : imageConvertUtil.toImageVOList(images);
    }

    @Override
    public String checkImagesResizedStatus(List<ImageDmmVO> urls) {
        List<Image> images = getImagesByUrls(urls);
        if (images == null) {
            return null;
        }
        if (!CollectionUtils.isEmpty(images)) {
            if (images.stream().filter(image -> image.getResizeStatus() == 1).collect(Collectors.toList()).size() != urls.size()) {
                return "false";
            }
            return "true";
        }
        return "false";
    }

    @Nullable
    private List<Image> getImagesByUrls(List<ImageDmmVO> urls) {
        ImageExample imageExample = new ImageExample();
        ImageExample.Criteria criteria = imageExample.createCriteria();
        if (CollectionUtils.isEmpty(urls)) {
            return null;
        }

        for (ImageDmmVO imageDmm : urls) {
            if (ObjectUtils.isNotEmpty(imageDmm.getCnUrl())) {
                criteria.andCnUrlIn(urls.stream().map(img -> img.getCnUrl()).collect(Collectors.toList()));
                break;
            }
            if (ObjectUtils.isNotEmpty(imageDmm.getUsUrl())) {
                criteria.andUsUrlIn(urls.stream().map(img -> img.getUsUrl()).collect(Collectors.toList()));
                break;
            }
        }
        List<Image> images = imageMapper.selectByExample(imageExample);
        return images;
    }
}
