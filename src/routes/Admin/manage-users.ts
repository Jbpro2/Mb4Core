import { z } from 'zod';
import prisma from '../../config/prisma-client';
import SafeCallback from '../../utils/safe-callback';
import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';

// Middleware local para verificar se é admin
const isAdmin = async (req: FastifyRequest, reply: FastifyReply) => {
  if (!req.user || req.user.role !== 'admin') {
    reply.status(403).send({ error: 'Acesso negado. Apenas administradores.' });
    throw new Error('Unauthorized');
  }
};

export const ListUsers: RouteOptions = {
  url: '/api/admin/users',
  method: 'GET',
  preHandler: [isAdmin],
  handler: async (_req: FastifyRequest, reply: FastifyReply) => {
    const users = await SafeCallback(() =>
      prisma.user.findMany({
        select: {
          id: true,
          username: true,
          email: true,
          role: true,
          expires_at: true,
          created_at: true,
        },
      })
    );
    reply.send(users);
  },
};

export const DeleteUser: RouteOptions = {
  url: '/api/admin/users/:id',
  method: 'DELETE',
  preHandler: [isAdmin],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    
    const userToDelete = await prisma.user.findUnique({ where: { id } });
    if (userToDelete?.role === 'admin') {
      return reply.status(400).send({ error: 'Não é possível remover um administrador.' });
    }

    await SafeCallback(() => prisma.user.delete({ where: { id } }));
    reply.send({ message: 'Usuário removido com sucesso.' });
  },
};

export const AddAccessDays: RouteOptions = {
  url: '/api/admin/users/:id/access',
  method: 'POST',
  preHandler: [isAdmin],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const { days } = z.object({ days: z.number().min(1) }).parse(req.body);

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) return reply.status(404).send({ error: 'Usuário não encontrado.' });

    const currentExpiry = user.expires_at ? new Date(user.expires_at) : new Date();
    const newExpiry = new Date(currentExpiry.getTime() + days * 24 * 60 * 60 * 1000);

    await SafeCallback(() =>
      prisma.user.update({
        where: { id },
        data: { expires_at: newExpiry },
      })
    );

    reply.send({ message: `Acesso estendido por ${days} dias. Nova expiração: ${newExpiry.toISOString()}` });
  },
};

// Exportar as rotas individualmente para o handle-routes carregar
export default [ListUsers, DeleteUser, AddAccessDays];
