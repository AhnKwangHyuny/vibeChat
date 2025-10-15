package com.vibechat.config.properties;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis ConnectionFactory 초기화 시작: {}:{}", redisHost, redisPort);

        ClientResources clientResources = ClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build();
        log.info("Lettuce ClientResources 설정 완료: ioThreads={}, computationThreads={}", 4, 4);

        ClientOptions clientOptions = ClientOptions.builder()
            .autoReconnect(true)
            .pingBeforeActivateConnection(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build();
        log.info("Lettuce ClientOptions 설정 완료: autoReconnect={}, pingBeforeActivate={}", true, true);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(60))       // 명령 타임아웃
            .shutdownTimeout(Duration.ofSeconds(100))     // 종료 타임아웃
            .clientOptions(clientOptions)                 // Client Options 연결
            .clientResources(clientResources)             // Client Resources 연결
            .build();
        log.info("LettuceClientConfiguration 설정 완료: commandTimeout=60s, shutdownTimeout=100s");

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        log.info("Redis 서버 설정 완료: {}:{}", redisHost, redisPort);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.setShareNativeConnection(false);  // Streams용 독립 연결
        factory.setValidateConnection(true);       // 연결 검증 활성화
        factory.afterPropertiesSet();

        log.info("Redis ConnectionFactory 생성 완료: shareNativeConnection={}, validateConnection={}",
                 false, true);

        return factory;
    }


    /**
     * Redis Streams 전용 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Serializer 설정
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        log.info("RedisTemplate 설정 완료: StringRedisSerializer (MapRecord 최적화)");
        return template;
    }
}
