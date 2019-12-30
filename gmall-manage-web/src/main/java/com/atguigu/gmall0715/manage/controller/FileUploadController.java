package com.atguigu.gmall0715.manage.controller;


import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {
    // 对于服务器ip 来讲：都应在应用程序中实现软编码！
    @Value("${fileServer.url}")
    private String fileUrl; //fileUrl=http://192.168.67.226

    // http://localhost:8082/fileUpload
    // springMVC 文件上传
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;
        // 项目目录不能有特殊字符
        if (file!=null){
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            String originalFilename = file.getOriginalFilename(); // zly.jpg

            // 设置文件的后缀名
            String extName = StringUtils.substringAfterLast(originalFilename, ".");
            // String orginalFilename="e://img//zly.jpg";
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                // imgUrl = http://192.168.67.226
                String path = upload_file[i];
                // System.out.println("s = " + s);
                // 字符串拼接
                // http://192.168.67.226 group1 M00/00/00/wKhD4l4G-aSAB0dxAACGx2c4tJ4196.jpg
                imgUrl+="/"+path;
			/*
			s = group1
			s = M00/00/00/wKhD4l4G-aSAB0dxAACGx2c4tJ4196.jpg
			 */
            }
        }
        // imgUrl = "http://192.168.67.226/group1/M00/00/00/wKhD4l4G-aSAB0dxAACGx2c4tJ4196.jpg"
        // return "http://192.168.67.226/group1/M00/00/00/wKhD4l4G-aSAB0dxAACGx2c4tJ4196.jpg";
        return imgUrl;
    }
}
