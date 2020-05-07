using LightGraphs, MetaGraphs

g=PathDiGraph(5)
mg=MetaDiGraph(g)

function remvertex!(g,k)
    clear_props!(g,k)
    rem_vertex!(g.graph,k)
end
for i in 1:5
    set_prop!(mg,i,:name,string(i))
end
set_indexing_prop!(mg,:name)
a=[get_prop(mg,x,:name) for x in vertices(mg)]
println(a)
println(collect(edges(mg)))
for i in [string(k) for k in 1:1]
    rem_vertex!(mg,mg[i,:name])
end
b=[get_prop(mg,x,:name) for x in vertices(mg)]
println(b)
println(collect(edges(mg)))
println(get_prop(mg,mg["2",:name],:name))
rem_vertex!(mg,mg["2",:name])
b=[get_prop(mg,x,:name) for x in vertices(mg)]
println(b)
println(collect(edges(mg)))
