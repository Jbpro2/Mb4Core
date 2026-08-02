import { Render } from '../../../config/render-config';
import Authentication from '../../../middlewares/authentication';
import { FastifyRequest, FastifyReply, RouteOptions } from 'fastify';

export default {
  url: '/admin',
  method: 'GET',
  onRequest: [Authentication.user],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    // Apenas admins podem acessar a página
    if (!req.user || req.user.role !== 'admin') {
      return reply.redirect('/');
    }

    Render.page(req, reply, '/admin/index.html', {
      user: req.user,
      active: 'admin',
      csrfToken: req.csrfProtection.generateCsrf(),
    });
  },
} as RouteOptions;
