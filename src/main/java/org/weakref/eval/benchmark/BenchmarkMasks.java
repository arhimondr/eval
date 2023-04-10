/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.weakref.eval.benchmark;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import org.weakref.eval.proto.mask.DenseMask;
import org.weakref.eval.proto.mask.DenseVectorized;
import org.weakref.eval.proto.mask.Masks;
import org.weakref.eval.proto.mask.SparseMask;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.weakref.eval.benchmark.BenchmarkRunner.benchmark;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:CompileCommand=print,*core*.*",
//        "-XX:PrintAssemblyOptions=intel"
})
@Warmup(iterations = 10, timeUnit = TimeUnit.MILLISECONDS, time = 500)
@Measurement(iterations = 10, timeUnit = TimeUnit.MILLISECONDS, time = 500)
@State(Scope.Thread)
public class BenchmarkMasks
{
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;

    @Param({"1024"})
    public int positions;

//    @Param({"1"})
    @Param({"0", "0.01", "0.1", "0.5", "1"})
    public double selectivity = 0.5;

    private DenseMask dense;
    private DenseVectorized denseVectorized;
    private SparseMask sparse;

    public MemorySegment bytesSegment1;
    public MemorySegment bytesSegment2;
    public MemorySegment bytesSegmentResult;

    public MemorySegment integersSegment1;
    public MemorySegment integersSegment2;
    public MemorySegment integersSegmentResult;

    public MemorySegment longsSegment1;
    public MemorySegment longsSegment2;
    public MemorySegment longsSegmentResult;

    @Setup
    public void setup()
    {
        dense = Masks.randomDenseMask(positions, selectivity);
        denseVectorized = dense.toVectorized();
        sparse = dense.toSparse();

        bytesSegment1 = allocateSegment(positions, true);
        bytesSegment2 = allocateSegment(positions, true);
        bytesSegmentResult = allocateSegment(positions, true);

        integersSegment1 = allocateSegment(positions * Integer.BYTES, true);
        integersSegment2 = allocateSegment(positions * Integer.BYTES, true);
        integersSegmentResult = allocateSegment(positions * Integer.BYTES, true);

        longsSegment1 = allocateSegment(positions * Long.BYTES, true);
        longsSegment2 = allocateSegment(positions * Long.BYTES, true);
        longsSegmentResult = allocateSegment(positions * Long.BYTES, true);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < positions; i++) {
            long value1 = random.nextLong();
            long value2 = random.nextLong();

            bytesSegment1.set(ValueLayout.JAVA_BYTE, i, (byte) value1);
            bytesSegment2.set(ValueLayout.JAVA_BYTE, i, (byte) value2);

            integersSegment1.setAtIndex(ValueLayout.JAVA_INT, i, (int) value1);
            integersSegment2.setAtIndex(ValueLayout.JAVA_INT, i, (int) value2);

            longsSegment1.setAtIndex(ValueLayout.JAVA_LONG, i, value1);
            longsSegment2.setAtIndex(ValueLayout.JAVA_LONG, i, value2);
        }
    }

    private MemorySegment allocateSegment(int size, boolean direct)
    {
        if (direct) {
            return MemorySegment.allocateNative(size, 8, SegmentScope.auto());
        }
        else {
            return MemorySegment.ofArray(new long[size / 8 + 1]);
        }
    }

//    @Benchmark
//    public void dense()
//    {
//        dense.forEach(this::consume);
//    }

//    @Benchmark
//    public void denseVectorized()
//    {
//        denseVectorized.forEach(this::consume);
//    }

//    @Benchmark
//    public void sparse()
//    {
//        sparse.forEach(this::consume);
//    }

//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void consume(int position)
//    {
//    }

