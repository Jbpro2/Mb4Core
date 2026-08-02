import prisma from '../../config/prisma-client';
import SafeCallback from '../../utils/safe-callback';
import { ConvertToHexAARRGGBB } from '../../utils/convert-color';

export default async function GetCategory(user_id: string) {
  const category = await SafeCallback(() =>
    prisma.category.findMany({
      where: {
        user_id,
        status: 'ACTIVE',
      },
      omit: {
        user_id: true,
        created_at: true,
        updated_at: true,
      },
    })
  );
  return category
    ? category.map((v) => {
        v.color = ConvertToHexAARRGGBB(v.color);
        return v;
      })
    : [];
}
