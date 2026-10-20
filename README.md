# FRN Chat

A lightweight, multi-client local chat application written in plain Java with no external libraries. Run the server and clients on desktop, and optionally join from Android phones. Messages, photos, and videos are sent over TCP sockets and encrypted on the client side with AES-256-GCM.

## Features

- Group chat through a single server
- Chat-bubble interface (Java Swing) with a timestamp on every message
- Send and open photos and videos
- Join and leave notifications
- AES-256-GCM encryption, with the key derived from a shared passphrase using PBKDF2
- Runs on Windows, macOS, and Linux

## Download

No Java installation needed. Download from [Releases](https://github.com/FajarRahmadani/FRN-Chat/releases/latest), extract, and run:

- `FRNChat-Server-Windows-v1.0.zip`: run `FRN Chat Server.exe` on one computer (keep the window open).
- `FRNChat-Windows-v1.0.zip`: run `FRN Chat.exe` on every computer that wants to chat.

If Windows SmartScreen shows a warning, click **More info → Run anyway**.

## Running from Source

Requires JDK 17 or newer.

```bash
git clone https://github.com/FajarRahmadani/FRN-Chat.git
cd FRN-Chat
javac -d out src/ChatServer.java src/ChatClient.java
```

**1. Start the server** (on one computer only)

```bash
java -cp out ChatServer
```

The server listens on port **2106**. Run it on one computer; other computers (and phones, optionally) connect to that computer's IP address. To find the IP address on Windows, run `ipconfig` and look for **IPv4 Address**.

**2. Start a client**

```bash
java -cp out ChatClient
```

Enter the **server IP** and a **username**, then click **Connect (Hubungkan)**. All devices must be on the same network. When the server runs on the same computer, use `localhost`.

### Passphrase

Every client must use the **exact same** passphrase, including capitalization and spaces. Otherwise messages cannot be decrypted. The client reads the passphrase from the `FRN_CHAT_SECRET` environment variable or from a `.env` file (see `.env.example`).

The default passphrase of the Android app is:

```text
fajar ganteng baik hati dan tidak sombong
```

To change it, edit `EMBEDDED_SECRET` in the Android app source and rebuild, then use the same passphrase in the desktop client.

## Building the Windows App

Requires JDK 17 or newer. No other tools are needed.

```bash
javac -d out src/ChatServer.java src/ChatClient.java
mkdir dist
jar --create --file dist/frnchat.jar --main-class ChatClient -C out .
jpackage --type app-image --name "FRN Chat" --input dist --main-jar frnchat.jar --main-class ChatClient --app-version 1.0 --dest release
```

The app is created in `release/FRN Chat/`. Zip that folder to distribute it.

## Optional: Android

Phones can join the same chat room with the Android app. This is optional; the desktop server and client work on their own.

| Repository | Description |
|------------|-------------|
| [FRNChatKotlin](https://github.com/FajarRahmadani/FRNChatKotlin) | Android chat client (Kotlin) |
| [FRNChatServer](https://github.com/FajarRahmadani/FRNChatServer) | Optional: run the server on an Android phone instead of a computer |

All clients use port 2106 and the same passphrase.

```text
Desktop client ─┐
                ├──► Server (desktop by default) ◄── Android client (optional)
Desktop client ─┘            port 2106
```

## Security

This project is built for learning and for trusted local networks. It has no authentication, TLS, or identity verification. Because the default passphrase is public, the encryption only protects against ordinary network eavesdropping. Do not use it on the open internet or for sensitive data. Never commit your `.env` file to the repository.

## License

[MIT](LICENSE)
