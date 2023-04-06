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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.util.function.IntConsumer;

public record DenseMask(boolean[] mask)
{
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    
    public boolean get(int position)
    {
        return mask[position];
    }

    public void forEach(IntConsumer action)
    {
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                action.accept(i);
            }
        }
    }

    public DenseVectorized toVectorized()
    {
        MemorySegment segment = MemorySegment.allocateNative(mask.length, 8, SegmentScope.auto());
        for (int i = 0; i < mask.length; i++) {
            segment.set(ValueLayout.JAVA_BOOLEAN, i, mask[i]);
        }

        return new DenseVectorized(segment);
    }

    public SparseMask toSparse()
    {
        int[] positions = new int[mask.length];
        int count = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                positions[count] = i;
                count++;
            }
        }

        return new SparseMask(positions, count);
    }
}
