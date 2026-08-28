# FRN Chat

## About FRN Chat

FRN Chat is a lightweight, multi-client chat application written in plain Java. It uses a simple TCP socket server to relay messages between connected clients, each running a Swing-based desktop GUI. Messages are encrypted client-side with AES-256-GCM before being sent over the wire.

## Features

- Multi-client TCP chat server that broadcasts messages to all connected clients
- Swing desktop GUI client with a chat-bubble style interface
- Join/leave system notifications
- Per-message timestamps
- Client-side AES-256-GCM message encryption using a shared passphrase

## Technologies

- **Language:** Java (JDK 26)
- **GUI:** Java Swing
- **Networking:** `java.net` (`ServerSocket` / `Socket`)
- **Cryptography:** `javax.crypto` — AES/GCM with a PBKDF2 (HMAC-SHA256) derived key
- **Packaging:** `jpackage` (used to produce the Windows `.exe` build)

No external libraries, frameworks, database, or web APIs are used.

## Requirements

- JDK 26 or newer (to compile and run from source)
- A shared network (LAN, VPN, or reachable IP) between the server and clients

## Installation

Clone the repository:

```bash
git clone https://github.com/<your-username>/FRN-Chat.git
cd FRN-Chat
```

Compile the source:

```bash
javac -d out src/ChatServer.java src/ChatClient.java
```

## Environment Configuration

FRN Chat needs one shared secret so clients can encrypt/decrypt each other's messages.

1. Copy the example file:

   ```bash
   cp .env.example .env
   ```

2. Set `FRN_CHAT_SECRET` to a passphrase of your choosing, and share it privately (out-of-band) with whoever you're chatting with. **Do not commit `.env` or share this value publicly** — anyone who knows it can decrypt the chat traffic.

3. Export it into your shell before running the client, for example:

   ```bash
   export FRN_CHAT_SECRET="your-shared-passphrase"
   ```

   If `FRN_CHAT_SECRET` isn't set, the client will prompt you for it when it starts instead.

## Database Setup

Not applicable — FRN Chat has no database. All state is held in memory by the running server process.

## Running the Application

Start the server first:

```bash
java -cp out ChatServer
```

By default it listens on port `2106`.

Then start one or more clients:

```bash
java -cp out ChatClient
```

In the client window, enter the server's IP address (use `localhost` if running on the same machine) and a username, then click **Hubungkan** (Connect).

## Build

To produce a native Windows launcher with `jpackage` (bundles its own JRE):

```bash
jpackage --input out --name "FRN Chat" --main-class ChatClient --main-jar ChatClient.jar --type exe
```

Native build output (`.exe` and the bundled runtime image) is not committed to this repository — see `.gitignore`.

## Project Structure

```
FRN-Chat/
├── README.md
├── .gitignore
├── .env.example
└── src/
    ├── ChatServer.java   # TCP server: accepts clients, broadcasts messages
    └── ChatClient.java   # Swing GUI client: connects, encrypts/decrypts, displays chat
```

## Usage

1. Run `ChatServer` on the machine that will host the chat.
2. Run `ChatClient` on each machine that wants to join, pointing it at the server's IP.
3. Make sure every client is configured with the same `FRN_CHAT_SECRET` so messages can be decrypted correctly.
4. Type a message and press Enter or click **Kirim** (Send).

## Security

- Message encryption depends entirely on `FRN_CHAT_SECRET` being kept private between the people using it. It must never be hardcoded in source or committed to this repository.
- `.env` is excluded from version control via `.gitignore`; only `.env.example` (with no real values) is committed.
- This project has no authentication, TLS, or identity verification between server and clients — it is intended for trusted/local use, not as a hardened, internet-facing chat service.

## Contributors

- Fajar Rahmadani

## License

No license has been specified for this project yet.
