import { z } from 'zod';
import GetVersionText from './get-app-text';
import GetVersionConfig from './get-app-config';
import GetVersionLayout from './get-app-layout';
import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';

const headerSchema = z.object({
  password: z.string().optional(),
  token: z.string(),
  version: z.enum(['app_config', 'app_layout', 'app_text']),
  //'user-agent': z.literal('DTunnelMod (@DTunnelMod, @DTunnelModGroup, @LightXVD)'),
});

const handler = {
  app_text: GetVersionText,
  app_config: GetVersionConfig,
  app_layout: GetVersionLayout,
};

export default {
  url: '/api/dtunnelmod/version',
  method: 'GET',
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const { success, data } = headerSchema.safeParse(req.headers);
    if (!success) return reply.send();

    const user_id = data.token;
    const response = await handler[data.version](user_id);

    return reply.send(response);
  },
} as RouteOptions;
