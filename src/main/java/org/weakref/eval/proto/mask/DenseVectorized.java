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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.function.IntConsumer;

public record DenseVectorized(MemorySegment segment)
{
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

    public void forEach(IntConsumer action)
    {
        MemorySegment data = segment;
        int i = 0;
        while (i < BYTE_SPECIES.loopBound(data.byteSize())) {
            var inputVector = ByteVector.fromMemorySegment(BYTE_SPECIES, data, i, ByteOrder.nativeOrder());
            VectorMask<Byte> isTrue = inputVector.eq((byte) 1);
            if (isTrue.anyTrue()) {
                for (int j = 0; j < BYTE_SPECIES.length(); j++) {
                    if (isTrue.laneIsSet(j)) {
                        action.accept(i + j);
                    }
                }
            }

//            if (inputVector.reduceLanes(VectorOperators.OR) > 0) {
//                for (int j = 0; j < BYTE_SPECIES.length(); j++) {
//                    if (inputVector.lane(j) > 0) {
//                        action.accept(i + j);
//                    }
//                }
//            }
            i += BYTE_SPECIES.length();
        }

        for (; i < data.byteSize(); i++) {
            if (data.get(ValueLayout.JAVA_BOOLEAN, i)) {
                action.accept(i);
            }
        }
    }
}
