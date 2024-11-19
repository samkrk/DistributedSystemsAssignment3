# Distributed Systems Assignment 3
#### Sam Kirk, a1851921

---

## Overview

This project simulates the **Paxos Consensus Algorithm** to determine the winner of the **Adelaide Suburbs Council Election**.

The simulation consists of multiple **council members**, each configured with unique IDs, roles, response delays, and communication ports. Members can act as either:
- **Proposers**: Individuals proposing themselves for election.
- **Acceptors**: Participants evaluating and responding to proposals.

### Configuration File Example
Below is an example JSON configuration file used to define the members:

```json
[
    {
        "id": "M1",
        "role": "PROPOSER",
        "responseDelay": 0,
        "port": 12345
    },
    {
        "id": "M2",
        "role": "ACCEPTOR",
        "responseDelay": 250,
        "port": 12346
    }
]
```

The "id" and "port" fields must be unique for each member as they are used to identify the members and in turn communicate with each-other. If a member personally wants to be elected, their "role" is a "PROPOSER", otherwise the member is an "ACCEPTOR". The "responseDelay" is given in milliseconds, and to simulate a Proposer proposing themselves and then disconnecting forever, a response delay of 12345  can be used (see test 4). 

The entry point for the simulation is the Main class, which requires a path to a given configuration file. The configuration file details are extracted using the ConfigLoader class, and then passed into the ElectionServer class, which will take control of the simulation. 

The ElectionServer class uses the details of the given member configurations to spawn in council members, with their respective response delays and ambitions. Depending on the role of the member, either the Acceptor class, or the Proposer class is instantiated. Both of these classes are inherited from the CouncilMember class as they share many methods for sending and receiving messages. 

One the Proposers and Acceptors are created, each member is started in its own thread, and the proposers will initiate their proposals. From here a number of messages will be sent back and forth between the Acceptors and Proposers, including PREPARE, PROMISE, ACCEPT_REQUEST, ACCEPTED, LEARN and REJECT messages. Most of these messages are logged to the System output, although some were muted to improve the readability of the logs. Finally, once the Paxos Algorithm is completed, meaning a majority of acceptors have accepted a single proposal, and the rest of the nodes are learning the value, the result will be logged to the terminal and the system will shutdown.  

## Dependencies 
Ensure the following dependencies are available in the lib/ directory
* Gson: `gson-2.8.9.jar` (For Json parsing)
* Junit: `junit-4.13.2.jar` (For unit tests)
* Hamcrest: `hamcrest-core-1.3.jar` (Used by Junit)

## Build Instructions 
To build and run the assignment, run 
`make run`. This will run the simulation with the `test1.json` configuration. To change the config file, edit the CONFIG_FILE variable in the Makefile from `CONFIG_FILE = src/main/resources/test1.json` to `CONFIG_FILE = src/main/resources/test2.json` or `test3.json`, or `test4.json`.

Alternatively, run all the configuration files at once using the junit test: `make test`.

## Test Overview \& Results 
* The first simulation simply tested that implementation works when two members send voting proposals at the same time. 5 members are used (3 Acceptors and 2 Proposers), with no response delay. 
* The second test involved 9 members, with 3 Proposers. Again this test involved no response delay.
* The third simulation had the same member distribution as test 2, but this test introduced varying response delays, from 0 to 2000 milliseconds. 
* Finally test 4 used the same 9 member configuration as the last two simulations, but it introduced a response delay of 12345 milliseconds for proposers M2 and M3. My implementation interprets this exact response delay as the proposers initiating their proposal and then going offline forever. 

The results for all of these tests can be found in the *results.pdf* file.  


