package data.scripts.combatanalytics.util;


import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// can't use anything from Java.io because it will throw a security exception :(
public class CompressionUtil {

    public static byte[] compress(String text) {
        try {
            byte[] rawBytes = text.getBytes("UTF-8");

            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(rawBytes);
            deflater.finish();
            List<ByteArrayLengthPair> chunks = new ArrayList<>();
            do {
                byte[] data = new byte[rawBytes.length + 100];
                int count = deflater.deflate(data);
                chunks.add(new ByteArrayLengthPair(count, data));
            } while (!deflater.finished());

            deflater.end();

            return assembleIntoByteArray(chunks);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String decompress(byte[] bytes) {
        try {
            // Decompress the bytes
            Inflater inflater = new Inflater();
            inflater.setInput(bytes, 0, bytes.length);

            List<ByteArrayLengthPair> chunks = new ArrayList<>();
            do {
                byte[] data = new byte[bytes.length * 2];
                int resultLength = inflater.inflate(data);
                chunks.add(new ByteArrayLengthPair(resultLength, data));
            } while(!inflater.finished());

            inflater.end();

            // Decode the bytes into a String
            byte[] decompressedBytes = assembleIntoByteArray(chunks);
            return new String(decompressedBytes, 0, decompressedBytes.length, "UTF-8");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static byte[] assembleIntoByteArray(List<ByteArrayLengthPair> byteArrayLengthPairs){
        int size = 0;
        for(ByteArrayLengthPair pair : byteArrayLengthPairs){
            size += pair.length;
        }

        byte[] ret = new byte[size];
        int retIndex = 0;
        for(ByteArrayLengthPair pair : byteArrayLengthPairs){
            System.arraycopy(pair.data, 0, ret, retIndex, pair.length);
            retIndex += pair.length;
        }

        return ret;
    }

    private static class ByteArrayLengthPair {
        public int length;
        public byte[] data;

        public ByteArrayLengthPair(int length, byte[] data) {
            this.length = length;
            this.data = data;
        }
    }
}
