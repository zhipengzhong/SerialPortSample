package com.young.serialportsample.util;

/**
 * Created by Zhipe on 2018/1/12.
 */

public class HexUtil {

    private final static String[] HEX = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    public static String byte2String(byte bByte) {
        int iRet = bByte;
        if (iRet < 0) {
            iRet += 256;
        }
        int iD1 = iRet / 16;
        int iD2 = iRet % 16;
        return (HEX[iD1] + HEX[iD2]).toUpperCase();
    }

    public static String bytes2String(byte[] bByte) {
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < bByte.length; i++) {
            sBuffer.append(byte2String(bByte[i]));
        }
        return sBuffer.toString().toUpperCase();
    }

    public static int byte2Int(byte bByte) {
        if (bByte < 0) {
            return bByte + 256;
        }
        return bByte;
    }

    public static byte hexS2Byte(String hex) throws NumberFormatException {
        return (byte) Integer.parseInt(hex, 16);
    }

    public static byte[] hexS2Bytes(String hex) throws NumberFormatException {
        int len = hex.length();
        byte[] result;
        if ((len & 0x1) == 1) {
            len++;
            result = new byte[(len / 2)];
            hex = "0" + hex;
        } else {
            result = new byte[(len / 2)];
        }
        int j = 0;
        for (int i = 0; i < len; i += 2) {
            result[j] = hexS2Byte(hex.substring(i, i + 2));
            j++;
        }
        return result;
    }


}
