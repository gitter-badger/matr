package matr.dflt: 

  import matr.MatrixFactory
  import matr.Matrix
  import matr.SpecialValues
  import scala.collection.mutable
  import scala.reflect.ClassTag

  object DefaultMatrixFactory: 
    
    given defaultMatrixFactory[R <: Int, C <: Int, T]
                              (using Matrix.DimsOK[R, C] =:= true)
                              (using vr: ValueOf[R], vc: ValueOf[C])
                              (using sp: SpecialValues[T])
                              : MatrixFactory[R, C, T] with

      def builder: MatrixFactory.Builder[R, C, T] = 
        new DefaultMatrixFactory.Builder[R, C, T]


      def rowMajor(elements: T*): Matrix[R, C, T] =
        require(elements.length == vr.value * vc.value, 
          s"Size of given element collection must be ${vr.value * vc.value}, but was ${elements.size}")
        val buildr: MatrixFactory.Builder[R, C, T] = builder
        var idx: Int = 0
        while (idx < elements.length) do 
          val (rowIdx, colIdx) = RowMajorIndex.fromIdx(idx, vc.value)
          buildr(rowIdx, colIdx) = elements(idx)
          idx = idx + 1
        buildr.result


      def tabulate(fillElem: (Int, Int) => T): Matrix[R, C, T] = 
        val buildr: MatrixFactory.Builder[R, C, T] = builder
        buildr.iterate{ (rowIdx, colIdx) => 
          buildr(rowIdx, colIdx) = fillElem(rowIdx, colIdx) 
        }
        buildr.result


      def zeros: Matrix[R, C, T] = 
        tabulate((_, _) => sp.zero)

      
      def ones: Matrix[R, C, T] =
        tabulate((_, _) => sp.one)


      def identity(using MatrixFactory.IsSquare[R, C] =:= true): Matrix[R, C, T] =
        tabulate{ (rowIdx, colIdx) => 
          if rowIdx == colIdx then sp.one else sp.zero
        }

  
    class Builder[R <: Int, C <: Int, T]
                 (using vr: ValueOf[R], vc: ValueOf[C])
                 (using sp: SpecialValues[T])
                 (using Matrix.DimsOK[R, C] =:= true)
                 extends MatrixFactory.Builder[R, C, T], Matrix[R, C, T]:

      private var elemMap: mutable.Map[(Int, Int), T] = mutable.Map.empty

      private var elemArr: Array[T] = null

      private val treshold: Int = (vr.value * vc.value * MIN_DENSE_FILL).floor.toInt 

      override def apply(rowIdx: Int, colIdx: Int): T = 
        if elemMap ne null then
          elemMap.getOrElse((rowIdx, colIdx), sp.zero)
        else
          elemArr(RowMajorIndex.toIdx(rowIdx, colIdx, valueOf[C]))

      override def update(rowIdx: Int, colIdx: Int, v: T): this.type = 
        if (elemMap ne null) && (elemMap.size < treshold) then
          if !sp.isZero(v) then
            elemMap((rowIdx, colIdx)) = v
        else 
          if elemArr eq null then
            val tag: ClassTag[T] = ClassTags.fromValue(v)
            given ClassTag[T] = tag
            elemArr = Array.fill(vr.value * vc.value)(sp.zero)
            elemMap.foreachEntry((mk, mv) => elemArr(RowMajorIndex.toIdx(mk._1, mk._2, vc.value)) = mv)
            elemMap = null
          elemArr(RowMajorIndex.toIdx(rowIdx, colIdx, valueOf[C])) = v
        this

      override def result: Matrix[R, C, T] = 
        if elemArr eq null then
          new DefaultSparseMatrix[R, C, T](elemMap.toMap)
        else
          new DefaultDenseMatrix[R, C, T](elemArr)


    private val MIN_DENSE_FILL: Float = 0.5  
