import prisma from '../../../config/prisma-client';
import SafeCallback from '../../../utils/safe-callback';

export default async function GetVersionText(user_id: string) {
  const user = await SafeCallback(() =>
    prisma.user.findUnique({
      where: {
        id: user_id,
      },
      select: {
        app_text_version: true,
      },
    })
  );
  return { version: user?.app_text_version || 0 };
}
