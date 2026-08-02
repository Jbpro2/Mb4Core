import prisma from '../../../config/prisma-client';
import SafeCallback from '../../../utils/safe-callback';

export default async function GetVersionLayout(user_id: string) {
  const user = await SafeCallback(() =>
    prisma.user.findUnique({
      where: {
        id: user_id,
      },
      select: {
        app_layout_version: true,
      },
    })
  );
  return { version: user?.app_layout_version || 0 };
}
