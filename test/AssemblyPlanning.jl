module TestAssemblyPlanning
using Test

using LightGraphs, MetaGraphs
using Catlab.WiringDiagrams
using CompositionalPlanning

# Sequential planning
#####################

# Sequential plan on a path of length 3.
g = path_digraph(3)
d = WiringDiagram([[1],[2],[3]], [[1,2,3]])
v12 = add_box!(d, Box([[1],[2]], [[1,2]]))
v123 = add_box!(d, Box([[1,2],[3]], [[1,2,3]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v12,1)),
  Wire((input_id(d),2) => (v12,2)),
  Wire((input_id(d),3) => (v123,2)),
  Wire((v12,1) => (v123,1)),
  Wire((v123,1) => (output_id(d),1)),
])
@test sequential_plan(g) == d

# Sequential plan on a path of length 3, with reversed order.
g = path_digraph(3)
d = WiringDiagram([[1],[2],[3]], [[1,2,3]])
v23 = add_box!(d, Box([[2],[3]], [[2,3]]))
v123 = add_box!(d, Box([[1],[2,3]], [[1,2,3]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v123,1)),
  Wire((input_id(d),2) => (v23,1)),
  Wire((input_id(d),3) => (v23,2)),
  Wire((v12,1) => (v123,2)),
  Wire((v123,1) => (output_id(d),1)),
])
@test sequential_plan(g, vertex_order=[3,2,1]) == d

# Sequential plan on a fork.
g = DiGraph(3)
add_edge!(g,1,2); add_edge!(g,1,3)
d = WiringDiagram([[1],[2],[3]], [[1,2,3]])
v13 = add_box!(d, Box([[1],[3]], [[1,3]]))
v123 = add_box!(d, Box([[1,3],[2]], [[1,2,3]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v13,1)),
  Wire((input_id(d),3) => (v13,2)),
  Wire((input_id(d),2) => (v123,2)),
  Wire((v13,1) => (v123,1)),
  Wire((v123,1) => (output_id(d),1)),
])
@test sequential_plan(g) == d

# Sequential plan on a meta graph (a path of length 3).
g = MetaDiGraph(path_digraph(3))
for (v, name) in enumerate([:a,:b,:c])
  set_prop!(g, v, :name, name)
end
set_indexing_prop!(g, :name)
d = WiringDiagram([[:a],[:b],[:c]], [[:a,:b,:c]])
v_ab = add_box!(d, Box([[:a],[:b]], [[:a,:b]]))
v_abc = add_box!(d, Box([[:a,:b],[:c]], [[:a,:b,:c]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v_ab,1)),
  Wire((input_id(d),2) => (v_ab,2)),
  Wire((input_id(d),3) => (v_abc,2)),
  Wire((v_ab,1) => (v_abc,1)),
  Wire((v_abc,1) => (output_id(d),1)),
])
@test sequential_plan((g,:name)) == d

# Parallel planning
###################

# Parallel planning in the simplest possible case.
g = path_digraph(2)
d = WiringDiagram([[1],[2]], [[1,2]])
v12 = add_box!(d, Box([[1],[2]], [[1,2]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v12,1)),
  Wire((input_id(d),2) => (v12,2)),
  Wire((v12,1) => (output_id(d),1)),
])
@test parallel_plan(g, [[1],[2]]) == d

# Parallel planning with one level of decomposition.
g = path_digraph(4)
d = WiringDiagram([[1],[2],[3],[4]], [[1,2,3,4]])
v12 = add_box!(d, Box([[1],[2]], [[1,2]]))
v34 = add_box!(d, Box([[3],[4]], [[3,4]]))
v1234 = add_box!(d, Box([[1,2],[3,4]], [[1,2,3,4]]))
add_wires!(d, [
  Wire((input_id(d),1) => (v12,1)),
  Wire((input_id(d),2) => (v12,2)),
  Wire((input_id(d),3) => (v34,1)),
  Wire((input_id(d),4) => (v34,2)),
  Wire((v12,1) => (v1234,1)),
  Wire((v34,1) => (v1234,2)),
  Wire((v1234,1) => (output_id(d),1)),
])
@test parallel_plan(g, [[1,2],[3,4]]) == d

# Parallel planning with two levels of decomposition.
g = path_digraph(8)
plan = parallel_plan(g, [[[1,2],[3,4]],[[5,6],[7,8]]])
@test input_ports(plan) == [[1],[2],[3],[4],[5],[6],[7],[8]]
@test output_ports(plan) == [[1,2,3,4,5,6,7,8]]
@test nboxes(plan) == 3
subplan1, subplan2 = boxes(plan)[1:2]
@test subplan1 == parallel_plan(path_digraph(4), [[1,2],[3,4]])
@test subplan2 == parallel_plan(Subgraph(g, path_digraph(4), [5,6,7,8]), [[5,6],[7,8]])

# Parallel planning with one level of decomposition, followed by sequential
# planning.
g = path_digraph(8)
plan = parallel_plan(g, [[1,2,3,4],[5,6,7,8]])
@test input_ports(plan) == [[1],[2],[3],[4],[5],[6],[7],[8]]
@test output_ports(plan) == [[1,2,3,4,5,6,7,8]]
@test nboxes(plan) == 3
subplan1, subplan2 = boxes(plan)[1:2]
@test subplan1 == sequential_plan(path_digraph(4))
@test subplan2 == sequential_plan(Subgraph(g, path_digraph(4), [5,6,7,8]))

end
