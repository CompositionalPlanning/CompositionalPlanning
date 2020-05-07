using LightGraphs,MetaGraphs
#converts a partition of ordered labels to the list of groups by name of each vertex
function tovertices(part,g)
    set_indexing_prop!(g,:name)
    getname(x)=get_prop(g,x,:name)
    subgraphs=[]
    for i in 1:maximum(part)
        push!(subgraphs,getname.(findall(x->x==i,part)))
    end
    subgraphs
end

function flattenmost(a::AbstractArray)
    while any(x->typeof(x)<:AbstractArray, Iterators.flatten(a))
        a = collect(Iterators.flatten(a))
    end
    return a
end
#k is the number of robots which can perform the process in parallel
#g is your connectivity graph, and part is your initial partition
function nestedpart!(part,g,k)
    set_indexing_prop!(g,:name)
    getvertex(x)=g[x,:name]
    previouspart=collect(vertices(g))
    while (previouspart != part) && (size(flattenmost(part))[1]<=k)
        previouspart=part
        for i in 1:size(part)[1]
            subpart=pop!(part)
            if typeof(subpart)<:Array{Int,1}
                finer=tovertices(label_propagation(induced_subgraph(g,getvertex.(subpart))[1])[1])
                pushfirst!(part,finer)
            else
                finer=nestedpart!(subpart,g,k)
                pushfirst!(part,finer)
            end
        end
    end
    part
end

g=MetaDiGraph(SimpleDiGraph(20,50))
for i in vertices(g)
    set_prop!(g,i,:name,i)
end
initpart=tovertices(label_propagation(g)[1])
println(initpart,typeof(initpart))
d=nestedpart!(initpart,g,10)
