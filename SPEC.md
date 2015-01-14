# Specification

## Type system

**Flow** is strongly typed language supporting a few basic types. There isn't
polymorphism support. Custom types are also not supported yet.

* [`Bool`](built-in/Bool.flow)- a standard boolean logic type.
* [`Char`](built-in/Char.flow)- a standard 8-bit character type.
* [`String`](built-in/String.flow)- an immutable string type.
  (no Unicode support)
* [`Int`](built-in/Int.flow) - a standard 64-bit integer type.
* [`Float`](built-in/Float.flow) - a standard floating point type with double precision.
* [`Unit`](built-in/Unit.flow) - a unit type representing results of procedures
  also known as *void*.
* [`IntArray`](built-in/IntArray.flow) - array if `Int`s.
* [`Range`](built-in/Range.flow) - a range type representing inclusive and
  exclusive integer ranges with a step parameter.

## Predefined functions

[predef.flow](built-in/predef.flow)

## Language structure ([antlr4 grammar](compiler/src/main/antlr4/Flow.g4))

### Comments

**Flow** uses Java/Scala/... style comments `//` for line comments and `/*`,
`*/` as multiline comment delimiters.

### Program

A valid **Flow** **program** consists of **statement**s separated by newlines
or semicolon.

### Statement

A **statement** is any **definition**, **type definition** or
a **complex expression**. A **Complex expression** is a **variable definition**
or an **expression**.

### Definition

**Definition** is common term for **variable definition**s
and **function definition**s.

#### Variable definition

* (`val` | `var`) **identifier** (`,` **identifier** )\* **type annotation**?
  `=` **expression**

  ```
  var i: Int = 42                          // variable i with annotated type Int
  var j      = 47                          // variable j with inferred  type Int
  j -= 5
  val k      = j                           // immutable variable k
  val n, m   = readInt()                   // immutable variables read from input one after anothe
r
  ```

  A **variable definition** consists of either the keyword `val`
  for an immutable or `var` for a mutable variable followed by **identifier**s
  separated by `,`, an optional **type annotation** and a mandatory `=`
  character followed by a definition **expression**. In case
  a **type annotation** is provided, the definition **expression** must evaluate
  to this type, otherwise its type will be used automatically. Immutable
  variables can't be reassigned. In case there are more **identifier**s
  specified, the definition **expression** is evaluated for all of them anew.

#### Function definition

* `def` **identifier** **parameter clause**? **type annotation** `=`
  **expression**

  ```
  var answer                 = 42
  def doubledAnswer: Int     = 2 * answer  // computed value

  def successor(i: Int): Int = i + 1

  external def printLine(a: String): Unit  // external function
  ```

  **Function definitions** are very similar to variable definitions. They
  consist of the keyword `def` followed by an **identifier**, an optional
  **parameter clause**, a **type annotation**, an `=` character and a definition
  **expression**. External function definitions are preceded by the keyword
  `external` and lack the definition part. A **parameter clause** follows
  structure `(` **parameters** `)`, where **parameters** are comma separated
  **identifier**s followed by a mandatory **type annotation**. Functions
  without a parameter clause mimic immutable variables that compute its value
  at each access. This behavior is commonly used for getters in context of
  classes.

### Type definition

* `type` **identifier** `=` `{` **member definitions** `}`

  ```
  type Int = {

    static def maxValue: Int = 9223372036854775807

    external static def random(from: Int, to: Int): Int

    external def ==(that: Int): Bool

    def min(that: Int): Int =
    if this < that then this else that

  }
  ```

  A **type definition** has the structure shown above,
  where **member definitions** are **member definition**s separated
  by newlines or semicolons. A **member definition** is any **definition**
  optionally preceded by the keyword `static` and in case of function
  definitions possibly also `external`.

### Expression

The term **expression** covers all kinds of computations that yield a value with
an associated type (**complex expression** to be precise). As in Scala this also
includes structures like `if`, `while`, `for` and blocks.

* `if` *condition=* **expression** `then` *true=* **expression**
  (`else` *false=* **expression**)?

  ```
  def isEven(n: Int): Bool =
    if n % 2 == 0 then true else false

  if isEven(n) then
    printLine("even")
  ```

  An *if expression* evaluates to one of its subexpressions based on the value
  of the *condition*. Both branches must evaluate to the same type. In case
  of missing the else branch `Unit` is assumed.

* `while` *condition=* **expression** `do` *body=* **expression**

  ```
  while i < 10 do
    printLine(i)
  ```

  A *while expression* will evaluate its *body* while the *condition* holds.
  The resulting expression will be `{}`.

* `for` **generators** `do` **expression**

  **Generators** are newline or semicolon separated **generator**s with
  following structure:

  * **identifier** `<-` *gen=* **expression** (`if` *guard=* **expression**)?

  ```
  for
    i <- 1 to 100 if i % 2 == 0
    j <- i to 100
  do {
    print(i); print(" "); printLine(j)
  }
  ```

  A *for expression* iterates over numeric ranges provided in **generator**
  clauses, effectively performing nested iterations. *Guard*s determine if
  the rest of *for expression* actually gets executed. The example above is
  equivalent of:

  ```
  val r = 1 to 100
  var i = r0.start
  while r contains i do {
    if i % 2 == 0 then {
      val r = i to 100
      var j = r1.start
      while r contains j do {
        print(i); print(" "); printLine(j)
        j += r.step
      }
    }
    i += r.step
  }
  ```

* `{` **complex expressions** `}`

  A *block expression* encapsulates newline or semicolon separated
  **complex expression**s to be used as a part of another expression, especially
  **definition**s and control structures. Its value and type is determined
  by the last **expression** it contains. Blocks create a new scope for variable
  definitions.

