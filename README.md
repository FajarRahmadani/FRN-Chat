# FRN Chat

A lightweight, multi-client local chat application written in plain Java with no external libraries. Messages, photos, and videos are sent over TCP sockets and encrypted on the client side with AES-256-GCM.

This desktop client can chat in the same room as the Android app.

## Features

- Group chat through a single server
- Chat-bubble interface (Java Swing) with a timestamp on every message
- Send and open photos and videos
- Join and leave notifications
- AES-256-GCM encryption, with the key derived from a shared passphrase using PBKDF2
- Runs on Windows, macOS, and Linux

## Download

No Java installation needed. Download the Windows installer from [Releases](https://github.com/FajarRahmadani/FRN-Chat/releases/latest).

## Other Clients

| Repository | Platform |
|------------|----------|
| [FRNChatKotlin](https://github.com/FajarRahmadani/FRNChatKotlin) | Android (Kotlin) |
| [FRNChatServer](https://github.com/FajarRahmadani/FRNChatServer) | Server on an Android phone |

All clients use port 2106 and the same passphrase, so phones and desktops can chat in the same room.

```text
Phone (Android) ─┐
                 ├──► Server (laptop or phone) ◄── Desktop (Java Swing)
Phone (Android) ─┘          port 2106
```

## Running from Source

Requires JDK 17 or newer.

```bash
git clone https://github.com/FajarRahmadani/FRN-Chat.git
cd FRN-Chat
javac -d out src/ChatServer.java src/ChatClient.java
```

**1. Start the server** (on one device only)

```bash
java -cp out ChatServer
```

The server listens on port **2106**. The server can also run on a phone using the FRNChatServer app.

**2. Start a client**

```bash
java -cp out ChatClient
```

Enter the **server IP** and a **username**, then click **Connect**. All devices must be on the same network.

### Passphrase

Every client must use the **exact same** passphrase, including capitalization and spaces. Otherwise messages cannot be decrypted. The client reads the passphrase from the `FRN_CHAT_SECRET` environment variable or from a `.env` file (see `.env.example`), or asks for it at startup.

The default passphrase of the Android app is:

```text
fajar ganteng baik hati dan tidak sombong
```

To change it, edit `EMBEDDED_SECRET` in the Android app source and rebuild, then use the same passphrase in the desktop client.

## Building a Windows Installer (.exe)

Requires JDK 17 or newer and [WiX Toolset 3.x](https://wixtoolset.org/).

```bash
javac -d out src/ChatServer.java src/ChatClient.java
mkdir dist
jar --create --file dist/frnchat.jar --main-class ChatClient -C out .
jpackage --type exe --name "FRN Chat" --input dist --main-jar frnchat.jar --main-class ChatClient --app-version 1.0 --win-shortcut --win-menu
```

## Security

This project is built for learning and for trusted local networks. It has no authentication, TLS, or identity verification. Because the default passphrase is public, the encryption only protects against ordinary network eavesdropping. Do not use it on the open internet or for sensitive data. Never commit your `.env` file to the repository.

## License

[MIT](LICENSE)
