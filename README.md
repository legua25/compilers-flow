# Flow

Programming language for compilers course, strongly inspired by Scala.

## Advanced features

* Unicode identifiers.
* Type inference, just for variables (`var`/`val`) now.
* Translation of instance call or assignment to methods `apply` or `update`.

  ```
  val array = IntArray(128)              // gets translated to
  val array = IntArray.apply(128)
  array(47) = 42                         // gets translated to
  array.update(47, 42)
  ```
* Translation of assignment operators (operators ending with `=`)
  to corresponding assignment expression.

  ```
  var a = 42
  a += 5                                 // gets translated to
  a = a + 5
  ```
* Translation of infix expressions to method calls. Any one argument method
  can be used as infix operator. Operators ending with `:` are right
  associative.

  ```
  42 + 5                                 // gets translated to
  42.+(5)

  1 +: 2 +: array                        // gets translated to
  array.+:(2).+:(1)
  ```
* Definitions without parameter clause (aka getters).

  ```
  def size: Int = ...
  ```
* Definition of custom type with external and/or static methods.
  Instances are not supported yet as well as garbage collection.
* Definition overloading. Definitions are selected based on argument types.

  ```
  type SomeType = {
    def get: Int = ...
    def get(a: Int): Int = ...
    def get(a: Int, b: Int): Int = ...
  }
  ```

# Specification

(* feature is not implemented yet)

## Variable declaration

```
var i: Int = 42             // variable i: Int
var j      = i              // variable j with inferred type Int
val k      = 42             // immutable value
def l      = 42             // computed value
```
<!-- ```
lazy val m = 42             // lazy value, computed at first access *
``` -->

## Control flow

#### branching

```
if cond then {
  ...
}
else {
  ...
}
```

#### while loop

```
while cond do {
  ...
}
```

#### for iteration *

```
for element <= iterable do {
  ...
}
```

##### nested iteration *

```
for
  i <= 1 to 10
  j <= i to 10
do {
  printLine(i + "," + j)
}
```

<!-- #### match expression *

```
i match {
  case 42 => "the answer"
  case 47 => "random number"
  case _  => "default"
}
```

#### for mapping *

```
val result = for (element <= iterable) = {
  ...
}
``` -->

### Function declaration
<!-- ## Function / method declaration -->

```
def sayHi(name: String): String =
  "hello " + name
```

##### with type inference *

```
def square(n: Int) =
  n * n
```

<!-- #### function literals *

```
val double = (i: Int) => 2 * i
``` -->

## Type system:

### Basic types:

<!-- add algebraic structures into type system like Numeric (Group / Monoid ... ?) -->
`Bool`, `Char`, `String`, `Int`, `Float`, `Unit`, `IntArray`

<!-- ### Advanced types:

`Long` (infinite precision),
`Rational[A]` -->

### Collection types: *

<!-- tuples `Tuple2[A, B] ...` -->
tuples `val pair = (42, 47)`,
arrays `Array(1, 2, 3)`
<!-- type constraints -->
<!-- `Array[A]`, `List[A]`, `Range[A]`, `Iterable[A]`
`Set[A]`, `Map[A, B]` -->

<!--### Internal types *
`Any`, `AnyVal`, `Nothing`, `Unit` -->

<!-- #### type aliases *

```
type IntAlias = Int
```-->

#### type definition

```
type NewType = {
  ...
}
```

<!--#### extending types *

```
type ThisType(name: String) = SuperType(name) + AnotherType + {
  ...
}
```

#### generics *

```
type Option[A](t: A) = {
  ...
}
``` -->
