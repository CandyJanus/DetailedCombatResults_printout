package data.scripts.combatanalytics.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//https://stackoverflow.com/a/56712198
public class Base64
{
    // For simplicity, this method's base-64 encoding always encodes groups of
    // three bytes if possible (as four characters). This decision may
    // unfortunately cut off the beginning of some ASCII runs.
    public static String convert(byte[] bytes) { return convert(bytes, true); }
    public static String convert(byte[] bytes, boolean allowControlChars)
    {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int b;
        while (i < bytes.length)
        {
            b = get(bytes,i++);

            // Do binary encoding in groups of 3 bytes
            for (;; b = get(bytes,i++)) {
                int accum = b;
                if (i < bytes.length) {
                    b = get(bytes,i++);
                    accum = (accum << 8) | b;
                    if (i < bytes.length) {
                        b = get(bytes,i++);
                        accum = (accum << 8) | b;
                        sb.append(encodeBase64Digit(accum >> 18));
                        sb.append(encodeBase64Digit(accum >> 12));
                        sb.append(encodeBase64Digit(accum >> 6));
                        sb.append(encodeBase64Digit(accum));
                        if (i >= bytes.length)
                            break;
                    } else {
                        sb.append(encodeBase64Digit(accum >> 10));
                        sb.append(encodeBase64Digit(accum >> 4));
                        sb.append(encodeBase64Digit(accum << 2));
                        break;
                    }
                } else {
                    sb.append(encodeBase64Digit(accum >> 2));
                    sb.append(encodeBase64Digit(accum << 4));
                    break;
                }
            }
        }
        return sb.toString();
    }

    // Decodes a BAIS string back to a byte array.
    public static byte[] convert(String s)
    {
        byte[] b;
        b = s.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < b.length - 1; ++i) {
            int iOut = i;

            for (;;) {
                int cur;
                if (i >= b.length || ((cur = get(b, i)) < 63 || cur > 126))
                    throw new RuntimeException("String cannot be interpreted as a BAIS array");
                int digit = (cur - 64) & 63;
                int zeros = 16 - 6; // number of 0 bits on right side of accum
                int accum = digit << zeros;

                while (++i < b.length)
                {
                    if ((cur = get(b, i)) < 63 || cur > 126)
                        break;
                    digit = (cur - 64) & 63;
                    zeros -= 6;
                    accum |= digit << zeros;
                    if (zeros <= 8)
                    {
                        b[iOut++] = (byte)(accum >> 8);
                        accum <<= 8;
                        zeros += 8;
                    }
                }

                if ((accum & 0xFF00) != 0 || (i < b.length && b[i] != '!'))
                    throw new RuntimeException("String cannot be interpreted as BAIS array");
                i++;

                // Start taking bytes verbatim
                while (i < b.length && b[i] != '\b')
                    b[iOut++] = b[i++];
                if (i >= b.length)
                    return Arrays.copyOfRange(b, 0, iOut);
                i++;
            }
        }
        return b;
    }

    static int get(byte[] bytes, int i) { return ((int)bytes[i]) & 0xFF; }

    public static int decodeBase64Digit(char digit)
    { return digit >= 63 && digit <= 126 ? (digit - 64) & 63 : -1; }
    public static char encodeBase64Digit(int digit)
    { return (char)((digit + 1 & 63) + 63); }

}