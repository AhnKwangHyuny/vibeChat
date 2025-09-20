package com.vibechat.service.supabase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SupabaseAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;
    
    @Value("${supabase.anon-key}")
    private String anonKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Supabase에서 사용자 정보 검증
     */
    public SupabaseUserInfo validateUser(String accessToken) {
        try {
            String url = supabaseUrl + "/auth/v1/user";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", anonKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode userNode = objectMapper.readTree(response.getBody());
                return parseSupabaseUser(userNode);
            } else {
                logger.error("Supabase 사용자 검증 실패. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Supabase 사용자 검증 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * Supabase 관리자 API를 통한 사용자 정보 조회 (백업용)
     */
    public SupabaseUserInfo getUserByProviderId(String providerId) {
        try {
            String url = supabaseUrl + "/auth/v1/admin/users/" + providerId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceRoleKey);
            headers.set("apikey", serviceRoleKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode userNode = objectMapper.readTree(response.getBody());
                return parseSupabaseUser(userNode);
            } else {
                logger.error("Supabase 관리자 API 사용자 조회 실패. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Supabase 관리자 API 사용자 조회 중 오류 발생", e);
            return null;
        }
    }
    
    private SupabaseUserInfo parseSupabaseUser(JsonNode userNode) {
        try {
            SupabaseUserInfo userInfo = new SupabaseUserInfo();
            
            userInfo.setId(userNode.get("id").asText());
            userInfo.setEmail(userNode.has("email") ? userNode.get("email").asText() : null);
            
            // user_metadata에서 Google 프로필 정보 추출
            if (userNode.has("user_metadata")) {
                JsonNode metadata = userNode.get("user_metadata");
                userInfo.setName(getStringValue(metadata, "name", "full_name"));
                userInfo.setAvatarUrl(getStringValue(metadata, "avatar_url", "picture"));
            }
            
            // app_metadata에서 provider 정보 추출
            if (userNode.has("app_metadata")) {
                JsonNode appMetadata = userNode.get("app_metadata");
                if (appMetadata.has("provider")) {
                    userInfo.setProvider(appMetadata.get("provider").asText());
                }
            }
            
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Supabase 사용자 정보 파싱 중 오류 발생", e);
            return null;
        }
    }
    
    private String getStringValue(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
    }
    
    public static class SupabaseUserInfo {
        private String id;
        private String email;
        private String name;
        private String avatarUrl;
        private String provider;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        @Override
        public String toString() {
            return "SupabaseUserInfo{" +
                    "id='" + id + '\'' +
                    ", email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    ", provider='" + provider + '\'' +
                    '}';
        }
    }
}
