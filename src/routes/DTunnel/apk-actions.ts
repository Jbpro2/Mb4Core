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
    
    // Aumentar o tempo limite de execução do comando para 15 minutos
    const command = `${env} && cd ${projectDir} && chmod +x gradlew && ./gradlew assembleDebug --no-daemon`;

    console.log("Iniciando compilação do APK...");

    exec(command, { timeout: 900000 }, (error, stdout, stderr) => {
      if (error) {
        console.error(`Erro ao gerar APK: ${error.message}`);
        console.error(`Stderr: ${stderr}`);
        // Não enviamos o reply aqui se ele já tiver sido enviado por timeout, 
        // mas o Fastify lidará com isso se configurarmos corretamente.
      }
      
      if (fs.existsSync(apkOutput)) {
        console.log("APK compilado com sucesso.");
        fs.copyFileSync(apkOutput, finalApk);
      } else {
        console.error("APK não encontrado após a compilação.");
      }
    });

    // Enviamos uma resposta imediata ou deixamos o cliente esperando?
    // Para evitar "Server connection error", vamos avisar que começou.
    // Mas o frontend espera o resultado final. Vamos manter o cliente esperando,
    // o aumento do timeout no http.ts deve ajudar.
    
    // Se quisermos ser mais robustos, poderíamos usar um sistema de status, 
    // mas vamos tentar primeiro com o timeout estendido.
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
      reply.status(404).send({ error: 'APK ainda não foi gerado ou está sendo processado.' });
    }
  },
};

export default [GenerateApk, DownloadApk];
