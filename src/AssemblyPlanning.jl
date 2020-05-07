""" Planning the assembly of objects from their parts.

The configuration of the parts is described by the *connectivity graph*, a
directed acyclic graph with atomic parts as vertices and with an edge from one
part to another if the first part is physically connected to the second. We
assume that the connections have an evident directionality. The planning problem
is to find a physically feasible sequence of subassembly joins that assembles
the object according to the connectivity graph. 

Our motivating example is the assembly of LEGO designs.
"""
module AssemblyPlanning
export read_connectivity_graph, connectivity_graph_to_dot,
  sequential_plan, sequential_plan!, parallel_plan, parallel_plan!

using Base.Iterators: flatten
using Compat

using LightGraphs, MetaGraphs
using Catlab.WiringDiagrams
using Catlab.WiringDiagrams.GraphMLWiringDiagrams: read_graphml_metagraph
using ..NamedGraphs


""" Read a connectivity graph from a GraphML file.
"""
function read_connectivity_graph(filename::String)::MetaDiGraph
  read_graphml_metagraph(filename, directed=true)
end

""" Connectivity graph in Graphviz dot format.

Returns a meta graph suitable for use with `to_graphviz`.
"""
function connectivity_graph_to_dot(graph::MetaDiGraph)
  dot = MetaDiGraph(graph.graph)
  set_props!(dot, Dict(
    :graph => Dict(:rankdir => "BT"),
    :node => Dict(:shape => "circle", :margin=>"0", :width=>"0", :height=>"0"),
    :edge => Dict(:arrowsize => "0.5"),
  ))
  for v in 1:nv(graph)
    attrs = Dict(
      :label => string(v),
    )
    if lowercase(get_prop(graph, v, :type)) == "ground"
      attrs[:style] = "filled"
      attrs[:fillcolor] = "gray"
    end
    set_props!(dot, v, attrs)
  end
  dot
end


""" Build a sequential plan from a connectivity graph.

Construct a plan by sequentially joining subassemblies according to a sorting
of the vertices, by default a topological sort. When the sort is topological
and the connectivity graph is a path graph, the sequential plan looks like a
staircase.
"""
function sequential_plan(named_graph; kw...)::WiringDiagram
  diagram = empty_plan(named_graph)
  sequential_plan!(diagram, named_graph; kw...)
  set_output_ports!(diagram)
end
function sequential_plan!(diagram::WiringDiagram, named_graph; vertex_order=nothing)
  # Topologically sort the edges of the graph.
  graph = unnamed_graph(named_graph)
  if isnothing(vertex_order)
    vertex_order = topological_sort_by_dfs(graph)
  end
  edges = sort_edges(graph, vertex_order)
  
  # Add box to diagram for each edge in graph.
  output_map = Dict{Any,Port}()
  for port in terminal_ports(diagram)
    for elem in port_value(diagram, port) output_map[elem] = port end
  end
  for edge in edges
    src_port = output_map[name_of_vertex(named_graph, src(edge))]
    dst_port = output_map[name_of_vertex(named_graph, dst(edge))]
    if src_port == dst_port
      # Already joined, so skip this edge.
      continue
    end
    src_elems = port_value(diagram, src_port)
    dst_elems = port_value(diagram, dst_port)
    joined_elems = sort!([src_elems; dst_elems])
    v = add_box!(diagram, Box([src_elems, dst_elems], [joined_elems]))
    add_wire!(diagram, Wire(src_port => Port(v, InputPort, 1)))
    add_wire!(diagram, Wire(dst_port => Port(v, InputPort, 2)))
    port = Port(v, OutputPort, 1)
    for elem in joined_elems; output_map[elem] = port end
  end
  diagram
end


""" Build a parallel plan from a decomposition of a connectivity graph.

The decomposition is given by `components`, a vector of vectors of vectors of...
named vertices in the graph, nested arbitrarily deep.
"""
function parallel_plan(named_graph, components::Vector{V}) where V <: Vector
  diagram = empty_plan(named_graph)
  parallel_plan!(diagram, named_graph, components)
  set_output_ports!(diagram)
