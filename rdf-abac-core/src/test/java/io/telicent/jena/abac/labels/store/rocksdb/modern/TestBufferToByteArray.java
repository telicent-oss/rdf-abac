package io.telicent.jena.abac.labels.store.rocksdb.modern;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class TestBufferToByteArray {

    private static Stream<Arguments> buffers() {
        return Stream.of(Arguments.of("empty", new byte[0]),
                         Arguments.of("tiny", "test".getBytes(StandardCharsets.UTF_8)),
                         Arguments.of("random 128", RandomUtils.insecure().randomBytes(128)),
                         Arguments.of("random 1k", RandomUtils.insecure().randomBytes(1024)),
                         Arguments.of("random 8k", RandomUtils.insecure().randomBytes(8 * 1024)));
    }

    @ParameterizedTest(name = "Large Buffer Read After Write - {0}")
    @MethodSource("buffers")
    public void givenLargeBuffer_whenWritingLessBytesThanBufferSize_thenAsByteArrayReturnsOnlyWrittenBytes(String name,
                                                                                                           byte[] data) {
        // Given
        ByteBuffer large = ByteBuffer.allocateDirect(1024 * 1024);

        // When
        large.put(data);

        // Then
        large.flip();
        byte[] read = DictionaryLabelStoreRocksDB.asByteArray(large);
        Assertions.assertArrayEquals(data, read);

    }
}
