# 🔧 Supabase + Google OAuth 설정 가이드

## 📋 **필요한 정보**
- Google Client ID: `40395775260-5c8g184cau53n87gm231hpfbhpse313m.apps.googleusercontent.com`
- Supabase Project URL: `https://[your-project-id].supabase.co`
- Supabase Anon Key: `[Supabase Dashboard에서 확인]`
- Supabase Service Role Key: `[Supabase Dashboard에서 확인]`

---

## 🌐 **1단계: Supabase Dashboard 설정**

### **1.1 Authentication > Providers > Google**
```
✅ Enabled: 체크
📝 Client ID: 40395775260-5c8g184cau53n87gm231hpfbhpse313m.apps.googleusercontent.com
🔐 Client Secret: [Google Cloud Console에서 생성한 시크릿 키 입력]
```

### **1.2 Authentication > Settings > Site URL**
```
🌐 Site URL: http://localhost:5173
```

### **1.3 Authentication > Settings > Redirect URLs**
```
➕ 추가할 URL들:
- http://localhost:5173/auth/callback
- http://localhost:3000/auth/callback
```

### **1.4 API Keys 확인**
- **Settings > API** 에서 다음 키들을 복사:
  - `anon public` key
  - `service_role` key (⚠️ 비밀 키 - 백엔드에서만 사용)

---

## 🔧 **2단계: 환경 변수 설정**

### **2.1 Backend 환경 변수 (.env 또는 application.yml)**
```yaml
# Supabase Configuration
SUPABASE_URL=https://[your-project-id].supabase.co
SUPABASE_SERVICE_ROLE_KEY=[service_role_key_여기에_입력]

# 기존 설정들
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/vibechat?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=vibechat
SPRING_DATASOURCE_PASSWORD=vibechat123
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SECURITY_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### **2.2 Frontend 환경 변수 (.env.local)**
```bash
# API Configuration
VITE_API_BASE=/api
VITE_WS_URL=ws://localhost/ws

# Supabase Configuration
VITE_SUPABASE_URL=https://[your-project-id].supabase.co
VITE_SUPABASE_ANON_KEY=[anon_public_key_여기에_입력]
```

---

## 🚀 **3단계: Google Cloud Console 추가 설정**

### **3.1 승인된 리다이렉트 URI 추가**
Google Cloud Console > APIs & Services > Credentials > OAuth 2.0 Client ID 편집

**기존 URI:**
- http://localhost:5173/auth/callback
- http://localhost:3000/auth/callback

**추가할 URI:**
```
https://[your-project-id].supabase.co/auth/v1/callback
```

---

## 🔄 **4단계: 동작 플로우**

### **4.1 로그인 플로우**
1. 사용자: "Google로 계속" 클릭
2. Frontend: Supabase signInWithOAuth('google') 호출
3. Supabase: Google OAuth 페이지로 리다이렉트
4. Google: 사용자 로그인 후 Supabase로 콜백
5. Supabase: Access Token 생성 후 Frontend `/auth/callback`으로 리다이렉트
6. Frontend: 세션 확인 후 Backend `/api/auth/google` 호출
7. Backend: Supabase 토큰 검증 → 사용자 생성/업데이트 → 세션 생성
8. Frontend: 홈페이지로 리다이렉트

### **4.2 데이터 매핑**
```
Google Profile → Supabase → Backend User:
- Google ID → Supabase user.id → User.providerId
- Google name → Supabase user_metadata.name → User.nickname  
- Google picture → Supabase user_metadata.picture → User.avatarUrl
- Provider: "GOOGLE" → UserProvider.GOOGLE
```

---

## 🛠️ **5단계: 테스트 방법**

### **5.1 개발 환경 실행**
```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend  
cd frontend
npm run dev
```

### **5.2 테스트 시나리오**
1. http://localhost:5173 접속
2. "로그인" 버튼 클릭
3. "Google" 탭 선택
4. "Google로 계속" 클릭
5. Google 로그인 페이지에서 로그인
6. 자동으로 홈페이지로 리다이렉트 확인

---

## ⚠️ **주의사항**

1. **Service Role Key는 절대 프론트엔드에 노출하면 안 됩니다!**
2. **Anon Key만 프론트엔드에서 사용하세요**
3. **환경 변수 파일(.env)은 .gitignore에 추가하세요**
4. **개발/운영 환경별로 다른 Supabase 프로젝트 사용 권장**

---

## 🔍 **문제 해결**

### **일반적인 오류들:**
- **"Invalid redirect URL"**: Supabase Redirect URLs 설정 확인
- **"Unauthorized"**: Service Role Key 확인
- **"Token validation failed"**: Anon Key 확인
- **CORS 오류**: SECURITY_ALLOWED_ORIGINS 설정 확인

### **로그 확인:**
- Backend: `logs/vibechat.log`
- Frontend: 브라우저 개발자 도구 콘솔
- Supabase: Dashboard > Logs
