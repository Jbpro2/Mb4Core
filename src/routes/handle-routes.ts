import csrf from '../utils/csrf-protection';
import { Render } from '../config/render-config';
import GetFilesDir from '../utils/get-files-dir';
import HandlerErrors from '../errors/handler-errors';
import { FastifyInstance, RouteOptions } from 'fastify';

export default function handler(fastify: FastifyInstance, _: any, done: () => void) {
  const routes = GetFilesDir(__dirname, ['handle-routes.ts', 'handle-routes.js']);

  routes.forEach((file) => {
    try {
      const exported = require(file).default;
      if (Array.isArray(exported)) {
        exported.forEach((route: RouteOptions) => {
          if (route && route.url) fastify.route(route);
        });
      } else {
        const route: RouteOptions = exported;
        if (route && route.url) fastify.route(route);
      }
    } catch (err) {
      console.log(err);
    }
  });

  fastify.decorateRequest('csrfProtection', { getter: () => csrf });

  fastify.setNotFoundHandler((req, reply) => {
    reply.status(404);
    Render.page(req, reply, '/404/index.html');
  });

  fastify.setErrorHandler(HandlerErrors);

  done();
}
