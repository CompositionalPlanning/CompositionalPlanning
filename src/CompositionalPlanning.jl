module CompositionalPlanning

using Reexport
using Requires

include("PlanningLanguage.jl")
include("NamedGraphs.jl")
include("AssemblyPlanning.jl")
include("Scheduling.jl")

function __init__()
  @require PyCall="438e738f-606a-5dbb-bf0a-cddfbfd45ab0" include("PyGraphs.jl")
end

@reexport using .PlanningLanguage
@reexport using .NamedGraphs
@reexport using .AssemblyPlanning
@reexport using .Scheduling

end
