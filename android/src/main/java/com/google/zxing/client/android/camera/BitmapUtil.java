package com.google.zxing.client.android.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zd on 2016/8/9.
 */
public class BitmapUtil {


    /**
     * 压缩图片 从60开始压缩，直到大小小于200k为止
     *
     * @param bitmap
     * @return
     */
    public static byte[] portraitBitmapCompressBelow200(Bitmap bitmap) {
        int ratio = 60;
        byte[] bitBuff = null;
        while (null == bitBuff || bitBuff.length > 200 * 1024) {
            try {
                bitBuff = bitmapCompress(bitmap, ratio);
                ratio = ratio - 10;
                if (ratio <= 10) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return bitBuff;
    }

    /**
     * 压缩图片
     *
     * @param bitmap
     * @param quality 提示到压缩机,0 - 100。0意思体积小的压缩,压缩为马克斯100意义质量。一些格式,如PNG是无损的,将忽略质量设置
     * @return
     */
    public static byte[] bitmapCompress(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = null;
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            b = baos.toByteArray();
        } finally {
            try {
                if (null != baos) {
                    baos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return b;
    }

    /**
     * 获取图片
     *
     * @param filePath  图片路径
     * @param reqWidth  要达到的宽
     * @param reqHeight 要达到的高
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(String filePath, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 获取要缩放图片的 inSampleSize
     *
     * @param options
     * @param reqWidth  要达到的宽
     * @param reqHeight 要达到的高
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        if (height > reqHeight || width > reqWidth) {
            float w = width / reqWidth;
            float h = height / reqHeight;
            if (w > h) {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            } else {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            }
        }
        return inSampleSize;
    }

    /**
     * 压缩图片
     */
    public static File compressImage(String picPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(picPath, null);
        byte[] bts = portraitBitmapCompressBelow200(bitmap);

        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }

        File file = new File(picPath);
        String fileName = file.getName();
        String nameFileName = "";
        if (fileName.contains(".")) {
            String tempFileName = fileName.substring(0, fileName.lastIndexOf("."));
            nameFileName = fileName.replace(tempFileName, tempFileName + "_compressed");
        }

        return settingStorePath(bts,  nameFileName);
    }



    public static File settingStorePath(byte[] buf, String filePath) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {
                dir.mkdirs();
            }
            file = new File(filePath);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    /**
     * 旋转图片
     *
     * @param angle  被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }

    /**
     * 处理旋转图片
     *
     * @param originPath 原图路径
     * @return 返回修复完毕后的图片路径
     */
    public static String amendRotatePhoto(String originPath, String path) {
        // 取得图片旋转角度
        int angle = readPictureDegree(originPath);
        if (angle == 0 || angle % 360 == 0) {
            return originPath;
        }
        //压缩图片
        Bitmap bitmap = getCompressPhoto(originPath);
        // 保存修复后的图片并返回保存后的图片路径
        return saveBitmap(path, rotaingImageView(angle, bitmap));
    }

    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 保存Bitmap图片在SD卡中
     * 如果没有SD卡则存在手机中
     *
     * @param bitmap 需要保存的Bitmap图片
     * @return 保存成功时返回图片的路径，失败时返回null
     */
    public static String saveBitmap(String path, Bitmap bitmap) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(path);
            // 把数据写入文件，100表示不压缩
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (outStream != null) {
                    // 记得要关闭流！
                    outStream.close();
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param path 原图的路径
     * @return 压缩后的图片
     */
    public static Bitmap getCompressPhoto(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 6;
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        options = null;
        return bmp;
    }
}
