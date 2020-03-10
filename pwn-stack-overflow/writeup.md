# Bepis
* **Event:** BepisCTF (ISSS CTF 09-06-2016)
* **Problem Type:** Binary Exploitation

## Background
[Netcat](https://en.wikipedia.org/wiki/Netcat) is an invaluable tool for testing networking and connecting to servers.

[Objdump](https://linux.die.net/man/1/objdump) Allows you to dump compiled code into readable assembly

[Pwntools](https://github.com/arthaud/python3-pwntools) is very useful when exploiting more complicated services.

[GDB](http://man7.org/linux/man-pages/man1/gdb.1.html) allows you to easily debug programs and see what is happening inside them at runtime.

## Exploit
```
We just need to call flag() with rdi = 0x1337
There is a pop rdi; ret; gadget in the binary (found using ROPGadget)
We can just overwrite our return addr with pop_rdi -> 0x1337 -> flag()

See exploit.py for more details
```
