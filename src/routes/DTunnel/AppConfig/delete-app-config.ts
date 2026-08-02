import { z } from 'zod';
import prisma from '../../../config/prisma-client';
import SafeCallback from '../../../utils/safe-callback';
import Authentication from '../../../middlewares/authentication';
import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';

const paramsSchema = z.object({
  id: z.string(),
});

export default {
  url: '/config/:id',
  method: 'DELETE',
  onRequest: [Authentication.user],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const params = paramsSchema.parse(req.params);

    const id = Number(params.id);

    await SafeCallback(() =>
      prisma.appConfig.deleteMany({
        where: {
          id,
          user_id: req.user.id,
        },
      })
    );

    await SafeCallback(async () => {
      await prisma.user.update({
        where: { id: req.user.id },
        data: {
          app_config_version: { increment: 1 },
        },
      });
    });

    reply.status(204).send();
  },
} as RouteOptions;
