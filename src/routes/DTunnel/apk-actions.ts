import { FastifyReply, FastifyRequest, RouteOptions } from 'fastify';
import Authentication from '../../middlewares/authentication';
import { exec } from 'child_process';
import path from 'path';
import fs from 'fs';

export const GenerateApk: RouteOptions = {
  url: '/api/app/generate-apk',
  method: 'POST',
  onRequest: [Authentication.user],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const projectDir = '/opt/Mb4Core';
    const apkOutput = path.join(projectDir, 'app/build/outputs/apk/debug/app-debug.apk');
    const finalApk = path.join(projectDir, 'DTunnelMod.apk');

    // Configurar variáveis de ambiente e compilar com suporte a NDK
    const env = `export ANDROID_HOME=/opt/android-sdk && export NDK_HOME=/opt/android-sdk/ndk/27.0.12077973 && export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$NDK_HOME`;
    const command = `${env} && cd ${projectDir} && chmod +x gradlew && ./gradlew assembleDebug`;

    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`Erro ao gerar APK: ${error.message}`);
        return reply.status(500).send({ error: 'Falha ao compilar o APK. Verifique se o NDK está instalado corretamente.' });
      }
      
      if (fs.existsSync(apkOutput)) {
        fs.copyFileSync(apkOutput, finalApk);
        reply.send({ message: 'APK gerado com sucesso!', downloadUrl: '/api/app/download-apk' });
      } else {
        reply.status(500).send({ error: 'APK não encontrado após a compilação.' });
      }
    });
  },
};

export const DownloadApk: RouteOptions = {
  url: '/api/app/download-apk',
  method: 'GET',
  onRequest: [Authentication.user],
  handler: async (_req: FastifyRequest, reply: FastifyReply) => {
    const finalApk = '/opt/Mb4Core/DTunnelMod.apk';
    
    if (fs.existsSync(finalApk)) {
      const fileStream = fs.createReadStream(finalApk);
      reply.header('Content-Type', 'application/vnd.android.package-archive');
      reply.header('Content-Disposition', 'attachment; filename="DTunnelMod.apk"');
      return reply.send(fileStream);
    } else {
      reply.status(404).send({ error: 'APK ainda não foi gerado.' });
    }
  },
};

export default [GenerateApk, DownloadApk];
