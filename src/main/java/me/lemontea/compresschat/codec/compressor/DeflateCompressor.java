package me.lemontea.compresschat.codec.compressor;

import me.lemontea.compresschat.codec.MessageCodec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class DeflateCompressor implements MessageCodec.StringCompressor {

    private static final int INITIAL_BUF_SIZE = 32;

    private final Deflater deflater;
    private final Inflater inflater;

    public DeflateCompressor() {
        deflater = new Deflater(Deflater.BEST_COMPRESSION);
        inflater = new Inflater();
    }

    @Override
    public byte[] compress(String str) {
        byte[] compressed = new byte[INITIAL_BUF_SIZE];

        deflater.setInput(str.getBytes(StandardCharsets.UTF_8));
        deflater.finish();

        int bytesRead = 0;

        while (!deflater.finished()) {
            if (bytesRead == compressed.length)
                compressed = Arrays.copyOf(compressed, compressed.length + INITIAL_BUF_SIZE);

            bytesRead += deflater.deflate(compressed, bytesRead, compressed.length - bytesRead);
        }

        deflater.reset();

        return Arrays.copyOf(compressed, bytesRead);
    }

    @Override
    public String decompress(byte[] data, int offset, int length) {
        byte[] decompressed = new byte[INITIAL_BUF_SIZE];

        inflater.setInput(data, offset, length);

        int bytesRead = 0;

        while (!inflater.finished()) {
            if (bytesRead == decompressed.length)
                decompressed = Arrays.copyOf(decompressed, decompressed.length + INITIAL_BUF_SIZE);

            try {
                bytesRead += inflater.inflate(decompressed, bytesRead, decompressed.length - bytesRead);
            } catch (DataFormatException e) {
                throw new IllegalArgumentException("Unable to decompress contents!", e);
            }

            if (bytesRead > DECOMPRESSION_SIZE_LIMIT)
                throw new IllegalArgumentException("Message is too long!");
        }

        inflater.reset();

        return new String(Arrays.copyOf(decompressed, bytesRead), StandardCharsets.UTF_8);
    }

}
