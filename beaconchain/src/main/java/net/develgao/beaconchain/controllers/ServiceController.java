package net.develgao.beaconchain.controllers;

import net.develgao.beaconchain.services.EventBusFactory;
import net.develgao.beaconchain.services.PowchainFactory;
import net.develgao.beaconchain.services.PowchainService;

import com.google.common.eventbus.EventBus;

public class ServiceController {
    private PowchainService powchainService;
    private EventBus eventBus;

    public ServiceController(){
        this.powchainService = PowchainFactory.getInstance();
        this.eventBus = EventBusFactory.getInstance();
        this.init();
    }

    // initialize/register all services
    public void init(){

        // PoWchain Service
       this.eventBus.register(this.powchainService);

        // Blockchain Service

        // Validator Service

        // P2P Service

        // RPC Service
    }

    public void start(){
        // start all services
        this.powchainService.start();

    }

    public void stop(){
        // stop all services
    }
}
