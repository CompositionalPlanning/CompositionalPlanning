""" Graphs whose vertices have unique names.

A generic interface that supports simple graphs and meta graphs. It exists
because in LightGraphs all graphs have vertices named as consecutive integers
starting at 1. That is not always convenient.
"""
module NamedGraphs
export Subgraph, unnamed_graph, name_of_vertex, has_vertex_named, vertex_named

using LightGraphs, MetaGraphs

# Simple graphs as named graphs.

unnamed_graph(named::AbstractGraph) = named
name_of_vertex(named::AbstractGraph, v::Int) = v
has_vertex_named(named::AbstractGraph, v::Int) = 1 <= v <= nv(named)
vertex_named(named::AbstractGraph, v::Int) = v

# Meta graphs as named graphs.

function unnamed_graph(named::Tuple{<:AbstractMetaGraph,Symbol})
  first(named)
end
function name_of_vertex(named::Tuple{<:AbstractMetaGraph,Symbol}, v::Int)
  g, prop = named
  get_prop(g, v, prop)
end
function has_vertex_named(named::Tuple{<:AbstractMetaGraph,Symbol}, name)
  g, prop = named
  haskey(g[prop], name)
end
function vertex_named(named::Tuple{<:AbstractMetaGraph,Symbol}, name)
  # Warning: Cannot use `g[name,prop]` when names are integers.
  g, prop = named
  g[prop][name]
end

# Subgraphs as named graphs.

""" A subgraph of another graph.

Suitable for use with `LightGraphs.induced_subgraph`.
"""
struct Subgraph
  named::Any
  subgraph::AbstractGraph
  vmap::Vector{Int}
  reverse_vmap::Dict{Int,Int}
  
  function Subgraph(named::Any, subgraph::AbstractGraph, vmap::Vector{Int})
    reverse_vmap = Dict{Int,Int}(v => i for (i, v) in enumerate(vmap))
    new(named, subgraph, vmap, reverse_vmap)
  end
end

function unnamed_graph(subgraph::Subgraph)
  subgraph.subgraph
end
function name_of_vertex(subgraph::Subgraph, v::Int)
  name_of_vertex(subgraph.named, subgraph.vmap[v])
end
function has_vertex_named(subgraph::Subgraph, name)
  (has_vertex_named(subgraph.named, name) &&
   haskey(subgraph.reverse_vmap, vertex_named(subgraph.named, name)))
end
function vertex_named(subgraph::Subgraph, name)
  subgraph.reverse_vmap[vertex_named(subgraph.named, name)]
end

end
