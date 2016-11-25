package com.comm.utils;

public class FormatUtil {
	/* 将字符串作为十六进制数转换为十六进制形式 */
	public static byte[] stringAsHex(String str) {
		int n = 0;
		String ss = str;
		while (ss.contains(" ")) {
			ss = ss.substring(ss.indexOf(" ") + 1);
			n++;
		}
		byte[] txData = new byte[(str.length() - n) / 2];
		int i = 0, j = 0;
		for (; i < str.length() - 1; i++) {
			if (str.charAt(i) != 32) {
				Integer a = new java.math.BigInteger(str.substring(i, i + 2),
						16).intValue();
				txData[j] = (byte) (a & 0xff);
				j++;
				i++;
			}
		}
		return txData;
	}

	public static String byteToHex(byte[] sbyte, int len) {
		String str = "";
		for (int i = 0; i < len; i++) {
			str += (sbyte[i] & 0xff) < 16 ? "0"
					+ Integer.toHexString(sbyte[i] & 0xff).toUpperCase() + " "
					: Integer.toHexString(sbyte[i] & 0xff).toUpperCase() + " ";
		}
		return str;

	}
}
