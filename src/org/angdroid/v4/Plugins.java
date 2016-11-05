package org.angdroid.v4;

import java.util.zip.ZipInputStream;
import java.util.Scanner;
import java.io.InputStream;

final public class Plugins {
	public enum Plugin {
		v4(0);

		private int id;

		private Plugin(int id) {
			this.id = id;
		}
		public int getId() {
			return id;
		}
		public static Plugin convert(int value) {
			return Plugin.class.getEnumConstants()[value];
		}
	}

	static final String DEFAULT_PROFILE = "0~Default~PLAYER~0~0";
	public static String LoaderLib ="loader-v4";

	public static String getFilesDir(Plugin p) {
//		switch (p) {
//		case Sangband: return "sang";
//		case Steamband: return "steam";
//		default: return p.toString().toLowerCase();
//		}
		return p.toString().toLowerCase();
	}

	public static int getKeyDown(Plugin p) {
		switch (p) {
		default: return '2';
		}
	}

	public static int getKeyUp(Plugin p) {
		switch (p) {
		default: return '8';
		}
	}

	public static int getKeyLeft(Plugin p) {
		switch (p) {
		default: return '4';
		}
	}

	public static int getKeyRight(Plugin p) {
		switch (p) {
		default: return '6';
		}
	}

	public static int getKeyEnter(Plugin p) {
		return '\r';
	}

	public static int getKeyTab(Plugin p) {
		return '\t';
	}

	public static int getKeyDelete(Plugin p) {
		return 0x7F;
	}

	public static int getKeyBackspace(Plugin p) {
		return '\b';
	}

	public static int getKeyEsc(Plugin p) {
		return 0x1B;
	}

	public static int getKeyQuitAndSave(Plugin p) {
		return 0x18;
	}

	public static ZipInputStream getPluginZip(int plugin) {
		InputStream is = null;

		if (plugin == Plugin.v4.getId())
			is = Preferences.getResources().openRawResource(R.raw.zipv4);
		//else if (plugin == Plugin.faangband.getId())
		//	is = Preferences.getResources().openRawResource(R.raw.zipfaangband);

		return new ZipInputStream(is);
	}
	public static String getPluginCrc(int plugin) {
		InputStream is = null;
		if (plugin == Plugin.v4.getId())
			is = Preferences.getResources().openRawResource(R.raw.crcv4);
		//else if (plugin == Plugin.faangband.getId())
		//	is = Preferences.getResources().openRawResource(R.raw.crcfaangband);
		return new Scanner(is).useDelimiter("\\A").next().trim();
	}

	public static String getUpgradePath(Plugin p) {
		switch (p) {
		default: return "";
		}
	}

	public static String getStartBorgSequence() {
		return "";
	}
}
