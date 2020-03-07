package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.upload.controller.UploadController;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    //文件支持的类型
    private static final List<String> suffixes = Arrays.asList("image/png", "image/jpeg");

    @Autowired
    FastFileStorageClient storageClient;

    public String upload(MultipartFile file) {

        try {
            //1、图片信息校验
            //1)校验呢文件类型
            String type = file.getContentType();
            if(!suffixes.contains(type)) {
                logger.info("上传失败，文件类型不匹配：{}", type);
                return null;
            }
            //2)校验图配内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image == null) {
                logger.info("上传的文件不符合要求");
                return null;
            }

/*          将文件保存到本地
            //2、保存图片
            //2.1生成目录
            File dir = new File("/Users/zym-shine/Desktop/leyou-temp-preserve-image");
            if(!dir.exists()){
                dir.mkdirs();
            }
            //2.2保存图片
            file.transferTo(new File(dir, file.getOriginalFilename()));

            //2.3拼接图片地址
            String url = "http://image.leyou.com/upload/" + file.getOriginalFilename();
*/
            //将文件上传到fastDFS
            // 2、将图片上传到FastDFS
            // 2.1、获取文件后缀名
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            // 2.2、上传
            StorePath storePath = this.storageClient.uploadFile(
                    file.getInputStream(), file.getSize(), extension, null);
            // 2.3、返回完整路径
            return "http://image.leyou.com/" + storePath.getFullPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
