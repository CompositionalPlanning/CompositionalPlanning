using LightGraphs, MetaGraphs, Catlab, Catlab.Doctrines, CompositionalPlanning, TikzGraphs, TikzPictures, Random

g=wheel_digraph(10)
mg=MetaDiGraph(g)
for x in edges(mg)
    set_prop!(mg,x,:name,randstring(5))
end
for x in vertices(mg)
    set_prop!(mg,x,:name,randstring(2))
end

function joinedge!(graph,v1,v2)
    add_vertex!(graph)
    lastvertex=nv(graph)
    #add outneighbors of v1 to new vertex
    set_prop!(graph,lastvertex,:name,get_prop(graph,v1,:name)*"+"*get_prop(graph,v2,:name))
    for i in outneighbors(graph,v1)
        if i!=v2
            add_edge!(graph,lastvertex,i)
            set_prop!(graph,lastvertex,i,:name,get_prop(graph,v1,i,:name))
        end
    end
    #add outneighbors of v2 to new vertex
    for i in outneighbors(graph,v2)
        if i!=v1
            add_edge!(graph,lastvertex,i)
            set_prop!(graph,lastvertex,i,:name,get_prop(graph,v2,i,:name))
        end
    end
    #add inneighbors of v1 to new vertex
    for i in inneighbors(graph,v1)
        if i!=v2
            add_edge!(graph,i,lastvertex)
            set_prop!(graph,i,lastvertex,:name,get_prop(graph,i,v1,:name))
        end
    end
    #add inneighbors of v2 to new vertex
    for i in inneighbors(graph,v2)
        if i!=v1
            add_edge!(graph,i,lastvertex)
            set_prop!(graph,i,lastvertex,:name,get_prop(graph,i,v2,:name))
        end
    end
    #remove joined vertices
    rem_vertex!(graph,v1)
    rem_vertex!(graph,v2)
end

function serialschedule(graph::MetaDiGraph,startvertex)
    set_indexing_prop!(graph,:name)
    currentvertex=get_prop(graph,startvertex,:name)
    objects=Dict(get_prop(graph,a,:name) => Ob(PlanningExpr,get_prop(graph,a,:name)) for a in vertices(graph))
    expression=id(objects[currentvertex])
    while nv(graph)>2
        println("currentvertex is")
        println(currentvertex)
        #find next connection in graph
        nextvertex=get_prop(graph,rand(outneighbors(graph,graph[currentvertex,:name])),:name)
        nextedge=Edge(graph[currentvertex,:name],graph[nextvertex,:name])
        println("nextvertex is")
        println(nextvertex)
        nextedgename=get_prop(graph,nextedge,:name)
        #edit graph
        currentvertexloc=graph[currentvertex,:name]
        joinedge!(graph,graph[currentvertex,:name],graph[nextvertex,:name])
        #the location of the joined vertex is the last vertex you deleted in the joinedge function
        joinedvertex=currentvertex*"+"*nextvertex
        #add joined vertex to your object dictionary
        objects[joinedvertex]=Ob(PlanningExpr,joinedvertex)
        println("joined vertex is")
        println(joinedvertex)
        nextstep=Hom(nextedgename,otimes(objects[currentvertex],objects[nextvertex]),objects[joinedvertex])
        expression=compose(otimes(expression,id(objects[nextvertex])),nextstep)
        currentvertex=joinedvertex
        println("current expression is")
        println(expression)
        println("graph size is")
        println(nv(graph))
    end
    return expression
end
#initialize a wheel graph with random names
g=wheel_digraph(10)
mg=MetaDiGraph(g)
for x in edges(mg)
    set_prop!(mg,x,:name,randstring(5))
end
for x in vertices(mg)
    set_prop!(mg,x,:name,randstring(2))
end
#randomly pick a starting vertex and run the scheduler
a=rand(vertices(mg))
#joinedge!(mg,3,4)
serialschedule(mg,a)
