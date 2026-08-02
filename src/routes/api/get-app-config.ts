import prisma from '../../config/prisma-client';
import SafeCallback from '../../utils/safe-callback';
import { AppConfigSelect } from '../../routes/DTunnel/AppConfig/zod-schema';
import { AppConfigParserApi } from '../../utils/parsers/app-config-parser';

const BASEV25_SUPPORTED_MODES = [
  'SSH_DIRECT',
  'SSH_PROXY',
  'SSH_DNSTT',
  'SSL_DIRECT',
  'SSL_PROXY',
  'V2RAY',
];

export default async function GetAppConfig(user_id: string) {
  const AppConfig = await SafeCallback(() =>
    prisma.appConfig.findMany({
      where: {
        user_id,
        status: 'ACTIVE',
        mode: { in: BASEV25_SUPPORTED_MODES },
        category: {
          status: 'ACTIVE',
        },
      },
      select: {
        ...AppConfigSelect,
      },
    })
  );
  if (!AppConfig?.length) return [];
  return AppConfig ? AppConfig.map(AppConfigParserApi) : [];
}
