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
    <version>1.1.0</version>
</dependency>
```

### Watch a Process

#### Create a Watch Dog object:
```
ProcessWatchDog watchDog = new ProcessWatchDog();
```

#### Create a process and watch it by the Watch Dog:
```
ProcessBuilder pb = new ProcessBuilder("myCommand", "myArg1", "myArg2");
Process p = pb.start();

watchDog.watch(p, 1000); // kill the process after 1 sec
```

#### Watch another process by the same Watch Dog:
```
pb = new ProcessBuilder("otherCommand");
Process p2 = pb.start();

watchDog.watch(p2, 2000); // kill the second process after 2 secs
```

### Unwatch a Process

#### Unwatch a process if you don't care any longer:
```
watchDog.unwatch(p);
```

### Keep a Process Alive
Normally, a process should be killed only after a timeout of inactivity. 
To tell the watch dog that a process is still active a heartbeat must be sent. 

#### Send a heartbeat explicitly to reset the timeout:
```
watchDog.heartBeat(p);
```

#### Send a heartbeat automatically with every read byte:
```
p = watchDog.watch(p, 1000);    // reassign the `WatchedProcess` object to the process reference

InputStream is = p.getInputStream();
int b;
while ((b = is.read()) != -1) {
    // heartbeat is sent implicitly with every successful call of `read()` 
}
``` 
Alternatively, send a heartbeat explicitly via `WatchedProcess` object:
```
WatchedProcess wp = watchDog.watch(p, 1000);    // use the returned watched process object
wp.heartBeat();
```

## Release Changes

### 1.1.0
Watched process with a heartbeat.

- `WatchedProcess` class added.
- `heartBeat(Process)` method added to the `ProcessWatchDog` class.

### 1.0.0
Initial version.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)