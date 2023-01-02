# CompressChat

### This mod is very experimental.
### Also, trusting random players with
### not sending something malicious in chat
### is not very bright idea, as well as
### passing random stuff through the decompression
### library. But if you really want to use it,
### the requirements are below.

Mod that passes your messages through the compression algorithm, so you could write a message longer than 256 symbols in length.
In order to view compressed messages, other players should have this mod installed too.

## Requirements

1) Java 19+ with preview features enabled (add --enable-preview JVM argument)
2) [Brotli](https://github.com/google/brotli) library should be available on the library path (so JVM can find and load it)

Many Linux distributions provide packages for Brotli, so you can avoid building it by yourself.
Check your Linux distro's repositories for details. I have to express my deepest condolences to Windows users.