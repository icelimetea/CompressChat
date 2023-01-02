package me.lemontea.compresschat.codec;

import me.lemontea.compresschat.codec.compressor.BrotliCompressor;
import me.lemontea.compresschat.codec.compressor.DeflateCompressor;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageCodec {

    private static final String COMPRESSED_MSG_START = "$!";
    private static final String COMPRESSED_MSG_END   = "!$";

    private static final Pattern COMPRESSED_MSG_PATTERN =
            Pattern.compile(Pattern.quote(COMPRESSED_MSG_START) + "(.+?)" + Pattern.quote(COMPRESSED_MSG_END));

    private final NavigableMap<Byte, StringCompressor> compressors;

    private final Map<BigInteger, Character> alphabetMap;
    private final Map<Character, BigInteger> invAlphabetMap;

    private final BigInteger base;

    private MessageCodec(Map<Byte, StringCompressor> compressors, char fromChar, char toChar, IntPredicate charFilter) {
        this.compressors = new TreeMap<>(Byte::compareUnsigned);

        for (Map.Entry<Byte, StringCompressor> entry : compressors.entrySet()) {
            if (entry.getKey() == 0)
                throw new IllegalArgumentException("Compressor ID should not be equal to zero!");

            this.compressors.put(entry.getKey(), entry.getValue());
        }

        if (compressors.isEmpty())
            throw new IllegalArgumentException("No string compressors defined!");

        alphabetMap = new HashMap<>();
        invAlphabetMap = new HashMap<>();

        int charIdx = 0;

        for (int charCode = fromChar; charCode <= toChar; charCode++) {
            if (charFilter.test(charCode)) {
                BigInteger idxBigInt = BigInteger.valueOf(charIdx);

                alphabetMap.put(idxBigInt, (char) charCode);
                invAlphabetMap.put((char) charCode, idxBigInt);

                charIdx++;
            }
        }

        if (charIdx < 2)
            throw new IllegalArgumentException("The resulting alphabet is too small!");

        base = BigInteger.valueOf(charIdx);
    }

    public static MessageCodec createCodec() {
        return new MessageCodec(
                Map.of(
                        (byte) 1, new DeflateCompressor(),
                        (byte) 2, new BrotliCompressor()
                ),
                '\u0021',
                Character.MAX_VALUE,
                charCode -> {
                    int charType = Character.getType((char) charCode);

                    return charCode != 0x7F
                            && charCode != 0xA7
                            && charType != Character.UNASSIGNED
                            && charType != Character.PRIVATE_USE
                            && charType != Character.SURROGATE;
                }
        );
    }

    public String encodeMessage(String msg) {
        return COMPRESSED_MSG_PATTERN.matcher(msg).replaceAll(
                matchResult -> Matcher.quoteReplacement(
                        COMPRESSED_MSG_START
                                + encode(matchResult.group(1))
                                + COMPRESSED_MSG_END
                )
        );
    }

    public String decodeMessage(String msg) {
        return COMPRESSED_MSG_PATTERN.matcher(msg).replaceAll(matchResult -> {
            try {
                return Matcher.quoteReplacement(decode(matchResult.group(1)));
            } catch (RuntimeException e) {
                return Matcher.quoteReplacement("!" + e.getMessage() + "!");
            }
        });
    }

    private String encode(String msg) {
        Map.Entry<Byte, StringCompressor> compressorEntry = compressors.lastEntry();

        byte[] compressed = compressorEntry.getValue().compress(msg);
        byte[] encoded = new byte[compressed.length + 1];

        encoded[0] = compressorEntry.getKey();

        System.arraycopy(compressed, 0, encoded, 1, compressed.length);

        StringBuilder result = new StringBuilder();
        BigInteger msgInt = new BigInteger(1, encoded);

        while (!msgInt.equals(BigInteger.ZERO)) {
            BigInteger[] divisionResult = msgInt.divideAndRemainder(base);

            result.append(alphabetMap.get(divisionResult[1]));

            msgInt = divisionResult[0];
        }

        return result.toString();
    }

    private String decode(String msg) {
        BigInteger msgInt = BigInteger.ZERO;
        BigInteger k = BigInteger.ONE;

        for (char ch : msg.toCharArray()) {
            BigInteger charIdx = invAlphabetMap.get(ch);

            if (charIdx == null)
                throw new IllegalArgumentException("Unexpected character!");

            msgInt = msgInt.add(k.multiply(charIdx));

            k = k.multiply(base);
        }

        byte[] encoded = msgInt.toByteArray();

        if (encoded.length == 0)
            throw new IllegalArgumentException("Malformed message!");

        int idByteIdx = (encoded[0] == 0) ? 1 : 0;

        if (encoded.length < idByteIdx + 1)
            throw new IllegalArgumentException("Message is too small!");

        StringCompressor compressor = compressors.get(encoded[idByteIdx]);

        if (compressor == null)
            throw new IllegalArgumentException("Unknown compressor!");

        return compressor.decompress(encoded, idByteIdx + 1);
    }

    public interface StringCompressor {

        byte[] compress(String str);

        String decompress(byte[] data, int offset, int length);

        default String decompress(byte[] data, int offset) {
            return decompress(data, offset, data.length - offset);
        }

    }

}
