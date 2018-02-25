package net.develgao.beaconchain.datastructures;

import net.develgao.beaconchain.ethereum.core.Hash;
import org.web3j.abi.datatypes.generated.Int64;

public class CrosslinkRecord {

    private Hash hash;
    private Int64 dynasty;
    private Int64 slot;

    public CrosslinkRecord() {

    }

}
