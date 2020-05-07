""" Interoperation with Python graph libraries.
"""
module PyGraphs
export to_networkx, from_networkx, cdlib_communities,
  nx_girvan_newman, nx_greedy_modularity_communities

using LightGraphs
using ..PyCall # @require-d module

const itertools = pyimport("itertools")
const nx = pyimport("networkx")


""" Convert a graph in Julia to a NetworkX graph.
"""
function to_networkx(g::AbstractGraph; node_name=identity)
  nx_g = is_directed(g) ? nx.DiGraph() : nx.Graph()
  nx_g.add_nodes_from(node_name(v) for v in 1:nv(g))
  nx_g.add_edges_from((node_name(src(e)),node_name(dst(e))) for e in edges(g))
  nx_g
end

""" Convert a NetworkX graph to a graph in Julia.

Returns a graph and a dictionary from NetworkX nodes to vertices.
"""
function from_networkx(nx_g::PyObject)
  n = nx_g.number_of_nodes()
  g = nx_g.is_directed() ? DiGraph(n) : Graph(n)
  node_map = Dict{Any,Int}(node => i for (i, node) in enumerate(nx_g.nodes()))
  for (src, dst) in nx_g.edges()
    add_edge!(g, node_map[src], node_map[dst])
  end
  (g, node_map)
end


""" Call the NetworkX implementation of the Girvan-Newman algorithm.
"""
function nx_girvan_newman(g::AbstractGraph, levels::Int)
  nx_g = to_networkx(g)
  communities_iter = nx.algorithms.community.girvan_newman(nx_g)
  [ [ Set{Int}(c) for c in communities ]
    for communities in itertools.islice(communities_iter, levels) ]
end

""" Call the NetworkX implementation of the Clauset-Newman-Moore algorithm.
"""
function nx_greedy_modularity_communities(g::AbstractGraph)
  nx_g = to_networkx(g)
  communities = nx.algorithms.community.greedy_modularity_communities(nx_g)
  [ Set{Int}(c) for c in communities ]
end


""" Call a community detection algorithm in CDlib.
"""
function cdlib_communities(g::AbstractGraph, algorithm::Symbol, args...; kw...)
  algorithms = pyimport("cdlib.algorithms")
  nx_g = to_networkx(g, node_name=string) # String IDs in case `igraph` is used.
  clustering = getproperty(algorithms, algorithm)(nx_g, args...; kw...)
  [ [parse(Int,v) for v in c] for c in clustering.communities ]
end

end
