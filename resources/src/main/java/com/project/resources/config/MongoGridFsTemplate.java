package com.project.resources.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import static java.util.Collections.singletonList;

@Configuration
public class MongoGridFsTemplate extends AbstractMongoConfiguration {

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;
    @Value("${spring.data.mongodb.host}")
    private String mongoHost;
    @Value("${spring.data.mongodb.port}")
    private int mongoPort;
    @Value("${spring.data.mongodb.username}")
    private String username;
    @Value("${spring.data.mongodb.password}")
    private String password;

    @Override
    @Bean
    public MongoClient mongoClient() {
        return new MongoClient(singletonList(new ServerAddress(mongoHost, mongoPort)),
                singletonList(MongoCredential.createCredential(username, getDatabaseName(), password.toCharArray())));
    }

    @Bean
    public GridFsTemplate gridFsTemplate() throws Exception {
        return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
    }

    @Override
    protected String getDatabaseName() {
        return mongoDatabase;
    }
}
