package me.lemontea.compresschat.codec.compressor;

import me.lemontea.compresschat.codec.MessageCodec;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BrotliCompressor implements MessageCodec.StringCompressor {

    private static final int INITIAL_BUF_SIZE = 32;

    private static final int BROTLI_FALSE = 0;
    private static final int BROTLI_TRUE = 1;

    private static final int BROTLI_MAX_QUALITY = 11;

    private static final MethodHandle BROTLI_ENCODER_CREATE_INSTANCE;
    private static final MethodHandle BROTLI_ENCODER_SET_PARAMETER;
    private static final MethodHandle BROTLI_ENCODER_COMPRESS_STREAM;
    private static final MethodHandle BROTLI_ENCODER_IS_FINISHED;
    private static final MethodHandle BROTLI_ENCODER_HAS_MORE_OUTPUT;
    private static final MethodHandle BROTLI_ENCODER_TAKE_OUTPUT;
    private static final MethodHandle BROTLI_ENCODER_DESTROY_INSTANCE;

    private static final MethodHandle BROTLI_DECODER_CREATE_INSTANCE;
    private static final MethodHandle BROTLI_DECODER_DECOMPRESS_STREAM;
    private static final MethodHandle BROTLI_DECODER_HAS_MORE_OUTPUT;
    private static final MethodHandle BROTLI_DECODER_TAKE_OUTPUT;
    private static final MethodHandle BROTLI_DECODER_DESTROY_INSTANCE;

    static {
        Linker linker = Linker.nativeLinker();

        SymbolLookup decoderLookup = SymbolLookup.libraryLookup(
                System.mapLibraryName("brotlidec"),
                MemorySession.global()
        );

        SymbolLookup encoderLookup = SymbolLookup.libraryLookup(
                System.mapLibraryName("brotlienc"),
                MemorySession.global()
        );

        // Encoder

        BROTLI_ENCODER_CREATE_INSTANCE =
                encoderLookup.lookup("BrotliEncoderCreateInstance").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_ENCODER_SET_PARAMETER =
                encoderLookup.lookup("BrotliEncoderSetParameter").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT
                        )
                )).orElseThrow();

        BROTLI_ENCODER_COMPRESS_STREAM =
                encoderLookup.lookup("BrotliEncoderCompressStream").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_ENCODER_TAKE_OUTPUT =
                encoderLookup.lookup("BrotliEncoderTakeOutput").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_ENCODER_IS_FINISHED =
                encoderLookup.lookup("BrotliEncoderIsFinished").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_ENCODER_HAS_MORE_OUTPUT =
                encoderLookup.lookup("BrotliEncoderHasMoreOutput").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_ENCODER_DESTROY_INSTANCE =
                encoderLookup.lookup("BrotliEncoderDestroyInstance").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
                )).orElseThrow();

        // Decoder

        BROTLI_DECODER_CREATE_INSTANCE =
                decoderLookup.lookup("BrotliDecoderCreateInstance").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_DECODER_DECOMPRESS_STREAM =
                decoderLookup.lookup("BrotliDecoderDecompressStream").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_DECODER_HAS_MORE_OUTPUT =
                decoderLookup.lookup("BrotliDecoderHasMoreOutput").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_DECODER_TAKE_OUTPUT =
                decoderLookup.lookup("BrotliDecoderTakeOutput").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS
                        )
                )).orElseThrow();

        BROTLI_DECODER_DESTROY_INSTANCE =
                decoderLookup.lookup("BrotliDecoderDestroyInstance").map(memorySegment -> linker.downcallHandle(
                        memorySegment,
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
                )).orElseThrow();
    }

    @Override
    public byte[] compress(String str) {
        Addressable brotliState;

        try {
            brotliState = (MemoryAddress) BROTLI_ENCODER_CREATE_INSTANCE.invokeExact(
                    (Addressable) MemoryAddress.NULL,
                    (Addressable) MemoryAddress.NULL,
                    (Addressable) MemoryAddress.NULL
            );
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to initialize brotli!", e);
        }

        if (brotliState.equals(MemoryAddress.NULL))
            throw new IllegalStateException("Unable to initialize brotli encoder!");

        int exitCode;

        try (MemorySession memorySession = MemorySession.openConfined()) {
            byte[] data = str.getBytes(StandardCharsets.UTF_8);

            MemorySegment dataBuf = MemorySegment.allocateNative(data.length, memorySession);
            MemorySegment.copy(data, 0, dataBuf, ValueLayout.JAVA_BYTE, 0, data.length);

            NativeContext ctx = new NativeContext(memorySession);

            ctx.setAvailableOut(0);
            ctx.setAvailableIn(dataBuf.byteSize());
            ctx.setNextIn(dataBuf);

            Addressable availOutPtr = ctx.getAvailableOutPtr();
            Addressable sizeOutPtr = ctx.getSizeOutPtr();
            Addressable availInPtr = ctx.getAvailableInPtr();
            Addressable nextInPtr = ctx.getNextInPtr();

            exitCode = (int) BROTLI_ENCODER_SET_PARAMETER.invokeExact(brotliState, BrotliEncoderParam.MODE.ordinal(), BrotliEncoderMode.TEXT.ordinal());

            if (exitCode == BROTLI_FALSE)
                throw new IllegalStateException("Unable to set compression mode!");

            exitCode = (int) BROTLI_ENCODER_SET_PARAMETER.invokeExact(brotliState, BrotliEncoderParam.QUALITY.ordinal(), BROTLI_MAX_QUALITY);

            if (exitCode == BROTLI_FALSE)
                throw new IllegalStateException("Unable to set compression level!");

            int bytesRead = 0;

            byte[] result = new byte[INITIAL_BUF_SIZE];

            while ((int) BROTLI_ENCODER_IS_FINISHED.invokeExact(brotliState) == BROTLI_FALSE) {
                exitCode = (int) BROTLI_ENCODER_COMPRESS_STREAM.invokeExact(
                        brotliState,
                        BrotliEncoderOperation.FINISH.ordinal(),
                        availInPtr,
                        nextInPtr,
                        availOutPtr,
                        (Addressable) MemoryAddress.NULL,
                        (Addressable) MemoryAddress.NULL
                );

                if (exitCode == BROTLI_FALSE)
                    throw new IllegalStateException("Compression error!");

                while ((int) BROTLI_ENCODER_HAS_MORE_OUTPUT.invokeExact(brotliState) == BROTLI_TRUE) {
                    if (bytesRead == result.length)
                        result = Arrays.copyOf(result, result.length + INITIAL_BUF_SIZE);

                    ctx.setSizeOut(result.length - bytesRead);

                    MemoryAddress compressedPtr = (MemoryAddress) BROTLI_ENCODER_TAKE_OUTPUT.invokeExact(brotliState, sizeOutPtr);

                    MemorySegment compressedData = MemorySegment.ofAddress(
                            compressedPtr,
                            ctx.getSizeOut(),
                            MemorySession.global()
                    );

                    MemorySegment.copy(compressedData, ValueLayout.JAVA_BYTE, 0, result, bytesRead, (int) compressedData.byteSize());

                    bytesRead += compressedData.byteSize();
                }
            }

            return Arrays.copyOf(result, bytesRead);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to perform compression!", e);
        } finally {
            try {
                BROTLI_ENCODER_DESTROY_INSTANCE.invokeExact(brotliState);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String decompress(byte[] data, int offset, int length) {
        Addressable brotliState;

        try {
            brotliState = (MemoryAddress) BROTLI_DECODER_CREATE_INSTANCE.invokeExact(
                    (Addressable) MemoryAddress.NULL,
                    (Addressable) MemoryAddress.NULL,
                    (Addressable) MemoryAddress.NULL
            );
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to initialize brotli!", e);
        }

        if (brotliState.equals(MemoryAddress.NULL))
            throw new IllegalStateException("Unable to initialize brotli decoder!");

        try (MemorySession memorySession = MemorySession.openConfined()) {
            MemorySegment dataBuf = MemorySegment.allocateNative(length, memorySession);
            MemorySegment.copy(data, offset, dataBuf, ValueLayout.JAVA_BYTE, 0, length);

            NativeContext ctx = new NativeContext(memorySession);

            ctx.setAvailableOut(0);
            ctx.setAvailableIn(dataBuf.byteSize());
            ctx.setNextIn(dataBuf);

            Addressable availOutPtr = ctx.getAvailableOutPtr();
            Addressable sizeOutPtr = ctx.getSizeOutPtr();
            Addressable availInPtr = ctx.getAvailableInPtr();
            Addressable nextInPtr = ctx.getNextInPtr();

            BrotliDecoderResult decodeResult;

            int bytesRead = 0;

            byte[] result = new byte[INITIAL_BUF_SIZE];

            do {
                int exitCode = (int) BROTLI_DECODER_DECOMPRESS_STREAM.invokeExact(
                        brotliState,
                        availInPtr,
                        nextInPtr,
                        availOutPtr,
                        (Addressable) MemoryAddress.NULL,
                        (Addressable) MemoryAddress.NULL
                );

                decodeResult = BrotliDecoderResult.values()[exitCode];

                switch (decodeResult) {
                    case NEEDS_MORE_OUTPUT, SUCCESS -> {
                        while ((int) BROTLI_DECODER_HAS_MORE_OUTPUT.invokeExact(brotliState) == BROTLI_TRUE) {
                            if (bytesRead == result.length)
                                result = Arrays.copyOf(result, result.length + INITIAL_BUF_SIZE);

                            ctx.setSizeOut(result.length - bytesRead);

                            MemoryAddress decompressedPtr = (MemoryAddress) BROTLI_DECODER_TAKE_OUTPUT.invokeExact(brotliState, sizeOutPtr);

                            MemorySegment decompressedData = MemorySegment.ofAddress(
                                    decompressedPtr,
                                    ctx.getSizeOut(),
                                    MemorySession.global()
                            );

                            MemorySegment.copy(decompressedData, ValueLayout.JAVA_BYTE, 0, result, bytesRead, (int) decompressedData.byteSize());

                            bytesRead += decompressedData.byteSize();

                            if (bytesRead > DECOMPRESSION_SIZE_LIMIT)
                                throw new IllegalStateException("Message is too long!");
                        }
                    }

                    default -> throw new IllegalStateException("Malformed data!");
                }
            } while (decodeResult != BrotliDecoderResult.SUCCESS);

            return new String(Arrays.copyOf(result, bytesRead), StandardCharsets.UTF_8);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to perform decompression!", e);
        } finally {
            try {
                BROTLI_DECODER_DESTROY_INSTANCE.invokeExact(brotliState);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private enum BrotliEncoderParam {
        MODE,
        QUALITY
    }

    private enum BrotliEncoderMode {
        GENERIC,
        TEXT
    }

    private enum BrotliEncoderOperation {
        PROCESS,
        FLUSH,
        FINISH
    }

    private enum BrotliDecoderResult {
        ERROR,
        SUCCESS,
        NEEDS_MORE_INPUT,
        NEEDS_MORE_OUTPUT
    }

    private static final class NativeContext {

        private static final GroupLayout CONTEXT_STRUCT = MemoryLayout.structLayout(
                ValueLayout.JAVA_LONG.withName("available_in"),
                ValueLayout.ADDRESS.withName("next_in"),
                ValueLayout.JAVA_LONG.withName("available_out"),
                ValueLayout.JAVA_LONG.withName("size_out")
        );

        private static final long AVAIL_OUT_OFFSET =
                CONTEXT_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("available_out"));

        private static final long SIZE_OUT_OFFSET =
                CONTEXT_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("size_out"));

        private static final long AVAIL_IN_OFFSET =
                CONTEXT_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("available_in"));

        private static final long NEXT_IN_OFFSET =
                CONTEXT_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("next_in"));

        private final MemorySegment segment;

        public NativeContext(MemorySession session) {
            segment = MemorySegment.allocateNative(CONTEXT_STRUCT, session);
        }

        public void setAvailableOut(long availableOut) {
            segment.set(ValueLayout.JAVA_LONG, AVAIL_OUT_OFFSET, availableOut);
        }

        public void setSizeOut(long sizeOut) {
            segment.set(ValueLayout.JAVA_LONG, SIZE_OUT_OFFSET, sizeOut);
        }

        public void setAvailableIn(long availableIn) {
            segment.set(ValueLayout.JAVA_LONG, AVAIL_IN_OFFSET, availableIn);
        }

        public void setNextIn(MemorySegment nextIn) {
            segment.set(ValueLayout.ADDRESS, NEXT_IN_OFFSET, nextIn);
        }

        public long getSizeOut() {
            return segment.get(ValueLayout.JAVA_LONG, SIZE_OUT_OFFSET);
        }

        public MemorySegment getAvailableOutPtr() {
            return segment.asSlice(AVAIL_OUT_OFFSET);
        }

        public MemorySegment getSizeOutPtr() {
            return segment.asSlice(SIZE_OUT_OFFSET);
        }

        public MemorySegment getAvailableInPtr() {
            return segment.asSlice(AVAIL_IN_OFFSET);
        }

        public MemorySegment getNextInPtr() {
            return segment.asSlice(NEXT_IN_OFFSET);
        }

    }

}
