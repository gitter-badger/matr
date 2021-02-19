package matr:

  import org.scalacheck.Arbitrary


  object ArbitraryMatrix:

    def apply[R <: Int, C <: Int, T]
             (using Matrix.DimsOK[R, C] =:= true)
             (using vr: ValueOf[R], vc: ValueOf[C])
             (using mf: MatrixFactory[R, C, T])
             (using arb: Arbitrary[T])
             : Arbitrary[Matrix[R, C, T]] = 
      Arbitrary(GenMatrix[R, C, T])
