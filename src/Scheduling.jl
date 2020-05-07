""" Making schedules out of plans.

For us, a schedule is a (totally ordered) sequence of morphism generators.
"""
module Scheduling
export jsonable_schedule, interleave_hom_expr, linearize_hom_expr

using Catlab, Catlab.Doctrines


""" Convert a schedule to a JSON-able sequence.
"""
function jsonable_schedule(exprs)
  collect_values = ob_expr -> tuple(map(first, collect(ob_expr))...)
  map(exprs) do expr::HomExpr{:generator}
    (first(expr), collect_values(dom(expr)), collect_values(codom(expr)))
  end
end


""" Linearly order the generators in a morphism expression by interleaving.

We interpret this operation as generating a schedule from a plan. The algorithm
here appears in a more complicated form in the paper (Cordasco & Rosenberg,
2014, On scheduling series-parallel DAGs to maximize AREA). The idea is to
maximize opportunities for parallelism when executing the schedule.
"""
function interleave_hom_expr(expr::HomExpr{:compose})
  mapreduce(interleave_hom_expr, vcat, args(expr))
end
function interleave_hom_expr(expr::HomExpr{:otimes})
  interleave(map(interleave_hom_expr, args(expr)))
end
interleave_hom_expr(expr::HomExpr) = linearize_hom_expr(expr)

""" Linearly order the generators in a morphism expression.

Order by simple concatenation. Interpreted as a schedule, this order is bad,
as it minimizes opportunities for parallelism.
"""
function linearize_hom_expr(expr::HomExpr{:compose})
  mapreduce(linearize_hom_expr, vcat, args(expr))
end
function linearize_hom_expr(expr::HomExpr{:otimes})
  mapreduce(linearize_hom_expr, vcat, args(expr))
end
linearize_hom_expr(expr::HomExpr{:generator}) = [ expr ]
linearize_hom_expr(expr::HomExpr{:id}) = []
linearize_hom_expr(expr::HomExpr{:braid}) = []


""" Flatten a sequence of sequences by interleaving the elements.
"""
function interleave(seqs::Vector{V}) where V <: Vector
  sizes = map(length, seqs)
  flat = V(undef, sum(sizes))
  i = 0
  for j in 1:maximum(sizes)
    for k in 1:length(seqs)
      if j <= sizes[k]
        flat[i += 1] = seqs[k][j]
      end
    end
  end
  flat
end

end
