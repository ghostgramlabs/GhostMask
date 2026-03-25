# GhostMask 👻

Hide secret text and images inside a normal-looking image using encryption and steganography.

## What It Does

GhostMask embeds encrypted secrets (text, images, or both) into the least-significant bits of a cover image's pixel data. The result is a PNG that looks identical to the original but carries hidden, encrypted content that can only be revealed with the correct password.

## Architecture

```
com.ghostgramlabs.ghostmask/
├── stego/              # Core processing engines
│   ├── CryptoEngine    # AES-256-GCM encryption with PBKDF2 key derivation
│   ├── PayloadBuilder  # Binary payload format construction
│   ├── PayloadParser   # Payload extraction and validation
│   ├── StegoEncoder    # LSB bit embedding into bitmap pixels
│   ├── StegoDecoder    # LSB bit extraction from bitmap pixels
│   ├── CapacityCalc    # Cover image capacity computation
│   ├── ImageCompress   # JPEG compression for secret images
│   ├── BitmapBitWriter # Pixel-level bit writing
│   └── BitmapBitReader # Pixel-level bit reading
├── viewmodel/          # UI state management
│   ├── HideSecretsVM   # Encode flow state & actions
│   └── RevealSecretsVM # Decode flow state & actions
├── ui/                 # Jetpack Compose screens
│   ├── screens/        # Dashboard, HideSecrets, RevealSecrets
│   ├── components/     # Reusable components
│   ├── navigation/     # Nav graph
│   └── theme/          # Material3 dark theme
├── util/               # File I/O and sharing
│   ├── FileSaveManager # MediaStore PNG saves
│   ├── ShareManager    # FileProvider-based sharing
│   └── BitmapUtils     # Safe bitmap loading with downsampling
└── MainActivity        # Single-activity entry point
```

## How Encoding Works

1. **Input**: User provides a cover image + secret text and/or secret image + password
2. **Compress**: Secret image is JPEG-compressed (quality 60) to minimize payload size
3. **Build payload**: Binary header prepended:
   ```
   [3B magic "GM1"][1B version][1B flags][4B text_len][4B img_len]
   ```
4. **Encrypt**: Combined text+image bytes encrypted with AES-256-GCM
   - Key derived via PBKDF2-HMAC-SHA256 (100k iterations)
   - Random 16-byte salt + 12-byte IV stored in the encrypted package
5. **Embed**: Encrypted payload bits written into LSBs of RGB channels
   - 3 bits per pixel (R, G, B least-significant bits)
   - Alpha channel preserved
   - First 32 bits encode payload length
6. **Export**: Result saved as lossless PNG

## Payload Format

```
Offset  Size  Field
0       3     Magic "GM1"
3       1     Version (0x01)
4       1     Flags (bit0=hasText, bit1=hasImage)
5       4     Text byte length
9       4     Image byte length  
13      16    Salt
29      12    IV/Nonce
41      N     AES-GCM ciphertext + auth tag
```

## Capacity

Each pixel contributes 3 usable bits. Capacity formula:

```
bytes = floor(width × height × 3 / 8) − 4
```

| Image Size | Capacity |
|-----------|----------|
| 640×480   | ~115 KB  |
| 1920×1080 | ~760 KB  |
| 4032×3024 | ~4.5 MB  |

## Limitations

- **Fragile to recompression**: Any app that re-encodes the image (social media, messaging apps) will destroy the hidden data. Save locally or share through channels that preserve exact file bytes.
- **PNG only**: Output must be PNG. JPEG compression destroys LSB data.
- **Memory usage**: Very large cover images consume significant memory during processing.
- **Not stealth-grade**: While visually imperceptible, statistical analysis (chi-square, RS analysis) can detect LSB steganography. This is a privacy tool, not a forensic-resistance tool.

## Build & Run

```bash
# Clone and open in Android Studio
cd GhostMask

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
```

**Requirements**: Android Studio Hedgehog+, JDK 17, Android SDK 34, min API 26.

## Security

- AES-256-GCM authenticated encryption
- PBKDF2-HMAC-SHA256 with 100,000 iterations for key derivation  
- Random salt and nonce per encryption — no IV reuse
- Wrong password detection via GCM authentication tag verification
- No custom or weak crypto — standard javax.crypto APIs only