//    @Benchmark
//    public MemorySegment sumByteVectorsDenseVectorized()
//    {
//        MemorySegment segment1 = bytesSegment1;
//        MemorySegment segment2 = bytesSegment2;
//        MemorySegment mask = denseVectorized.segment();
//        MemorySegment result = bytesSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = BYTE_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += BYTE_SPECIES.length()) {
//            ByteVector maskVector = ByteVector.fromMemorySegment(BYTE_SPECIES, mask, i, ByteOrder.nativeOrder());
//            VectorMask<Byte> isTrue = maskVector.eq((byte) 1);
//            if (isTrue.anyTrue()) {
//                ByteVector v1 = ByteVector.fromMemorySegment(BYTE_SPECIES, segment1, i, ByteOrder.nativeOrder());
//                ByteVector v2 = ByteVector.fromMemorySegment(BYTE_SPECIES, segment2, i, ByteOrder.nativeOrder());
//                v1.add(v2).intoMemorySegment(result, i, ByteOrder.nativeOrder());
//            }
//        }
//        for (; i < positions; i++) {
//            if (mask.get(ValueLayout.JAVA_BOOLEAN, i)) {
//                result.set(ValueLayout.JAVA_BYTE, i, (byte) (segment1.get(ValueLayout.JAVA_BYTE, i) + segment2.get(ValueLayout.JAVA_BYTE, i)));
//            }
//        }
//        return result;
//    }
//
//    @Benchmark
//    public MemorySegment sumIntegerVectorsDenseVectorized()
//    {
//        MemorySegment segment1 = integersSegment1;
//        MemorySegment segment2 = integersSegment2;
//        MemorySegment mask = denseVectorized.segment();
//        MemorySegment result = integersSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = BYTE_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += BYTE_SPECIES.length()) {
//            ByteVector maskVector = ByteVector.fromMemorySegment(BYTE_SPECIES, mask, i, ByteOrder.nativeOrder());
//            VectorMask<Byte> isTrue = maskVector.eq((byte) 1);
//            if (isTrue.anyTrue()) {
//                for (int j = i; j < i + BYTE_SPECIES.length(); j += INT_SPECIES.length()) {
//                    IntVector v1 = IntVector.fromMemorySegment(INT_SPECIES, segment1, (long) j * Integer.BYTES, ByteOrder.nativeOrder());
//                    IntVector v2 = IntVector.fromMemorySegment(INT_SPECIES, segment2, (long) j * Integer.BYTES, ByteOrder.nativeOrder());
//                    v1.add(v2).intoMemorySegment(result, (long) j * Integer.BYTES, ByteOrder.nativeOrder());
//                }
//            }
//        }
//
//        for (; i < positions; i++) {
//            if (mask.get(ValueLayout.JAVA_BOOLEAN, i)) {
//                result.setAtIndex(ValueLayout.JAVA_INT, i, segment1.getAtIndex(ValueLayout.JAVA_INT, i) + segment2.getAtIndex(ValueLayout.JAVA_INT, i));
//            }
//        }
//        return result;
//    }
//
//    @Benchmark
//    public MemorySegment sumLongVectorsDenseVectorized()
//    {
//        MemorySegment segment1 = longsSegment1;
//        MemorySegment segment2 = longsSegment2;
//        MemorySegment mask = denseVectorized.segment();
//        MemorySegment result = longsSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = BYTE_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += BYTE_SPECIES.length()) {
//            ByteVector maskVector = ByteVector.fromMemorySegment(BYTE_SPECIES, mask, i, ByteOrder.nativeOrder());
//            VectorMask<Byte> isTrue = maskVector.eq((byte) 1);
//            if (isTrue.anyTrue()) {
//                for (int j = i; j < i + BYTE_SPECIES.length(); j += LONG_SPECIES.length()) {
//                    LongVector v1 = LongVector.fromMemorySegment(LONG_SPECIES, segment1, (long) j * Long.BYTES, ByteOrder.nativeOrder());
//                    LongVector v2 = LongVector.fromMemorySegment(LONG_SPECIES, segment2, (long) j * Long.BYTES, ByteOrder.nativeOrder());
//                    v1.add(v2).intoMemorySegment(result, (long) j * Long.BYTES, ByteOrder.nativeOrder());
//                }
//            }
//        }
//
//        for (; i < positions; i++) {
//            if (mask.get(ValueLayout.JAVA_BOOLEAN, i)) {
//                result.setAtIndex(ValueLayout.JAVA_LONG, i, segment1.getAtIndex(ValueLayout.JAVA_LONG, i) + segment2.getAtIndex(ValueLayout.JAVA_LONG, i));
//            }
//        }
//        return result;
//    }

