import { ICsrfProtection } from './src/utils/csrf-protection';

declare module 'fastify' {
  interface FastifyRequest {
    csrfProtection: ICsrfProtection;
    user: {
      id: string;
      username: string;
      email: string;
      role: string;
      expires_at: Date | null;
    };
  }
}

declare global {
  namespace NodeJS {
    interface ProcessEnv {
      PORT: number;
      CSRF_SECRET: string;
      JWT_SECRET_KEY: string;
      JWT_SECRET_REFRESH: string;
      NODE_ENV: 'development' | 'production';
    }
  }
}

export {};
