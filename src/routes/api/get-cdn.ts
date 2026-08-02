import prisma from '../../config/prisma-client';
import SafeCallback from '../../utils/safe-callback';

export default async function GetCdn(user_id: string) {
  const cdn = await SafeCallback(() =>
    prisma.cdn.findMany({
      where: {
        user_id,
        status: 'ACTIVE',
      },
      omit: {
        user_id: true,
        status: true,
      },
    })
  );
  return cdn ? cdn : [];
}
