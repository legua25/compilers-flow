# Prerequisities

* [LLVM](http://llvm.org/)
* [SBT](http://www.scala-sbt.org/)

# Contents

* **build.sh** - builds compiler
* **compile.sh in out** - compiles source code in (flow) into intermediate out (llvm)
* **run.sh in** - runs interpreted intermediate in (llvm)
* **flowc** - runs flowc.jar
* **flowc.jar** - runs compiler

# Compiler Usage

```
flowc 0.1
Usage: flowc [options] source

-r | --run
run complied program
-v | --verbose

-d | --debug
print debug messages
-t | --tree
show parse tree
-o <value> | --output <value>
compiled output
-i <value> | --input <value>
input to program when run
source
source file to compile
```
