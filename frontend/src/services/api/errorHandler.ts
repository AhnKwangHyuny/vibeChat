import { toast } from 'react-toastify';

export interface ApiError {
  status: number;
  message: string;
  timestamp?: string;
  path?: string;
  details?: Record<string, any>;
}

export class ApiErrorHandler {
  private static lastErrorTime = 0;
  private static lastErrorMessage = '';
  
  static handle(error: unknown): ApiError {
    let apiError: ApiError;
    
    if (error && (error as any).response) {
      // Server responded with error status
      const { status, data } = (error as any).response;
      const message = data.message || data.detail || data.title || "An unexpected error occurred.";
      
      apiError = {
        status,
        message,
        timestamp: data.timestamp,
        path: data.path,
        details: data
      };

      // Log error details for debugging
      console.error(`API Error [${status}] at ${data.path || 'unknown'}:`, {
        message,
        details: data,
        timestamp: data.timestamp
      });

      // Show user-friendly toast messages (중복 방지)
      const now = Date.now();
      const toastMessage = `${status}: ${message}`;
      
      // 같은 에러가 1초 이내에 중복되면 토스트 표시 안 함
      if (now - this.lastErrorTime > 1000 || this.lastErrorMessage !== toastMessage) {
        this.lastErrorTime = now;
        this.lastErrorMessage = toastMessage;
        
        switch (status) {
          case 400:
            toast.error(`잘못된 요청: ${message}`);
            break;
          case 401:
//             toast.error(`인증 오류: ${message}`);
            break;
          case 403:
            toast.error(`접근 권한이 없습니다: ${message}`);
            break;
          case 404:
            toast.error(`리소스를 찾을 수 없습니다: ${message}`);
            break;
          case 409:
            toast.error(`충돌 오류: ${message}`);
            break;
          case 429: {
            const retryAfter = (error as any).response.headers['retry-after'];
            const retryMessage = retryAfter 
              ? `요청이 너무 많습니다. ${retryAfter}초 후 다시 시도해주세요.`
              : `요청이 너무 많습니다. 잠시 후 다시 시도해주세요.`;
            toast.warn(retryMessage);
            break;
          }
          case 500:
            toast.error(`서버 오류: ${message}`);
            break;
          default:
            toast.error(`오류 ${status}: ${message}`);
        }
      }
    } else if (error && (error as any).request) {
      // Request was made but no response received
      const message = "서버에서 응답을 받지 못했습니다. 네트워크 연결을 확인해주세요.";
      
      apiError = {
        status: 0,
        message
      };

      console.error('Network Error:', {
        message,
        request: (error as any).request,
        timestamp: new Date().toISOString()
      });

      // 네트워크 에러도 중복 방지
      const now = Date.now();
      if (now - this.lastErrorTime > 1000 || this.lastErrorMessage !== message) {
        this.lastErrorTime = now;
        this.lastErrorMessage = message;
        toast.error(message);
      }
    } else {
      // Something happened in setting up the request
      const message = error instanceof Error ? error.message : "An unknown error occurred.";
      
      apiError = {
        status: -1,
        message
      };

      console.error('Request Setup Error:', {
        message,
        stack: error instanceof Error ? error.stack : undefined,
        timestamp: new Date().toISOString()
      });

      toast.error(message);
    }

    return apiError;
  }

  static isRetryableError(error: ApiError): boolean {
    return error.status === 0 || error.status >= 500 || error.status === 429;
  }

  static getRetryDelay(error: ApiError, attempt: number): number {
    if (error.status === 429) {
      // For rate limiting, use exponential backoff starting at 1 second
      return Math.min(1000 * Math.pow(2, attempt), 30000);
    }
    
    if (error.status >= 500 || error.status === 0) {
      // For server errors or network issues, use exponential backoff starting at 500ms
      return Math.min(500 * Math.pow(2, attempt), 10000);
    }

    return 0; // No retry for other errors
  }
}

// Retry utility function
export async function retryApiCall<T>(
  apiCall: () => Promise<T>, 
  maxRetries: number = 3
): Promise<T> {
  let lastError: ApiError;
  
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await apiCall();
    } catch (error) {
      lastError = ApiErrorHandler.handle(error);
      
      if (attempt === maxRetries || !ApiErrorHandler.isRetryableError(lastError)) {
        break;
      }
      
      const delay = ApiErrorHandler.getRetryDelay(lastError, attempt);
      if (delay > 0) {
        console.log(`Retrying API call in ${delay}ms (attempt ${attempt + 1}/${maxRetries + 1})`);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }
  
  throw lastError!;
}