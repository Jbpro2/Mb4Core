import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';
import AESCrypt from '../../utils/crypto';
import GetAppConfig from './get-app-config';
import GetAppLayout from './get-app-layout';
import GetAppText from './get-app-text';

const handler = {
  app_config: GetAppConfig,
  app_layout: GetAppLayout,
  app_text: GetAppText,
};

export default {
  url: '/api2',
  method: 'POST',
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const token = req.headers['dragoncore-token'] as string;
    const update = req.headers['dragoncore-update'] as 'app_config' | 'app_layout' | 'app_text';
    
    if (!token || !update || !handler[update]) {
      return reply.status(400).send({ error: 'Missing headers' });
    }

    const response = await handler[update](token);
    
    // O aplicativo Android usa esta senha específica para descriptografar
    const password = "05VE1b3kx10ntsfzvsSmZD3KYuilFXyS";
    
    // O aplicativo Android espera um objeto JSON com o campo "data"
    const encryptedData = AESCrypt.encrypt(password, JSON.stringify(response));
    
    reply.send({ data: encryptedData });
  },
} as RouteOptions;
