package com.project.resources.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.project.resources.model.ResourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequestMapping("/resources/")
@RestController
public class ResourcesController {

    Logger logger = LoggerFactory.getLogger(ResourcesController.class);

    @Autowired
    GridFsOperations gridFSOperations;

    @PostMapping("/upload/{username}")
    public HashMap<String, String> uploadDocuments(@PathVariable("username") String username, @RequestParam("file") MultipartFile[] files){

        DBObject metaData = new BasicDBObject();
        metaData.put("username", username);

        HashMap ids = new HashMap<String, String>(files.length + 1, 1);

        Arrays.stream(files).forEach(file -> {

            logger.info(file.getContentType());

            try {
                String id = gridFSOperations.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), metaData).toString();

                if(id != null && !id.isEmpty())
                    ids.put(file.getOriginalFilename(), id);
            }catch(Exception ex){
                logger.warn("Exception occurred: "+ ex.getMessage() + ". For " + file.getOriginalFilename());
            }
        });

        return ids;
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable("fileId") String fileId){

        GridFSFile gridFSFile = gridFSOperations.findOne(new Query(Criteria.where("_id").is(fileId)));

        GridFsResource resource = gridFSOperations.getResource(gridFSFile.getFilename());

        try{
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(resource.getContentType())).body(resource);
        }catch(Exception ex){
            ex.printStackTrace();
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/download/all/{username}")
    public ResponseEntity<File> downloadDocuments(@PathVariable("username") String username){

        GridFSFindIterable files = gridFSOperations.find(new Query(Criteria.where("metadata.username").is(username)));

        logger.info("Got files: " + files.toString());

        Iterator<GridFSFile> itr = files.iterator();

        File zippedFile = null;
        FileOutputStream fos = null;
        ZipOutputStream zipOut = null;

        try {
            zippedFile = new File("resources.zip");
            fos = new FileOutputStream(zippedFile);
            zipOut = new ZipOutputStream(fos);
        }catch (FileNotFoundException ex){
            logger.warn(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        while(itr.hasNext()){
            GridFSFile file = itr.next();

            GridFsResource resource = gridFSOperations.getResource(file.getFilename());

            logger.info("Got Resource: " + resource.toString());

            try{
                if(resource != null){

                    InputStream fileInputStream = resource.getInputStream();

                    ZipEntry zipEntry = new ZipEntry(resource.getFilename());
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while((length = fileInputStream.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                    fileInputStream.close();
                }
            }catch (IOException ex){
                logger.warn(ex.getMessage());
            }
        }

        try {
            zipOut.close();
            fos.close();
        }catch (IOException ex){
            logger.warn(ex.getMessage());
        }

        try {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).body(zippedFile);
        }catch(Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }
}
