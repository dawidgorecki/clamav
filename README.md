# Java ClamAV Client
A Java client for interacting with a ClamAV antivirus server. This client allows you to send commands to the ClamAV server, 
check its status, and scan files or streams for viruses.

## Features
- Connect to a ClamAV server via TCP socket.
- Send commands to check server status and retrieve the version.
- Scan files and byte arrays for viruses.

## Installation
To use this library, you can clone the repository or include it in your project.

### Prerequisites
- Java 11 or higher

### Maven
If you're using Maven, include the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.dawidgorecki</groupId>
    <artifactId>clamav-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Create a Client
To create a client, instantiate the ClamAVClient with the ClamAV server's host and port:
```java
ClamAVClient client = new ClamAVClient("localhost", 3310);
```

### Check server status
To check if the server is reachable, use the ping() method:
```java
if (client.ping()) {
    System.out.println("ClamAV server is reachable!");
} else {
    System.out.println("ClamAV server is not reachable.");
}
```

### Get version
To retrieve the version of the ClamAV server:
```java
String version = client.getVersion();
System.out.println("ClamAV Version: " + version);
```

### Scan for viruses
To scan a file for viruses:
```java
File fileToScan = new File("path/to/file.txt");
ScanResult result = client.scan(fileToScan);
```
To scan a byte array:
```java
byte[] eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
        .getBytes(StandardCharsets.US_ASCII);
ScanResult result = client.scan(eicar);
```

## License
This project is licensed under the MIT License - see the LICENSE file for details.