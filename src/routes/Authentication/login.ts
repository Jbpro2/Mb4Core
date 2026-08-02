import { z } from 'zod';
import BCrypt from '../../utils/bcrypt';
import prisma from '../../config/prisma-client';
import SafeCallback from '../../utils/safe-callback';
import CookieManager from '../../utils/cookie-manager';
import csrfProtection from '../../middlewares/csrf-protection';
import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';

const loginSchema = z.object({
  login: z.string().min(3).max(50),
  password: z.string().min(6).max(20),
});

export default {
  url: '/login',
  method: 'POST',
  onRequest: [csrfProtection],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const { login, password } = loginSchema.parse(req.body);

    // Tentar encontrar por email ou username
    const user = await SafeCallback(() =>
      prisma.user.findFirst({
        where: {
          OR: [
            { email: login },
            { username: login }
          ]
        },
      })
    );

    if (!user || !BCrypt.compare(password, user.password)) {
      reply.status(401);
      reply.header('csrf-token', req.csrfProtection.generateCsrf());
      throw new Error('Usuário e/ou senha inválidos');
    }

    CookieManager.setCookiesLoggedIn(reply, user.id);

    reply.send({ status: 200, message: 'Sucesso, aguarde você está sendo redirecionando' });
  },
} as RouteOptions;
