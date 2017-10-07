# Process Watch Dog

**Java library for watching (and *killing*) processes.**

Watch Dog thread runs only when there are same active processes to watch.

```
       .-------------.       .    .   *       *   
      /_/_/_/_/_/_/_/ \         *       .   )    .
     //_/_/_/_/_/_// _ \ __          .        .   
    /_/_/_/_/_/_/_/|/ \.' .`-o                    
     |             ||-'(/ ,--'                    
     |             ||  _ |                        
     |             ||'' ||                        
     |_____________|| |_|L                     hjm
```

## Prerequisites
- Java 6

## Usage

Copy the Maven dependency into your Maven project:
```
<dependency>
    <groupId>cz.net21.ttulka.exec</groupId>
    <artifactId>process-watch-dog</artifactId>
    <version>1.0.0</version>
</dependency>
```

Create a Watch Dog object:
```
ProcessWatchDog watchDog = new ProcessWatchDog();
```

Create a process and watch it by the Watch Dog:
```
ProcessBuilder pb = new ProcessBuilder("myCommand", "myArg1", "myArg2");
Process p = pb.start();

watchDog.watch(p, 1000); // kill the process after 1 sec
```

Watch another process by the same Watch Dog:
```
pb = new ProcessBuilder("otherCommand");
Process p2 = pb.start();

watchDog.watch(p2, 2000); // kill the second process after 2 secs
```

Unwatch a process if you don't care any longer:
```
watchDog.unwatch(p);
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)