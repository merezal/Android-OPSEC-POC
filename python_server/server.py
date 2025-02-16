import socket
import base64
import hashlib
import json
import time
import threading
import os
from cryptography.hazmat.primitives.asymmetric import x25519
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes, serialization
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

# Ensure 'data' directory exists
data_dir = "./data"
os.makedirs(data_dir, exist_ok=True)

# Generate ECDH key pair (Server)
server_private_key = x25519.X25519PrivateKey.generate()
server_public_key = server_private_key.public_key()

# Convert keys to bytes
server_private_key_bytes = server_private_key.private_bytes(
    encoding=serialization.Encoding.Raw,
    format=serialization.PrivateFormat.Raw,
    encryption_algorithm=serialization.NoEncryption()
)
server_public_key_bytes = server_public_key.public_bytes(
    encoding=serialization.Encoding.Raw,
    format=serialization.PublicFormat.Raw
)

print("Server private key (Base64):", base64.b64encode(server_private_key_bytes).decode())
print("Server public key (Base64):", base64.b64encode(server_public_key_bytes).decode())

# Start Server
HOST = "0.0.0.0"  # Listen on all available interfaces
PORT = 4444

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((HOST, PORT))
server.listen(10)  # Accept up to 10 simultaneous connections

print(f"🌍 Server is listening on {HOST}:{PORT}...")


def handle_client(client_socket, addr):
    """Handles individual client connection."""
    try:
        print(f"\n🔗 New connection from {addr}")

        # Receive client's public key (32 bytes)
        client_public_key_bytes = client_socket.recv(32)
        client_socket.sendall(server_public_key_bytes)  # Send server's public key

        # Convert client's public key to X25519 format
        client_public_key = x25519.X25519PublicKey.from_public_bytes(client_public_key_bytes)

        print(f"🔑 Client public key (Base64): {base64.b64encode(client_public_key_bytes).decode()}")

        # Compute shared secret
        shared_secret = server_private_key.exchange(client_public_key)

        hashed_shared_secret = hashlib.sha256(shared_secret).digest()

        print(f"🔐 Shared Secret (Base64): {base64.b64encode(shared_secret).decode()}")

        # Key derivation using HKDF
        aes_key = HKDF(
            algorithm=hashes.SHA256(),
            length=32,
            salt=b'\x00' * 16,
            info=b"ecdh-key-exchange",
        ).derive(hashed_shared_secret)

        print(f"🛠️ AES Key (Base64): {base64.b64encode(aes_key).decode()}")

        # Receive encrypted data
        encrypted_data = b""
        while True:
            chunk = client_socket.recv(4096)
            if not chunk:
                break  # Exit loop if no more data
            encrypted_data += chunk

        # Extract IV (first 16 bytes) and ciphertext
        iv, ciphertext = encrypted_data[:16], encrypted_data[16:]

        print(f"📥 Received IV (Base64): {base64.b64encode(iv).decode()}")
        print(f"📥 First 32 bytes of encrypted data (Base64): {base64.b64encode(ciphertext[:32]).decode()}")

        # Decrypt data
        cipher = AES.new(aes_key, AES.MODE_CBC, iv)
        decrypted_data = unpad(cipher.decrypt(ciphertext), AES.block_size).decode("utf-8")

        # Convert JSON string to dictionary
        json_data = json.loads(decrypted_data)

        # Append sender's IP & Port
        json_data["sender_ip"] = addr[0]
        json_data["sender_port"] = addr[1]

        # Save to file with timestamp
        timestamp = int(time.time())
        file_path = os.path.join(data_dir, f"{timestamp}.json")

        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(json_data, f, indent=4, sort_keys=True)

        print(f"✅ Data received & saved to {file_path}")

    except Exception as e:
        print(f"❌ Error handling client {addr}: {e}")

    finally:
        client_socket.close()
        print(f"🔌 Connection closed: {addr}")


# Accept multiple connections using threads
while True:
    client_socket, client_addr = server.accept()
    client_thread = threading.Thread(target=handle_client, args=(client_socket, client_addr))
    client_thread.start()
