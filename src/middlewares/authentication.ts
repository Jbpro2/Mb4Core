import prisma from '../config/prisma-client';
import { JWTConfig } from '../config/jwt-config';
import CookieManager from '../utils/cookie-manager';
import { JsonWebTokenError } from 'jsonwebtoken';
import SafeCallback from '../utils/safe-callback';
import { FastifyReply, FastifyRequest } from 'fastify';

interface IJwtPayload {
  id: string;
  exp: number;
}

export default class Authentication {
  private static redirect(req: FastifyRequest, reply: FastifyReply, message?: string) {
    CookieManager.deleteCookiesLoggedIn(reply);
    const routesNotAuthorized = ['/login', '/register'];
    const route = routesNotAuthorized.find((route) => route === req.routeOptions.config.url);
    if (!route) {
      // Se for uma chamada de API, retornar erro JSON em vez de redirect
      if (req.url.startsWith('/api/')) {
        reply.status(401).send({ error: message || 'Não autorizado' });
        throw new Error(message || 'Unauthorized');
      }
      reply.redirect('/login');
    }
  }

  static async user(req: FastifyRequest, reply: FastifyReply) {
    const { accessToken, refreshToken } = req.cookies;
    if (!refreshToken || !accessToken) return Authentication.redirect(req, reply);

    const verifyRefreshToken = JWTConfig.verify(refreshToken, process.env.JWT_SECRET_REFRESH!) as IJwtPayload;
    if (verifyRefreshToken instanceof JsonWebTokenError) {
      return Authentication.redirect(req, reply);
    }

    const verifyAccessToken = JWTConfig.verify(accessToken, process.env.JWT_SECRET_KEY!) as IJwtPayload;
    if (verifyAccessToken instanceof JsonWebTokenError) {
      if (verifyAccessToken.name === 'TokenExpiredError') {
        CookieManager.setCookiesLoggedIn(reply, verifyRefreshToken.id);
        reply.redirect('/');
      }
      return Authentication.redirect(req, reply);
    }

    if ((verifyAccessToken as IJwtPayload).id) {
      const id = (verifyAccessToken as IJwtPayload).id;
      const user = await SafeCallback(() => prisma.user.findUnique({ where: { id } }));
      
      if (!user) {
        return Authentication.redirect(req, reply);
      }

      // Verificar expiração (exceto para admin)
      if (user.role !== 'admin' && user.expires_at) {
        const now = new Date();
        const expiry = new Date(user.expires_at);
        if (now > expiry) {
          return Authentication.redirect(req, reply, 'Sua conta expirou. Entre em contato com o administrador.');
        }
      }

      req.user = user;
    }
  }
}