end
function parallel_plan!(diagram::WiringDiagram, named_graph,
                        components::Vector{Vector{T}}) where T
  graph = unnamed_graph(named_graph)
  vertices = [ [vertex_named(named_graph, elem) for elem in flatten(component)]
               for component in components ]
  
  # Add box to diagram for each component in top level of decomposition.
  output_map = Dict{Any,Port}()
  for port in terminal_ports(diagram)
    for elem in port_value(diagram, port) output_map[elem] = port end
  end
  for (component, vs) in zip(components, vertices)
    incoming_ports = unique!([ output_map[elem] for elem in flatten(component) ])
    inputs = [ port_value(diagram, port) for port in incoming_ports ]
    if length(inputs) <= 1
      # Already joined, so skip this component.
      continue
    end
    v = if T <: Array
      # Case 1: Components are further nested. Recurse.
      subdiagram = WiringDiagram(inputs, [])
      subgraph = Subgraph(named_graph, induced_subgraph(graph, vs)...)
      parallel_plan!(subdiagram, subgraph, component)
      add_box!(diagram, set_output_ports!(subdiagram))
    elseif length(inputs) > 2
      # Case 2: At leaf with more than two inputs left. Do sequential planning.
      subdiagram = WiringDiagram(inputs, [])
      subgraph = Subgraph(named_graph, induced_subgraph(graph, vs)...)
      sequential_plan!(subdiagram, subgraph)
      add_box!(diagram, set_output_ports!(subdiagram))
    else
      # Case 3: At leaf with exactly two inputs. Do a single join.
      outputs = [ sort!([inputs[1]; inputs[2]]) ]
      add_box!(diagram, Box(inputs, outputs))
    end
    add_wires!(diagram, [ Wire(port => Port(v, InputPort, i))
                          for (i, port) in enumerate(incoming_ports) ])
  end
  
  # Join the component boxes sequentially.
  component_map = Dict(v => i for (i, vs) in enumerate(vertices) for v in vs)
  between_edges = filter(collect(edges(graph))) do edge
    component_map[src(edge)] != component_map[dst(edge)]
  end
  subgraph = Subgraph(named_graph, induced_subgraph(graph, between_edges)...)
  sequential_plan!(diagram, subgraph)
end


""" Initialize an empty plan given a connectivity graph.
"""
function empty_plan(named_graph)::WiringDiagram
  graph = unnamed_graph(named_graph)
  elems = [ name_of_vertex(named_graph,v) for v in 1:nv(graph) ]
  WiringDiagram([ [elem] for elem in elems ], [ elems ])
end

""" Set output ports of diagram based on terminal ports inside the diagram.
"""
function set_output_ports!(diagram::WiringDiagram)
  outgoing_ports = terminal_ports(diagram)
  diagram.output_ports = [ port_value(diagram, port) for port in outgoing_ports ]
  add_wires!(diagram, [ Wire(port => Port(output_id(diagram), InputPort, i))
                        for (i, port) in enumerate(outgoing_ports) ])
  diagram
end

""" List all outport ports of boxes in diagram with no outgoing wires.
"""
function terminal_ports(diagram::WiringDiagram)::Vector{Port}
  terminal_vs = filter([input_id(diagram); box_ids(diagram)]) do v
    isempty(out_wires(diagram, v))
  end
  mapreduce(vcat, terminal_vs; init=Port[]) do v
    [ Port(v, OutputPort, i) for i in eachindex(output_ports(diagram, v)) ]
  end
end


""" Lexicographically sort the edges of a graph, given a sort of the vertices.
"""
function sort_edges(graph::AbstractGraph, vertex_order::Vector{Int})
  vertex_pos = Dict(v => i for (i, v) in enumerate(vertex_order))
  edge_pos(e::Edge) = (vertex_pos[src(e)], vertex_pos[dst(e)])
  edge_lt(e1::Edge, e2::Edge) = edge_pos(e1) < edge_pos(e2)
  sort!(collect(edges(graph)), lt=edge_lt)
end

end
