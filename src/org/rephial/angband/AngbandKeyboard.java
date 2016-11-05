package org.rephial.angband;

import android.content.Context;
import android.app.Activity;
import android.view.LayoutInflater;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.util.Log;

public class AngbandKeyboard implements OnKeyboardActionListener
{
	KeyboardView virtualKeyboardView;
	Keyboard virtualKeyboardQwerty;
	Keyboard virtualKeyboardSymbols;
	Keyboard virtualKeyboardSymbolsShift;
	StateManager state = null;

	AngbandKeyboard(Context ctx)
	{
		state = ((GameActivity)ctx).getStateManager();

		virtualKeyboardQwerty       = new Keyboard(ctx, R.xml.keyboard_qwerty);
		virtualKeyboardSymbols      = new Keyboard(ctx, R.xml.keyboard_sym);
		virtualKeyboardSymbolsShift = new Keyboard(ctx, R.xml.keyboard_symshift);

		LayoutInflater inflater = LayoutInflater.from(ctx);
		virtualKeyboardView = (KeyboardView)inflater.inflate(R.layout.input, null);

		virtualKeyboardView.setKeyboard(virtualKeyboardQwerty);
		virtualKeyboardView.setOnKeyboardActionListener(this);
	}

	private void handleShift()
	{
		Keyboard currentKeyboard = virtualKeyboardView.getKeyboard();

		if (currentKeyboard == virtualKeyboardQwerty) {
			virtualKeyboardView.setShifted(!virtualKeyboardView.isShifted());
		}
	}

	public void onKey(int primaryCode, int[] keyCodes)
	{
		char c = 0;

		switch (primaryCode) {
			case Keyboard.KEYCODE_DELETE:
				c = 0x9F;
				break;

			case Keyboard.KEYCODE_SHIFT:
				handleShift();
				break;

			case Keyboard.KEYCODE_ALT: {
				Keyboard current = virtualKeyboardView.getKeyboard();
				if (current == virtualKeyboardSymbolsShift) {
					virtualKeyboardView.setKeyboard(virtualKeyboardQwerty);
				} else {
					virtualKeyboardView.setKeyboard(virtualKeyboardSymbolsShift);
				}
				break;
			}

			case Keyboard.KEYCODE_MODE_CHANGE: {
				Keyboard keyboard = virtualKeyboardView.getKeyboard();
				if (keyboard == virtualKeyboardSymbols) {
					keyboard = virtualKeyboardQwerty;
				} else {
					keyboard = virtualKeyboardSymbols;
				}

				virtualKeyboardView.setKeyboard(keyboard);
				if (keyboard == virtualKeyboardSymbols) {
					keyboard.setShifted(false);
				}

				break;
			}

			default: {
				c = (char)primaryCode;
				if (virtualKeyboardView.getKeyboard() == virtualKeyboardQwerty &&
						virtualKeyboardView.isShifted()) {
					c = Character.toUpperCase(c);
					virtualKeyboardView.setShifted(false);
				}

				break;
			}
		}

		if (c != 0) {
			state.addKey(c);
		}
	}

	public void onPress(int primaryCode)
	{
	}
	public void onRelease(int primaryCode)
	{
	}
	public void onText(CharSequence text)
	{
	}
	public void swipeDown()
	{
	}
	public void swipeLeft()
	{
	}
	public void swipeRight()
	{
	}
	public void swipeUp()
	{
	}
}
