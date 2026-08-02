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
    const user = req.user as any;
    const userId = user.id;
    const protocol = req.protocol || 'http';
    const host = req.headers.host;
    const serverUrl = `${protocol}://${host}/api2`;

    const projectDir = '/opt/Mb4Core';
    const mainActivityPath = path.join(projectDir, 'app/src/main/java/com/penguinehis/socksrevive/SocksReviveMainActivity.java');
    const apkOutput = path.join(projectDir, 'app/build/outputs/apk/debug/app-debug.apk');
    const finalApkName = `DTunnelMod_${user.username}.apk`;
    const finalApkPath = path.join(projectDir, finalApkName);

    // 1. Fazer backup do arquivo original
    const backupPath = `${mainActivityPath}.bak`;
    fs.copyFileSync(mainActivityPath, backupPath);

    try {
      // 2. Injetar a URL e o Token do usuário no código-fonte
      let content = fs.readFileSync(mainActivityPath, 'utf8');
      
      // Substituir a URL hardcoded
      content = content.replace(
        /\.url\("https:\/\/panel\.dr2\.site\/api2"\)/g, 
        `.url("${serverUrl}")`
      );
      
      // Substituir o Token hardcoded
      content = content.replace(
        /\.addHeader\("dragoncore-token", ".*"\)/g, 
        `.addHeader("dragoncore-token", "${userId}")`
      );

      fs.writeFileSync(mainActivityPath, content);

      // 3. Configurar variáveis de ambiente e compilar
      const env = `export ANDROID_HOME=/opt/android-sdk && export NDK_HOME=/opt/android-sdk/ndk/27.0.12077973 && export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$NDK_HOME`;
      const command = `${env} && cd ${projectDir} && chmod +x gradlew && ./gradlew assembleDebug --no-daemon`;

      console.log(`Iniciando compilação do APK para o usuário: ${user.username} (ID: ${userId})`);

      exec(command, { timeout: 900000 }, (error, stdout, stderr) => {
        // Restaurar o arquivo original após a compilação (independente de sucesso ou erro)
        if (fs.existsSync(backupPath)) {
          fs.copyFileSync(backupPath, mainActivityPath);
          fs.unlinkSync(backupPath);
        }

        if (error) {
          console.error(`Erro ao gerar APK para ${user.username}: ${error.message}`);
          return;
        }
        
        if (fs.existsSync(apkOutput)) {
          console.log(`APK gerado com sucesso para ${user.username}`);
          fs.copyFileSync(apkOutput, finalApkPath);
        }
      });

      // Responder que o processo começou
      reply.send({ 
        message: 'A compilação do seu APK personalizado foi iniciada!', 
        downloadUrl: `/api/app/download-apk?user=${user.username}` 
      });

    } catch (err) {
      if (fs.existsSync(backupPath)) {
        fs.copyFileSync(backupPath, mainActivityPath);
        fs.unlinkSync(backupPath);
      }
      reply.status(500).send({ error: 'Erro interno ao preparar o código do APK.' });
    }
  },
};

export const DownloadApk: RouteOptions = {
  url: '/api/app/download-apk',
  method: 'GET',
  onRequest: [Authentication.user],
  handler: async (req: FastifyRequest, reply: FastifyReply) => {
    const user = req.user as any;
    const finalApkPath = `/opt/Mb4Core/DTunnelMod_${user.username}.apk`;
    
    if (fs.existsSync(finalApkPath)) {
      const fileStream = fs.createReadStream(finalApkPath);
      reply.header('Content-Type', 'application/vnd.android.package-archive');
      reply.header('Content-Disposition', `attachment; filename="DTunnelMod_${user.username}.apk"`);
      return reply.send(fileStream);
    } else {
      reply.status(404).send({ error: 'Seu APK ainda não foi gerado ou está sendo processado. Aguarde alguns minutos.' });
    }
  },
};

export default [GenerateApk, DownloadApk];
