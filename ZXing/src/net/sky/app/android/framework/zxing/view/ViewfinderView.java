/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sky.app.android.framework.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import net.sky.app.android.framework.zxing.R;
import net.sky.app.android.framework.zxing.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial transparency outside it, as well as the laser scanner animation and result points.
 *
 */
public final class ViewfinderView extends View {
	private static final String TAG = "log";
	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;
	private static final int OPAQUE = 0xFF;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int ScreenRate;

	/**
	 * 四个绿色边角对应的宽度
	 */
	private static final int CORNER_WIDTH = 6;
	/**
	 * 扫描框中的中间线的宽度
	 */
	private static final int MIDDLE_LINE_WIDTH = 8;

	/**
	 * 扫描框中的中间线的与扫描框左右的间隙
	 */
	private static final int MIDDLE_LINE_PADDING = 5;

	/**
	 * 中间那条线每次刷新移动的距离
	 */
	private static final int SPEEN_DISTANCE = 5;

	/**
	 * 手机的屏幕密度
	 */
	private static float density;
	/**
	 * 字体大小
	 */
	private static final int TEXT_SIZE = 12;
	/**
	 * 字体距离扫描框下面的距离
	 */
	private static final int TEXT_PADDING_TOP = 30;

	/**
	 * 画笔对象的引用
	 */
	private Paint paint;

	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;

	/**
	 * 中间滑动线的最底端位置
	 */
	private int slideBottom;

	/**
	 * 将扫描的二维码拍下来，这里没有这个功能，暂时不考虑
	 */
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final int cornerColor;
	private final int frameColor;

	private final int resultPointColor;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;

	boolean isFirst;
	private Context context;

	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		// 将像素转换成dp
		ScreenRate = dip2px(context, 16);

