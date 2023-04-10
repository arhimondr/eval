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
package org.weakref.eval.proto.mask;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public class SparseMask
{
    private final int[] positions;
    private int count;
    private final MemorySegment segment;

    public SparseMask(int[] positions, int count)
    {
        this.positions = positions;
        this.count = count;
        this.segment = MemorySegment.allocateNative(Integer.BYTES * count, Integer.BYTES, SegmentScope.auto());
        for (int i = 0; i < count; i++) {
            segment.setAtIndex(ValueLayout.JAVA_INT, i, positions[i]);
        }
    }

    public MemorySegment getSegment()
    {
        return segment;
    }

    public void forEach(IntConsumer action)
    {
        for (int i = 0; i < count; i++) {
            action.accept(positions[i]);
        }
    }

    public DenseMask toDense()
    {
        boolean[] mask = new boolean[positions.length];
        for (int position : positions) {
            mask[position] = true;
        }

        return new DenseMask(mask);
    }

    public void filter(IntPredicate predicate)
    {
        count = filter(count, positions, positions, predicate);
    }

    /**
     * input and output can refer to the same array for in-place filtering
     */
    private static int filter(int count, int[] input, int[] output, IntPredicate predicate)
    {
        int out = 0;
        for (int in = 0; in < count; in++) {
            int position = input[in];
            output[out] = position;
            out += predicate.test(position) ? 1 : 0;
        }

        return out;
    }
}
