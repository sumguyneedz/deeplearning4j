package org.deeplearning4j.spark.impl.common.repartition;

import lombok.AllArgsConstructor;
import org.apache.spark.Partitioner;

/**
 * This is a custom partitioner (used in conjunction with {@link AssignIndexFunction} to repartition a RDD.
 * Unlike a standard .repartition() call (which assigns partitions like [2,3,4,1,2,3,4,1,2,...] for 4 partitions],
 * this function attempts to keep contiguous elements (i.e., those elements originally in the same partition) together
 * much more frequently. Furthermore, it is less prone to producing larger or smaller than expected partitions, as
 * it is entirely deterministic, whereas .repartition() has a degree of randomness (i.e., start index) which can result in
 * a large degree of variance when the number of elements in the original partitions is small (as is the case generally in DL4J)
 *
 * @author Alex Black
 */
@AllArgsConstructor
public class BalancedPartitioner extends Partitioner {
    private final int numPartitions;            //Total number of partitions
    private final int numStandardPartitions;    //Number of partitions of standard size; these are 1 larger than the others (==numPartitions where total number of examples is divisible into numPartitions without remainder)
    private final int elementsPerPartition;

    @Override
    public int numPartitions() {
        return numPartitions;
    }

    @Override
    public int getPartition(Object key) {
        int elementIdx = (Integer)key;

        //First 'numStandardPartitions' executors get "elementsPerPartition" each; the remainder get
        // elementsPerPartition-1 each. This is because the total number of examples might not be an exact multiple
        // of the number of cores in the cluster

        //Work out: which partition it belongs to...
        if(elementIdx <= elementsPerPartition * numStandardPartitions){
            //This goes into one of the standard partitions (of size 'elementsPerPartition')
            return elementIdx / elementsPerPartition;
        } else {
            //This goes into one of the smaller partitions (of size elementsPerPartition - 1)
            int numValsInStdPartitions = elementsPerPartition * numStandardPartitions;
            int idxInSmallerPartitions = elementIdx - numValsInStdPartitions;
            int smallPartitionIdx = idxInSmallerPartitions / (elementsPerPartition-1);
            return numStandardPartitions + smallPartitionIdx;
        }
    }
}
