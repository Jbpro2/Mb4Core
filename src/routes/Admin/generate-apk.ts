import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';
import Authentication from '../../middlewares/authentication';
import { exec } from 'child_process';
import path from 'path';
import fs from 'fs';

const isAdmin = async (req: FastifyRequest, reply: FastifyReply) => {
  if (!req.user || req.user.role !== 'admin') {
    reply.status(403).send({ error: 'Acesso negado. Apenas administradores.' });
    throw new Error('Unauthorized');
  }
};

export const GenerateApk: RouteOptions = {
  url: '/api/admin/generate-apk',
  method: 'POST',
  onRequest: [Authentication.user],
  preHandler: [isAdmin],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const projectDir = '/opt/Mb4Core';
    const apkOutput = path.join(projectDir, 'app/build/outputs/apk/debug/app-debug.apk');

    // Comando para compilar o APK usando o Gradle
    // Em um cenário real, precisaríamos configurar o local.properties com o caminho do SDK do Android
    const command = `cd ${projectDir} && ./gradlew assembleDebug`;

    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`Erro ao gerar APK: ${error.message}`);
        return reply.status(500).send({ error: 'Falha ao compilar o APK. Verifique se o SDK do Android está instalado.' });
      }
      
      if (fs.existsSync(apkOutput)) {
        reply.send({ message: 'APK gerado com sucesso!', downloadUrl: '/api/admin/download-apk' });
      } else {
        reply.status(500).send({ error: 'APK não encontrado após a compilação.' });
      }
    });
  },
};

export const DownloadApk: RouteOptions = {
  url: '/api/admin/download-apk',
  method: 'GET',
  onRequest: [Authentication.user],
  handler: async (_req: FastifyRequest, reply: FastifyReply) => {
    const apkPath = '/opt/Mb4Core/app/build/outputs/apk/debug/app-debug.apk';
    
    if (fs.existsSync(apkPath)) {
      const fileStream = fs.createReadStream(apkPath);
      reply.header('Content-Type', 'application/vnd.android.package-archive');
      reply.header('Content-Disposition', 'attachment; filename="DTunnelMod.apk"');
      return reply.send(fileStream);
    } else {
      reply.status(404).send({ error: 'APK ainda não foi gerado.' });
    }
  },
};

export default [GenerateApk, DownloadApk];
