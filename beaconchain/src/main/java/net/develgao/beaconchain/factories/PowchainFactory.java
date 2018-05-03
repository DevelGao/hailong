package net.develgao.beaconchain.services;



public class PowchainFactory {

    private static final PowchainService powchainService = new PowchainService();

    public static PowchainService getInstance() {
        return powchainService;
    }
}
