import os
import sys
import pdb

import numpy as np
import networkx as nx
from networkx.drawing.nx_pydot import write_dot

# LDraw units (LDUs)
# http://www.brickwiki.info/wiki/LDraw_unit
LDU = dict(
    BRICK_WIDTH = 20,
    BRICK_HEIGHT = 24,
    PLATE_HEIGHT = 8,
    STUD_DIAMETER = 12,
    STUD_HEIGHT = 4,
)


class Lego:
    def __init__(self, color, x, y, z, mat, lego_ref):
        self.x = x
        self.y = y
        self.z = z
        self.color = color
        self.lego_ref = lego_ref
        self.mat = mat 
        self.rotation = self.rotation_degrees()
        self.type, self.width, self.length, self.height = self.id_lego_ref(lego_ref)
        self.corners = self.bounding_box()

    def __repr__(self):
        return '<Lego %s %sx%sx%s, (%s,%s,%s), Rotation %s, %s>' % (
            self.type, self.width, self.length, self.height,
            self.x, self.y, self.z, self.rotation, self.lego_ref,
        )

    # The naming convention is inconsistent. This function tries to cover all the corner cases I've found
    def id_lego_ref(self, lego_ref):
        with open('parts/%s' % (lego_ref), 'r') as f:
            header = f.readline()
        header = header.strip()
        elts = header.split(' ')
        lego_type = elts[1]

        size = header.split('x')
        size = [x.strip() for x in size]
        assert len(size) >= 2
        size_width = int(size[0][-1])    # assumes a single digit
        size_length = int(size[1][0:2])  # assumes double digits
 
        if len(size) == 2:
           size_height = 1
        elif len(size) > 2:
            size_height = int(size[2])
        else:
            raise ValueError('Error, format not recognized by the parser')

        if self.rotation == 90 or self.rotation == 270:
            length = int(size_width) * LDU['BRICK_WIDTH']
            width = int(size_length) * LDU['BRICK_WIDTH']
        else:
            width = int(size_width) * LDU['BRICK_WIDTH']
            length = int(size_length) * LDU['BRICK_WIDTH']

        if 'Brick' in lego_type:
            height = size_height * LDU['BRICK_HEIGHT']
        elif 'Plate' in lego_type:
            height = LDU['PLATE_HEIGHT']
        else:
            raise ValueError('Error! do not know how to process %s' % lego_type)

        return lego_type, width, length, height

    def bounding_box(self):
        origin = np.array([self.x, self.y, self.z])

        corners = [None] * 8
        corners[0] = origin + np.array([self.length/2, 0, self.width/2])
        corners[1] = origin + np.array([self.length/2, 0, -self.width/2])
        corners[2] = origin + np.array([-self.length/2, 0, -self.width/2])
        corners[3] = origin + np.array([-self.length/2, 0, self.width/2])
        corners[4] = corners[0] + np.array([0, self.height, 0])
        corners[5] = corners[1] + np.array([0, self.height, 0])
        corners[6] = corners[2] + np.array([0, self.height, 0])
        corners[7] = corners[3] + np.array([0, self.height, 0])

        return corners

    def rotation_degrees(self):
        mat = self.mat.round()
        if np.array_equal(mat, np.array([[1.,0.,0.],[0.,1.,0.],[0.,0.,1.]])):
            return 0
        elif np.array_equal(mat, np.array([[0.,0.,1.],[0.,1.,0.],[-1.,0.,0.]])):
            return 90
        elif np.array_equal(mat, np.array([[-1.,0.,0.],[0.,1.,0.],[0.,0.,-1.]])):
            return 180
        elif np.array_equal(mat, np.array([[0.,0.,-1.],[0.,1.,0.],[1.,0.,0.]])):
            return 270

    def top_ycoord(self):
        return self.corners[0][1]

    def bottom_ycoord(self):
        return self.corners[4][1]
    
    def y_in_brick_units(self):
        return self.y / LDU['BRICK_HEIGHT']


def parse_ldr_reference(line):
    ref = line.split(' ')
    color = parse_color(int(ref[1]))
    x = round(float(ref[2]))
    y = round(float(ref[3]))
    z = round(float(ref[4]))
    mat = np.array(ref[5:14], dtype='float').reshape((3,3))
    lego_ref = ref[14]
    return Lego(color, x, y, z, mat, lego_ref)

