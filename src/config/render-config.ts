import path from 'path';
import { eta } from '../http';
import { FastifyRequest, FastifyReply } from 'fastify';

const pages = path.resolve(__dirname, '../../frontend/pages');

export class Render {
  static async page(req: FastifyRequest, reply: FastifyReply, filename: string, options?: object) {
    const file = path.join(pages, filename);
    const content = eta.readFile(file);

    const res = eta.renderString(content, { ...options });
    reply.header('Content-Type', 'text/html');
    reply.send(res);
  }
}
