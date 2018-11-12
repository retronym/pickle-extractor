## Pickle Extractor

### Demo

Fetch Akka:

```
$ coursier fetch --intransitive  com.typesafe.akka:akka-actor_2.12:2.5.18
/Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar
```

Compile sample file against the real JAR:


```scala
package sample.hello

import akka.actor.Actor
import akka.actor.Props

class HelloWorld extends Actor {

  override def preStart(): Unit = {
    // create the greeter actor
    val greeter = context.actorOf(Props[Greeter], "greeter")
    // tell it to perform the greeting
    greeter ! ""
  }

  def receive = {
    // when the greeter is done, stop this actor and with it the application
    case Greeter.Done => context.stop(self)
  }
}

object Greeter {
  case object Greet
  case object Done
}

class Greeter extends Actor {
  def receive = {
    case Greeter.Greet =>
      println("Hello World!")
      sender() ! Greeter.Done
  }
}
```

```
$ scalac -cp /Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar /tmp/AkkaTest.scala
```

Create the Pickle JAR:

```
$ sbt 'runMain io.github.retronym.pickleextractor.PickleExtractor /Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar /tmp/akka-pickle.jar'
[info] Loading settings for project global-plugins from idea.sbt ...
[info] Loading global plugins from /Users/jz/.sbt/1.0/plugins
[info] Loading project definition from /Users/jz/code/pickle-extractor/project
[info] Loading settings for project pickle-extractor from build.sbt ...
[info] Set current project to pickle-extractor (in build file:/Users/jz/code/pickle-extractor/)
[info] Compiling 1 Scala source to /Users/jz/code/pickle-extractor/target/scala-2.12/classes ...
[info] Done compiling.
[info] Packaging /Users/jz/code/pickle-extractor/target/scala-2.12/pickle-extractor_2.12-0.1-SNAPSHOT.jar ...
[info] Done packaging.
[info] Running io.github.retronym.pickleextractor.PickleExtractor /Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar /tmp/akka-pickle.jar
[success] Total time: 3 s, completed 12/11/2018 4:47:22 PM

$ ls -lah /Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar /tmp/akka-pickle.jar
-rw-r--r--  1 jz  staff   3.3M Nov  8 02:48 /Users/jz/.coursier/cache/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.12/2.5.18/akka-actor_2.12-2.5.18.jar
-rw-------  1 jz  wheel   1.3M Nov 12 16:47 /tmp/akka-pickle.jar
```

Compile with only the Pickle JAR:

```
$ scalac -cp /tmp/akka-pickle.jar /tmp/AkkaTest.scala
```