def parse_color(colornumber):
    return {
        0 : 'black',
        1 : 'blue' ,
        2 : 'green',
        3 : 'green', #Dark_Turquoise
        4 : 'red'  ,
        5 : 'pink' ,
        6 : 'brown',
        7 : 'grey' ,
        8 : 'grey' ,
        14: 'yellow',
        15 : 'white',
        19 : 'white',
        25 : 'orange',
        27 : 'lime',
        70 : 'brown',
        72 : 'grey',
        320: 'red',
        484: 'orange'
    }.get(colornumber, 'white')

def parse_ldr(filename):
    with open(filename, 'r') as f:
        content = f.readlines()
    content = [x.strip() for x in content]

    lego_count = 0
    lego_blocks = []
    for line in content:
        if line[0] == '1':
            lego_count += 1
            lego = parse_ldr_reference(line)
            print(lego_count, lego)
            lego_blocks.append(lego)

    print('File has %s legos' % (len(lego_blocks)))
    return lego_blocks


# Adapted from https://gamedev.stackexchange.com/questions/586/what-is-the-fastest-way-to-work-out-2d-bounding-box-intersection
def is_connected(a, b):
    if a.top_ycoord() == b.bottom_ycoord():
        return ((abs(a.x - b.x) * 2 < (a.length + b.length)) and 
                (abs(a.z - b.z) * 2 < (a.width + b.width)))
    return False
 

def build_connectivity_graph(legos):
    """ Create connectivity graph from list of lego blocks.
    
    Number the nodes from 1 to n to allow easy comparison with Julia.
    """
    graph = nx.DiGraph()
    
    # Create node for each lego block.
    for i, lego in enumerate(legos):
        graph.add_node(i+1,
            type=lego.type, ref=lego.lego_ref, color = lego.color,
            width=lego.width, length=lego.length, height=lego.height,
            x=lego.x, y=lego.y, z=lego.z, rotation=lego.rotation,
        )
    
    # Add edges for connected lego blocks.
    for i, src in enumerate(legos):
        for j, tgt in enumerate(legos):
            if is_connected(src, tgt):
                layer = src.y_in_brick_units() # layer at which pieces connect
                graph.add_edge(i+1, j+1, layer=layer)
    
    return graph


def partition_graph_metis(G, n_partitions=3):
    import metis
    assert nx.is_connected(G.to_undirected())
    cuts, parts = metis.part_graph(G.to_undirected(), n_partitions, contig=True)
    colors = ['red', 'blue', 'green', 'purple']
    for i, part in enumerate(parts):
        G.nodes[i]['color'] = colors[part]
    nx.nx_pydot.write_dot(G, 'graph_partitions.dot')
    return cuts, parts


def ground_connectivity_graph(G):
    """ Add parts representing the ground to connectivity graph.
    """
    ground_node = G.number_of_nodes()
    grounded_pairs = []
    for node, data in list(G.nodes(data=True)):
        if len(list(G.predecessors(node))) == 0:
            ground_node += 1
            G.add_node(ground_node,
                type='Ground',
                x=data['x'], y=0, z=data['z'], # -Y is "up"
            )
            G.add_edge(ground_node, node)
    return G


if __name__ == "__main__":
    if len(sys.argv) <= 1:
        print("Usage: python connectivity.py lego.ldr")
        sys.exit()

    filename = sys.argv[1]
    rootname, _ = os.path.splitext(filename)
    
    lego_blocks = parse_ldr(filename)
    graph = build_connectivity_graph(lego_blocks)
    ground_connectivity_graph(graph)
    #cuts, parts = partition_graph_metis(graph, n_partitions=2)
    
    # Export to GraphML.
    nx.write_graphml(graph, rootname+".graphml")
    
    # Export to Graphviz dot format, discarding the data attributes.
    dot = nx.DiGraph(graph={
        "rankdir": "BT",
    })
    dot.add_nodes_from(graph.nodes())
    dot.add_edges_from(graph.edges())
    write_dot(dot, rootname+".dot")
