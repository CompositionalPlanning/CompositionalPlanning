module TestScheduling
using Test

using Catlab, Catlab.Doctrines
using CompositionalPlanning.Scheduling

X = Ob(FreeSymmetricMonoidalCategory, :X)
f, g, h, k = Hom(:f, X, X), Hom(:g, X, X), Hom(:h, X, X), Hom(:k, X, X)
m, n = Hom(:h, X, otimes(X,X)), Hom(:k, otimes(X,X), X)

@test linearize_hom_expr(compose(f,g)) == [f,g]
@test linearize_hom_expr(otimes(f,g)) == [f,g]
@test linearize_hom_expr(otimes(compose(f,g),compose(h,k))) == [f,g,h,k]
@test linearize_hom_expr(otimes(compose(f,g,h),k)) == [f,g,h,k]

@test interleave_hom_expr(compose(f,g)) == [f,g]
@test interleave_hom_expr(otimes(f,g)) == [f,g]
@test interleave_hom_expr(compose(m, otimes(f,g), n)) == [m,f,g,n]
@test interleave_hom_expr(compose(m, otimes(f,id(X)), n)) == [m,f,n]
@test interleave_hom_expr(otimes(compose(f,g),compose(h,k))) == [f,h,g,k]
@test interleave_hom_expr(otimes(compose(f,g,h),k)) == [f,k,g,h]

end
