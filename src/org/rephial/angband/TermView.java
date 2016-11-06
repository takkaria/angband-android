/*
 * File: TermView.java
 * Purpose: Terminal-base view for Android application
 *
 * Copyright (c) 2010 David Barr, Sergey Belinsky
 *
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of either:
 *
 * a) the GNU General Public License as published by the Free Software
 *    Foundation, version 2, or
 *
 * b) the "Angband licence":
 *    This software may be copied and distributed for educational, research,
 *    and not for profit purposes provided that this copyright and statement
 *    are included in all such copies.  Other copyrights may also apply.
 */

package org.rephial.angband;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.os.Vibrator;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.GestureDetector.OnGestureListener;

public class TermView extends View implements OnGestureListener {

	Typeface tfStd;
	Typeface tfTiny;
	Bitmap bitmap;
	Canvas canvas;
	Paint fore;
	Paint back;
	Paint cursor;

	public int rows = 0;
	public int cols = 0;

	public int canvas_width = 0;
	public int canvas_height = 0;

	private int char_height = 0;
	private int char_width = 0;
	private int font_text_size = 0;

	private Vibrator vibrator;
	private boolean vibrate;
	private Handler handler = null;
	private StateManager state = null;
	private Context _context;

	private GestureDetector gesture;

	public TermView(Context context) {
		super(context);
		initTermView(context);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
	}