		paint = new Paint();
		Resources resources = getResources();
		frameColor = resources.getColor(R.color.viewfinder_frame);
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);

		cornerColor = resources.getColor(R.color.color_blue);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		possibleResultPoints = new HashSet<ResultPoint>(5);
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		// 中间的扫描框，你要修改扫描框的大小，去CameraManager里面修改
		Rect frame = CameraManager.get().getFramingRect();
		if (frame == null) {
			return;
		}

		// 初始化中间线滑动的最上边和最下边
		if (!isFirst) {
			isFirst = true;
			slideTop = frame.top;
			slideBottom = frame.bottom;
		}

		// 获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		paint.setColor(resultBitmap != null ? resultColor : maskColor);

		// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(OPAQUE);
			canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
		} else {

			// 画出扫描内边框
			paint.setColor(frameColor);
			canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
			canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
			canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
			canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
			// 画扫描框边上的角，总共8个部分
			paint.setColor(cornerColor);
			canvas.drawRect(frame.left-CORNER_WIDTH+2, frame.top-CORNER_WIDTH+2, frame.left + ScreenRate+2, frame.top+2, paint);
			canvas.drawRect(frame.left-CORNER_WIDTH+2, frame.top-CORNER_WIDTH+2, frame.left+2, frame.top + ScreenRate+2, paint);
			canvas.drawRect(frame.right - ScreenRate-2, frame.top-CORNER_WIDTH+2, frame.right+CORNER_WIDTH-2, frame.top+2, paint);
			canvas.drawRect(frame.right-2, frame.top-CORNER_WIDTH+2, frame.right + CORNER_WIDTH-2, frame.top + ScreenRate+2, paint);//
			canvas.drawRect(frame.left-CORNER_WIDTH+2, frame.bottom-2, frame.left + ScreenRate+2, frame.bottom + CORNER_WIDTH-2, paint);//
			canvas.drawRect(frame.left-CORNER_WIDTH+2, frame.bottom - ScreenRate-2, frame.left+2, frame.bottom+CORNER_WIDTH-2, paint);
			canvas.drawRect(frame.right - ScreenRate-2, frame.bottom-2, frame.right+ CORNER_WIDTH-2, frame.bottom+ CORNER_WIDTH-2, paint);
			canvas.drawRect(frame.right-2, frame.bottom - ScreenRate-2, frame.right+ CORNER_WIDTH-2, frame.bottom+ CORNER_WIDTH-2, paint);

			// 绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
			slideTop += SPEEN_DISTANCE;
			if (slideTop >= frame.bottom) {
				slideTop = frame.top;
			}
//			canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);

			Bitmap bmp = drawableToBitmap(context.getResources().getDrawable(R.drawable.qb_scan_light));
			Bitmap mBitmap = Bitmap.createScaledBitmap(bmp, frame.right - frame.left, (int) ((frame.right - frame.left) / 500.0 * 32.0), true);
			canvas.drawBitmap(mBitmap, frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, paint);
//			drawImage(canvas, bmp, frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, mBitmap.getWidth(), mBitmap.getHeight(), 0, 0);
			// 画扫描框下面的字
			paint.setColor(Color.WHITE);
			paint.setTextSize(TEXT_SIZE * dip2px(context, 1));
			paint.setAlpha(0x40);
			paint.setTypeface(Typeface.create("System", Typeface.BOLD));
			canvas.drawText(getResources().getString(R.string.scan_text), frame.left+dip2px(context, 2), (float) (frame.top - (float) TEXT_PADDING_TOP * dip2px(context, 1)), paint);

			Collection<ResultPoint> currentPossible = possibleResultPoints;
			Collection<ResultPoint> currentLast = lastPossibleResultPoints;
			if (currentPossible.isEmpty()) {
				lastPossibleResultPoints = null;
			} else {
				possibleResultPoints = new HashSet<ResultPoint>(5);
				lastPossibleResultPoints = currentPossible;
				paint.setAlpha(OPAQUE);
				paint.setColor(resultPointColor);
				for (ResultPoint point : currentPossible) {
					canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
				}
			}
			if (currentLast != null) {
				paint.setAlpha(OPAQUE / 2);
				paint.setColor(resultPointColor);
				for (ResultPoint point : currentLast) {
					canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
				}
			}

			// 只刷新扫描框的内容，其他地方不刷新
			postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);

		}
	}

	public void drawViewfinder() {
		resultBitmap = null;
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live scanning display.
	 *
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {

		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height,

		drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}

//  GameView.drawImage(canvas, mBitDestTop, miDTX, mBitQQ.getHeight(), mBitDestTop.getWidth(), mBitDestTop.getHeight()/2, 0, 0);
	public static void drawImage(Canvas canvas, Bitmap blt, int x, int y, int w, int h, int bx, int by) { // x,y表示绘画的起点，
		Rect src = new Rect();// 图片
		Rect dst = new Rect();// 屏幕位置及尺寸
		// src 这个是表示绘画图片的大小
		src.left = bx; // 0,0
		src.top = by;
		src.right = bx + w;// mBitDestTop.getWidth();,这个是桌面图的宽度，
		src.bottom = by + h;// mBitDestTop.getHeight()/2;// 这个是桌面图的高度的一半
		// 下面的 dst 是表示 绘画这个图片的位置
		dst.left = x; // miDTX,//这个是可以改变的，也就是绘图的起点X位置
		dst.top = y; // mBitQQ.getHeight();//这个是QQ图片的高度。 也就相当于 桌面图片绘画起点的Y坐标
		dst.right = x + w; // miDTX + mBitDestTop.getWidth();// 表示需绘画的图片的右上角
		dst.bottom = y + h; // mBitQQ.getHeight() + mBitDestTop.getHeight();//表示需绘画的图片的右下角
		canvas.drawBitmap(blt, src, dst, null);// 这个方法 第一个参数是图片原来的大小，第二个参数是 绘画该图片需显示多少。也就是说你想绘画该图片的某一些地方，而不是全部图片，第三个参数表示该图片绘画的位置

		src = null;
		dst = null;
	}
	/**
	 *
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 *
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue ) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}
}
