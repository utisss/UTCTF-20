1) Attempt to use static analysis. 
2) Check main. It calls a mysterious function that tells you if the flag is correct or not. 
3) Realize that there is virtualization is going on and this would be best suited for dynamic analysis. 
4) Patch out debugger check (which would previously segfault to hopefully piss off the user).
5) Debug the binary and what causes the flag to be verified as correct.
6) Find out that constants have been encrypted with DES.
7) Dump an array of 6 64 bit integers and the DES key used to decrypt them.
8) Decrypt them ... still gibberish. 
9) Keep debugging... , find a new buffer and realize that bytes are being extracted from the decrypted integers and  being XORed with an sequence of bytes.
10) Also realize that 9 bytes are missing from the DES decryption.
11) Realize that they're just concatenated at the end of the decryption function and were never encrypted.
12) Split the integers into characters and XOR them with the buffer.
13) This produces the flag.

