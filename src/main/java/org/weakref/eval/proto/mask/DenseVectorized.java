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
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.util.function.IntConsumer;

public record DenseVectorized(byte[] mask)
{
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

    public boolean get(int position)
    {
        return mask[position] != 0;
    }

    public void forEach(IntConsumer action)
    {
        int i = 0;
        for (; i < BYTE_SPECIES.loopBound(mask.length); i += BYTE_SPECIES.length()) {
            var inputVector = ByteVector.fromArray(BYTE_SPECIES, mask, i);
            VectorMask<Byte> isTrue = inputVector.eq((byte) 0).not();
            if (isTrue.anyTrue()) {
                for (int j = 0; j < BYTE_SPECIES.length(); j++) {
                    if (isTrue.laneIsSet(j)) {
                        action.accept(i + j);
                    }
                }
            }
        }

        for (; i < mask.length; i++) {
            if (mask[i] != 0) {
                action.accept(i);
            }
        }
    }

    public SparseMask toSparse()
    {
        int[] positions = new int[mask.length];
        int count = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] != 0) {
                positions[count] = i;
                count++;
            }
        }

        return new SparseMask(positions, count);
    }
}
