1) Realize that baby is a MIPS ELF file.
2) Attempt to virtualize with QEMU, this fails due to a debugger check.
3) Load into IDA/Ghidra and find x-refs for the "correct" string.
4) Reverse engineer the encryption (index of current character + 0x17) ^ ciphertext
5) Extract the ciphertext from the hexdump and apply the decryption.

```
string plaintext
for (int i = 0;i<ciphertext.length;i++)
      plaintext.add(ciphertext[i] ^ (i+0x17));
print(plaintext)

```