	public TermView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTermView(context);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
	}

	protected void initTermView(Context context) {
		_context = context;

		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		if (isHighRes()) fore.setAntiAlias(true);
		setForeColor(Color.WHITE);

		back = new Paint();
		setBackColor(Color.BLACK);

		cursor = new Paint();
		cursor.setColor(Color.GREEN);
		cursor.setStyle(Paint.Style.STROKE);
		cursor.setStrokeWidth(0);

		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		setFocusableInTouchMode(true);
		gesture = new GestureDetector(context, this);
	}

	protected void onDraw(Canvas canvas) {
		if (bitmap != null) {

			canvas.drawBitmap(bitmap, 0, 0, null);

			// The Great DEBUG CIRCLE
			Paint p = new Paint();
			p.setARGB(128, 128, 128, 128);
			p.setStrokeWidth(10);
			canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth()/4, p);

			if (state.stdscr.cursor_visible) {
				int x = state.stdscr.col * (char_width);
				int y = (state.stdscr.row + 1) * char_height;

				// due to font "scrunch", cursor is sometimes a bit too big
				int cl = Math.max(x,0);
				int cr = Math.min(x+char_width,canvas_width-1);
				int ct = Math.max(y-char_height,0);
				int cb = Math.min(y,canvas_height-1);

				canvas.drawRect(cl, ct, cr, cb, cursor);
			}
		}
	}

	public void computeCanvasSize() {
		int width = getWidth();
		int height = getHeight();
		Log.d("Angband","screensize h="+height+",w="+width);

		if (width != canvas_width || height != canvas_height) {
			canvas_width = width;
			canvas_height = height;
			Log.d("Angband","createBitmap "+canvas_width+","+canvas_height);
			bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.RGB_565);
			canvas = new Canvas(bitmap);

			Log.d("Angband","computeCanvasSize. " +
				", height/width = " + char_height + "," + char_width +
				"canvas = " + canvas_height + "," + canvas_width);

			rows = canvas_height / char_height;
			cols = canvas_width / char_width;
		}
	}

	protected void setForeColor(int a) {
		fore.setColor(a);
	}

	protected void setBackColor(int a) {
		back.setColor(a);
	}

	public void autoSizeFontByHeight(int maxHeight) {
	}

	public void autoSizeFontByWidth(int maxWidth) {
	}

	public boolean isHighRes() {
		Display display = ((WindowManager) _context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int maxWidth = display.getWidth();
		int maxHeight = display.getHeight();

		return Math.max(maxWidth,maxHeight) > 480;
	}

	private void setFontSizeLegacy() {
		font_text_size = 13;
		char_height = 13;
		char_width = 6;
		setFontSize(font_text_size);
	}

	private void setFontFace() {
		if (!isHighRes()) {
			tfTiny = Typeface.createFromAsset(getResources().getAssets(), "6x13.ttf");
			fore.setTypeface(tfTiny);
		} else {
			tfStd = Typeface.createFromAsset(getResources().getAssets(), "VeraMoBd.ttf");
			fore.setTypeface(tfStd);
		}
	}

	public boolean increaseFontSize() {
		return setFontSize(font_text_size + 1);
	}

	public boolean decreaseFontSize() {
		return setFontSize(font_text_size - 1);
	}

	private boolean setFontSize(int size) {
		return setFontSize(size, true);
	}

	private boolean setFontSize(int size, boolean persist) {
		setFontFace();

		if (size < 6) {
			size = 6;
			return false;
		} else if (size > 64) {
			size = 64;
			return false;
		} else {
			font_text_size = size;
			fore.setTextSize(font_text_size);

			if (persist) {
				if (Preferences.isScreenPortraitOrientation()) {
					Preferences.setPortraitFontSize(font_text_size);
				} else {
					Preferences.setLandscapeFontSize(font_text_size);
				}
			}

			char_height = (int) Math.ceil(fore.getFontSpacing());
			char_width = (int) fore.measureText("X", 0, 1);

			Log.d("Angband","setFontSize. size = " + size +
				", height/width = " + char_height + "," + char_width +
				"canvas = " + canvas_height + "," + canvas_width);

			rows = canvas_height / char_height;
			cols = canvas_width / char_width;

			return true;
		}
	}

	@Override
	protected void onMeasure(int widthmeasurespec, int heightmeasurespec) {
		int height = MeasureSpec.getSize(heightmeasurespec);
		int width = MeasureSpec.getSize(widthmeasurespec);

		int fs = 0;
		if (Preferences.isScreenPortraitOrientation())
			fs = Preferences.getPortraitFontSize();
		else
			fs = Preferences.getLandscapeFontSize();

		if (fs == 0)
			autoSizeFontByWidth(width);
		else
			setFontSize(fs, false);

		fore.setTextAlign(Paint.Align.LEFT);

		int minheight = getSuggestedMinimumHeight();
		int minwidth = getSuggestedMinimumWidth();

		setMeasuredDimension(width, height);
		Log.d("Angband","onMeasure "+canvas_width+","+canvas_height+";"+
				"w/h:" + width + "," + height +
				"mw/mh:" + minwidth + "," + minheight);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gesture.onTouchEvent(me);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		int newscrollx = this.getScrollX() + (int)distanceX;
		int newscrolly = this.getScrollY() + (int)distanceY;

		if (newscrollx < 0)
			newscrollx = 0;
		if (newscrolly < 0)
			newscrolly = 0;
		if (newscrollx >= canvas_width - getWidth())
			newscrollx = canvas_width - getWidth() + 1;
		if (newscrolly >= canvas_height - getHeight())
			newscrolly = canvas_height - getHeight() + 1;

		if (canvas_width <= getWidth())
			newscrollx = 0; //this.getScrollX();
		if (canvas_height <= getHeight())
			newscrolly = 0; //this.getScrollY();

		scrollTo(newscrollx, newscrolly);

		return true;
	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return true;
	}

	public void onLongPress(MotionEvent e) {
		handler.sendEmptyMessage(AngbandDialog.Action.OpenContextMenu.ordinal());
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onSingleTapUp(MotionEvent event) {
		if (Preferences.getEnableTouch()) {
			int x = (int) event.getX();
			int y = (int) event.getY();

			int c = (x * 3) / getWidth();
			int r = (y * 3) / getHeight();

			int key = (2 - r) * 3 + c + '1';

			state.addDirectionKey(key);

			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d("Angband", "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		computeCanvasSize();
		handler.sendEmptyMessage(AngbandDialog.Action.StartGame.ordinal());
	}

	public void drawPoint(int r, int c, char ch, int fcolor, int bcolor, boolean extendedErase) {
		float x = c * char_width;
		float y = r * char_height;

		if (canvas == null) {
			// OnSizeChanged has not been called yet
			//Log.d("Angband","null canvas in drawPoint");
			return;
		}

		setBackColor(bcolor);

		canvas.drawRect(x,
				y,
				x + char_width + (extendedErase ? 1 : 0),
				y + char_height + (extendedErase ? 1 : 0),
				back);

		if (ch != ' ') {
			String str = ch + "";

			setForeColor(fcolor);
			canvas.drawText(str,
					x,
					y + char_height - fore.descent(),
					fore);
		}
	}

	public void clear() {
		if (canvas != null) {
			canvas.drawPaint(back);
		}
	}

	public void noise() {
		if (vibrate) {
			vibrator.vibrate(50);
		}
	}

	public void onResume() {
		//Log.d("Angband","Termview.onResume()");
		vibrate = Preferences.getVibrate();
	}

	public void onPause() {
		//Log.d("Angband","Termview.onPause()");
		// this is the only guaranteed safe place to save state
		// according to SDK docs
		state.gameThread.send(GameThread.Request.SaveGame);
	}
}
