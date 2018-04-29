package net.develgao.beaconchain.services;

import net.develgao.beaconchain.services.PowchainService;

public class PowchainFactory {

    private static final PowchainService powchainService = new PowchainService();

    public static PowchainService getInstance() {
        return powchainService;
    }
}
