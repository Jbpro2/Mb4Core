import prisma from '../../../config/prisma-client';
import SafeCallback from '../../../utils/safe-callback';

export default async function GetVersionConfig(user_id: string) {
  const user = await SafeCallback(() =>
    prisma.user.findUnique({
      where: {
        id: user_id,
      },
      select: {
        app_config_version: true,
      },
    })
  );
  return { version: user?.app_config_version || 0 };
}
