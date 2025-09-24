package com.vibechat.config.init;

import com.vibechat.domain.message.ChatMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * 애플리케이션 시작 시 MongoDB 인덱스를 생성하는 초기화 로직을 담당합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    /**
     * Bean 초기화 완료 후 인덱스 생성을 시작합니다.
     */
    @PostConstruct
    public void initIndexes() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps(ChatMessage.class);
            IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
            resolver.resolveIndexFor(ChatMessage.class).forEach(indexOps::ensureIndex);
            log.info("MongoDB indexes ensured successfully for Message collection.");
        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes.", e);
        }
    }
}