import random
import simpy
import pdb
import networkx as nx
import random

RANDOM_SEED = 42
NUM_ROBOTS = 1     # Number of machines in the machine shop
WEEKS = 4              # Simulation time in weeks


def time_per_part():
    return 5


def mercedes():
    G = nx.DiGraph()
    G.add_edge('Engine', 'Transmission')
    G.add_edge('Engine', 'TransmissionMount')
    G.add_edge('Sides', 'SidesL')
    G.add_edge('Sides', 'SidesR')
    G.add_edge('Lights', 'LightsL')
    G.add_edge('Lights', 'LightsR')
    G.add_edge('Back', 'Trunk')
    G.add_edge('Back', 'Spoiler')
    G.add_edge('Wheels', 'Wheels1')
    G.add_edge('Wheels', 'Wheels2')
    G.add_edge('Wheels', 'Wheels3')
    G.add_edge('Wheels', 'Wheels4')
    G.add_edge('Chassis', 'Engine')
    G.add_edge('Chassis', 'Sides')
    G.add_edge('Chassis', 'Lights')
    G.add_edge('Chassis', 'Interior')
    G.add_edge('Chassis', 'Skin')
    G.add_edge('Chassis', 'Cockpit')
    G.add_edge('Chassis', 'Back')
    G.add_edge('Chassis', 'Wheels')
    G.add_edge('Car', 'Front')
    G.add_edge('Car', 'Chassis')
    G.add_edge('Car', 'Back')
    print('Mercedes has %s nodes and %s edges' % (len(G.nodes()), len(G.edges())))
    return G


def Planner(env, store, htn):
    done = False

    tasks = [x[1] for x in list(nx.bfs_tree(htn, 'Car').edges())]
    tasks = list(reversed(tasks))
    for task in tasks:
        yield store.put((task, 5))


def Robot(name, env, store):
    while True:
        print('Robot', name, 'requesting lego module at', env.now)
        item = yield store.get()
        print(name, 'got', item, 'at', env.now)
        yield env.timeout(item[1])


print('Lego shop')

# Create an environment and start the setup process
env = simpy.Environment()
htn = mercedes()
store = simpy.Store(env, capacity=2)
prod = env.process(Planner(env,store,htn))
robots = [env.process(Robot(i, env, store)) for i in range(2)]

# Execute!
env.run(until=100)



