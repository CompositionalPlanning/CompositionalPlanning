module PlanningLanguage
export PlanningCategory, PlanningExpr, Ob, Hom, dom, codom, compose, id,
  otimes, munit, braid, mcopy, mmerge, delete, create, blackbox

using Catlab
import Catlab.Doctrines: CategoryExpr, ObExpr, HomExpr,
  BiproductCategory, Ob, Hom, dom, codom, compose, id, otimes, munit, braid,
  mcopy, mmerge, delete, create
import Catlab.Syntax: show_latex


@signature BiproductCategory(Ob,Hom) => PlanningCategory(Ob,Hom) begin
  blackbox(x::Any, f::Hom(A,B))::Hom(A,B) <= (A::Ob, B::Ob)
end

@syntax PlanningExpr(ObExpr,HomExpr) PlanningCategory begin
  compose(f::Hom, g::Hom) = associate(Super.compose(f,g; strict=true))
  otimes(A::Ob, B::Ob) = associate_unit(Super.otimes(A,B), munit)
  otimes(f::Hom, g::Hom) = associate(Super.otimes(f,g))
end

# LaTeX pretty-print.

function show_latex(io::IO, expr::CategoryExpr{:generator}; kw...)
  # Display generator names in code style.
  print(io, latex_code_style(string(first(expr))))
end

function show_latex(io::IO, expr::HomExpr{:blackbox}; kw...)
  print(io, "\\blacksquare\\left[")
  print(io, latex_code_style(string(first(expr))))
  print(io, ",")
  show_latex(io, last(expr))
  print(io, "\\right]")
end

""" Display text in code style in LaTeX math mode.
"""
function latex_code_style(text::String)::String
  text = replace(text, "_" => "\\_")
  "\\mathtt{$text}"
end

end
