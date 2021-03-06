#!/bin/sh



# "Usage: bash run_hailong.sh  [validator_count] [owned_validator_start_index] [owned_validator_count] [peers] [genesis_file] [interop_active]"
#
#
#
# Static Peering
# Run multiclient in interop mode:
# 16 validators
# validator start index = 0
# num of owned validators 16
# /ip4/127.0.0.1/tcp/19001 address of peer
# 10 sec delay from genesis
# 123454 genesis time
# /tmp/genesis.ssz is the genesis file
#   sh run_hailong.sh 16 0 16 /ip4/127.0.0.1/tcp/19001 /tmp/genesis.ssz true
#

export VALIDATOR_COUNT=$1
export OWNED_VALIDATOR_START_INDEX=$2
export OWNED_VALIDATOR_COUNT=$3
export PEERS=$4
export GENESIS_FILE=$5
INTEROP_MODE=$6

#BOOTNODE_ENR=$(cat ~/.mothra/network/enr.dat)

#CURRENT_TIME=$(date +%s)
#GENESIS_TIME=$((CURRENT_TIME + START_DELAY))


SCRIPT_DIR=`pwd`
CONFIG_DIR=$SCRIPT_DIR/../config

source $SCRIPT_DIR/run_utils.sh

rm -rf ./demo
mkdir -p ./demo
rm -f ../config/runConfig.*

NODE_INDEX=0
NUM_NODES=1

configure_node "mothra" $NODE_INDEX $NUM_NODES "$CONFIG_DIR/config.toml"
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" numValidators $VALIDATOR_COUNT
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" numNodes $NUM_NODES
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" active $INTEROP_MODE
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" genesisTime $GENESIS_TIME
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" ownedValidatorStartIndex $OWNED_VALIDATOR_START_INDEX
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" ownedValidatorCount $OWNED_VALIDATOR_COUNT
sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" startState "\"$GENESIS_FILE"\"


if [ "$PEERS" != "" ]
then
     HAILONG_PEERS=$(echo $PEERS | awk '{gsub(/\./,"\\.")}1' | awk '{gsub(/\//,"\\/")}1')
     HAILONG_PEERS=$(echo [\"$HAILONG_PEERS\"] )
     sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" peers $HAILONG_PEERS
     sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" discovery "\"static\""
     sh configurator.sh "$CONFIG_DIR/runConfig.0.toml" isBootnode false
fi

cd $SCRIPT_DIR/demo/node_0/ && ./hailong --config=$CONFIG_DIR/runConfig.0.toml --logging=INFO