* `(` **expression** `)`

* **expression** `(` **arguments**? `)`

  ```
  def double(n: Int): Int =
    2 * n

  double(21)                             // function call

  val array = IntArray(32)

  array(0)                               // call of method apply with argument 0
  ```

  <!-- FIXME: recursive definition -->
  Actual semantics of an *application expression* is based on **expression**
  being applied to. In case of an **identifier** global functions are searched
  first and if found **arguments** are applied to it, otherwise
  the **expression** is evaluated and its type is searched for a method named
  *apply*. In case of a *selection expression* it is treated as a method call.

* **expression** `.` **identifier**

  ```
  Int.maxValue
  (42 + 5).toFloat
  ```

  A *selection expression* is used to access **member definition**s of
  an instance.

* **expression** **identifier** **expression**

  [*Infix expressions*](#Infix expressions)

* **expression** `=` **expression**

  ```
  var i = 0
  i = 42

  val array = IntArray(32)
  array(0) = i                           // call of method update with arguments 0 and i
  ```

  Actual semantics of an *assignment expression* is very similar
  to the *application expression*. In case the **expression** being assigned to
  is an application, the target of the application is searched for a method
  named *update* taking all arguments of the application plus the value obtained
  from the **expression** being assigned. Otherwise it must consist
  of an **identifier** that will be assigned to.

* **identifier**

  Identifiers consist of *letter identifier*s and *operator identifier*s
  that are not keywords.

  A *letter identifier* is any sequence of characters starting with a *letter*
  or an *_* followed by any number of *letter*s, *_*s, or *digit*s,
  where a *letter* is any letter of the english alphabet or any unicode
  character with code 0x00C0 or greater.

  An *operator identifier* is formed by any number of characters from
  `!#$%&*+\-/:<=>?@\\^_|~` or unicode characters in range [0x00A1,&nbsp;0x00AC]
  or [0x00AE,&nbsp;0x00BF].

* **literal**

  Valid **literal**s are as follows:

  * `Bool`: `true` and `false`
  * `Char`: an escape sequence or any character other than `'` and
    the newline, enclosed in `'`s.
  * `String`: any number of escape sequences or characters other than the `"`
    and the newline, enclosed in `"`.
  * `Int` (binary): `0b` followed by binary digits e.g. `0b00101010`.
  * `Int` (octal): `0o` followed by octal digits e.g. `0o52`.
  * `Int` (decimal): `0` or a positive integer e.g. `42`.
  * `Int` (hexadecimal): `0x` followed by hexadecimal digits
    (upper or lower case) e.g. `0x2A`.
  * `Float`: a decimal integer optionally followed by `.` and decimal digits
    optionally followed by `e` and decimal digits

## List of keywords and reserved strings

* `type`, `val`, `var`, `def`, `external`, `static`, `if`, `then`, `else`,
  `while`, `do`, `for`
* `:`, `<-`, `=`, `,`, `;`, `(` ,`)`, `{`, `}`, `'`, `"`

## Compilation process

* The first step in the compilation process is parsing source codes, that is
  library files found in [built-in](compiler/built-in) and the input source
  file.

* After, all type and global function definitions are declared and then defined
  along with type methods in the global scope. This way, those definitions can
  reference each other, but they have no knowledge of other statements.

* Next, all the other statements found in the input source file are processed
  in a new scope.

## Definition resolution rules

* When a definition is declared it is first matched with the set of already
  declared definitions. All global definitions share one scope and are compared
  by their signatures. A definition signature consists of its name and parameter
  types if any, making functions with the same name, one without parameters,
  the other with empty parameter list, different. Variable definitions,
  which can appear in deeper scopes, follow the same rules.

* When a definition is looked up, it is searched by its signature, starting
  in the nearest scope following up. Program statements other than function
  definitions are defined one scope deeper than global function definitions,
  which allows to shadow them. Methods, definitions in types, are defined
  in the scope of their type. A method call is thus looked up in the type
  of the expression it is being called on.

* If a method ending in `=` e.g. `+=` in `a += 3` is not found, it is tried
  to be transformed, if possible, into an assignment of an expression formed
  by stripping the `=` from the original expression to the left hand side
  e.g. `a = a + 3`.

  ```
  def two: Int =                         // valid
    one + one

  def one: Int =
    1

  val one: String = "1"                  // valid, shadows global definition

  // def one: Float =                    // invalid, same signature
  //   1.0
  ```

  ```
  def plusOne: String =
    "+ 1"

  def plusOne(i: Int): Int =             // valid, signatures are different
    i + 1

  def plusOne(f: Float): Float =         // valid, signatures are different
    f + 1.0

  // def plusOne(i: Int): Float =        // invalid, same signature
  //  i.toFloat + 1.0
  ```

  ```
  type Int = {
    def +(that: Int): Int
  }

  var i = 0
  i += 1
  ```

## Infix expressions

Unlike many languages, any method can be used as an infix operator. Assignment
operators, that is any operators ending with `=` and not beginning by `=` except
`!=`, `<=`, `>=`, have the lowest priority. Others are determined by their last
character in the following order (from the lowest priority):

* any letter
* `|`
* `^`
* `&`
* `<`
* `=`
* `:`
* `+`
* `*`
* others

Operators ending with `:` are right associative, thus are transformed
into a method call on its right operand.

```
val a = IntArray(0)
val b = 1 +: a :+ 2

// equivalently

val c = a.+:(1).:+(2)

printLine(b)
// IntArray(1, 0, 2)
```
