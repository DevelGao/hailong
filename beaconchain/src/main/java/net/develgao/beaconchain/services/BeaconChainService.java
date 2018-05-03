package net.develgao.beaconchain.services;
import net.develgao.beaconchain.vrc.ValidatorRegisteredEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class BeaconChainService {

    public BeaconChainService(){
        EventBus eventBus = EventBusFactory.getInstance();
    }

    @Subscribe
    public void onValidatorRegistered(ValidatorRegisteredEvent validatorRegisteredEvent){

    }

}
