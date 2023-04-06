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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
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
    private final static int POSITIONS = 1024;

    @Param({"0", "0.01", "0.1", "0.5", "1"})
    public double selectivity = 0.5;

    private DenseMask dense;
    private DenseVectorized denseVectorized;
    private SparseMask sparse;

    @Setup
    public void setup()
    {
        dense = Masks.randomDenseMask(POSITIONS, selectivity);
        denseVectorized = dense.toVectorized();
        sparse = dense.toSparse();
    }

//    @Benchmark
//    public void dense()
//    {
//        dense.forEach(this::consume);
//    }

    @Benchmark
    public void denseVectorized()
    {
        denseVectorized.forEach(this::consume);
    }

//    @Benchmark
//    public void sparse()
//    {
//        sparse.forEach(this::consume);
//    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void consume(int position)
    {
    }

    public static void main(String[] args)
            throws RunnerException
    {
        benchmark(BenchmarkMasks.class);
    }
}
