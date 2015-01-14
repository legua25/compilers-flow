# Flow

Programming language for compilers course, strongly inspired by Scala.

The full [specification](SPEC.md).

## Advanced features

* Unicode identifiers.

* Type inference, just for variables (`var`/`val`) now.

* Multiple variables defined at once.

  ```
  val n, m = readInt()                   // gets translated to
  val n = readInt()
  val m = readInt()
  ```

* Translation of an instance call or an assignment to methods `apply`
  or `update`.

  ```
  val array = IntArray(128)              // gets translated to
  val array = IntArray.apply(128)
  array(47) = 42                         // gets translated to
  array.update(47, 42)
  ```

* Translation of assignment operators (operators ending with `=`)
  to the corresponding assignment expression.

  ```
  var a = 42
  a += 5                                 // gets translated to
  a = a + 5
  ```

* Translation of infix expressions to method calls. Any one argument method
  can be used as an infix operator. Operators ending with `:` are right
  associative.

  ```
  42 + 5                                 // gets translated to
  42.+(5)

  1 +: 2 +: array                        // gets translated to
  array.+:(2).+:(1)
  ```

* For iteration allowing nesting and guarding

  ```
  for
    i <- range0 if condition
    j <- range1
  do {
    printLine(i)
    printLine(j)
  }                                      // gets translated to

  var i = range0.start
  while range0 contains i do {
    if condition then {
      var j = range1.start
      while range1 contains j do {
        printLine(i)
        printLine(j)
      }
      j += range1.step
    }
    i += range0.step
  }
  ```

* Definitions without parameter clause (aka getters).

  ```
  def size: Int = ...
  ```

* Definition of custom types with external and/or static methods.
  Instances are not supported yet as well as garbage collection.

* Definition overloading. Definitions are selected based on argument types.

  ```
  type SomeType = {
    def get: Int = ...
    def get(a: Int): Int = ...
    def get(a: Int, b: Int): Int = ...
  }
  ```
## Known limitations and bugs

* No prefix operators yet. There are a few global functions to mimic them
  defined in [predef.flow](compiler/built-in/predef.flow) e.g.
  `def -(value: Int): Int = 0 - value`, thus instead of `-5` write `-(5)`.

* No instances for custom types. Currently only type instances are created from
  built-in types.

* Since `if` is an expression and there is no type hierarchy both branches must
  evaluate to the same type. Missing `else` branch evaluates to `Unit`,
  therefore an expression used as a statement needs to be followed by `Unit`
  expression like `{}`. This also discourages from using expressions in place
  of statements.

  ```
  if true then 42 else 47.0              // incorrect
  if true then 42 else 47                // correct
  if true then 42                        // incorrect
  if true then { 42; {} }                // correct
  ```

* There is a bug in the grammar / parser causing `while` / `for` with just
  an assignment as body to parse incorrectly, use braces to prevent it.

  instead of:
  ```
  for i <- 0 until array.size do
    array(i) = i
  ```
  write:
  ```
  for i <- 0 until array.size do {
    array(i) = i
  }
  ```