//    @Benchmark
//    public MemorySegment sumByteVectors()
//    {
//        MemorySegment segment1 = bytesSegment1;
//        MemorySegment segment2 = bytesSegment2;
//        MemorySegment result = bytesSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = BYTE_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += BYTE_SPECIES.length()) {
//            ByteVector v1 = ByteVector.fromMemorySegment(BYTE_SPECIES, segment1, i, ByteOrder.nativeOrder());
//            ByteVector v2 = ByteVector.fromMemorySegment(BYTE_SPECIES, segment2, i, ByteOrder.nativeOrder());
//            v1.add(v2).intoMemorySegment(result, i, ByteOrder.nativeOrder());
//        }
//        for (; i < positions; i++) {
//            result.set(ValueLayout.JAVA_BYTE, i, (byte) (segment1.get(ValueLayout.JAVA_BYTE, i) + segment2.get(ValueLayout.JAVA_BYTE, i)));
//        }
//        return result;
//    }
//
//    @Benchmark
//    public MemorySegment sumIntegerVectors()
//    {
//        MemorySegment segment1 = integersSegment1;
//        MemorySegment segment2 = integersSegment2;
//        MemorySegment result = integersSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = INT_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += INT_SPECIES.length()) {
//            IntVector v1 = IntVector.fromMemorySegment(INT_SPECIES, segment1, (long) i * Integer.BYTES, ByteOrder.nativeOrder());
//            IntVector v2 = IntVector.fromMemorySegment(INT_SPECIES, segment2, (long) i * Integer.BYTES, ByteOrder.nativeOrder());
//            v1.add(v2).intoMemorySegment(result, (long) i * Integer.BYTES, ByteOrder.nativeOrder());
//        }
//        for (; i < positions; i++) {
//            result.setAtIndex(ValueLayout.JAVA_INT, i, segment1.getAtIndex(ValueLayout.JAVA_INT, i) + segment2.getAtIndex(ValueLayout.JAVA_INT, i));
//        }
//        return result;
//    }
//
//    @Benchmark
//    public MemorySegment sumLongVectors()
//    {
//        MemorySegment segment1 = longsSegment1;
//        MemorySegment segment2 = longsSegment2;
//        MemorySegment result = longsSegmentResult;
//        int positions = this.positions;
//
//        var upperBound = LONG_SPECIES.loopBound(positions);
//        var i = 0;
//        for (; i < upperBound; i += LONG_SPECIES.length()) {
//            LongVector v1 = LongVector.fromMemorySegment(LONG_SPECIES, segment1, (long) i * Long.BYTES, ByteOrder.nativeOrder());
//            LongVector v2 = LongVector.fromMemorySegment(LONG_SPECIES, segment2, (long) i * Long.BYTES, ByteOrder.nativeOrder());
//            v1.add(v2).intoMemorySegment(result, (long) i * Long.BYTES, ByteOrder.nativeOrder());
//        }
//        for (; i < positions; i++) {
//            result.setAtIndex(ValueLayout.JAVA_LONG, i, segment1.getAtIndex(ValueLayout.JAVA_LONG, i) + segment2.getAtIndex(ValueLayout.JAVA_LONG, i));
//        }
//        return result;
//    }

    @Benchmark
    public MemorySegment sumByteVectorsSparse()
    {
        MemorySegment segment1 = bytesSegment1;
        MemorySegment segment2 = bytesSegment2;
        MemorySegment result = bytesSegmentResult;
        MemorySegment mask = sparse.getSegment();
        int positions = (int) mask.byteSize() / Integer.BYTES;

        for (int i = 0; i < positions; i++) {
            int position = mask.getAtIndex(ValueLayout.JAVA_INT, i);
            result.set(ValueLayout.JAVA_BYTE, position, (byte) (segment1.get(ValueLayout.JAVA_BYTE, position) + segment2.get(ValueLayout.JAVA_BYTE, position)));
        }
        return result;
    }

    @Benchmark
    public MemorySegment sumIntegerVectorsSparse()
    {
        MemorySegment segment1 = integersSegment1;
        MemorySegment segment2 = integersSegment2;
        MemorySegment result = integersSegmentResult;
        MemorySegment mask = sparse.getSegment();
        int positions = (int) mask.byteSize() / Integer.BYTES;

        for (int i = 0; i < positions; i++) {
            int position = mask.getAtIndex(ValueLayout.JAVA_INT, i);
            result.setAtIndex(ValueLayout.JAVA_INT, position, segment1.getAtIndex(ValueLayout.JAVA_INT, position) + segment2.getAtIndex(ValueLayout.JAVA_INT, position));
        }
        return result;
    }

    @Benchmark
    public MemorySegment sumLongVectorsSparse()
    {
        MemorySegment segment1 = longsSegment1;
        MemorySegment segment2 = longsSegment2;
        MemorySegment result = longsSegmentResult;
        MemorySegment mask = sparse.getSegment();
        int positions = (int) mask.byteSize() / Integer.BYTES;

        for (int i = 0; i < positions; i++) {
            int position = mask.getAtIndex(ValueLayout.JAVA_INT, i);
            result.setAtIndex(ValueLayout.JAVA_LONG, position, segment1.getAtIndex(ValueLayout.JAVA_LONG, position) + segment2.getAtIndex(ValueLayout.JAVA_LONG, position));
        }
        return result;
    }

    public static void main(String[] args)
            throws RunnerException
    {
        benchmark(BenchmarkMasks.class);
    }
}
