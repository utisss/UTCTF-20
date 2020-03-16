# Hill
* **Event:** UTCTF
* **Problem Type:** Crypto
* **Point Value / Difficulty:** Easy
* **(Optional) Tools Required / Used:** None

## Steps​
The prompt and title of the problem should lead you to the hill cipher, which is basically just matrix multiplication as a cipher. Reading the Wikipedia page, you can see that this cipher is vulnerable to a "known-plaintext" attack. Looking at the encrypted text we have, it looks to be in the flag format, so we know that the beginning `wznqca` maps to `utflag`. From here you can bruteforce the key or set up a system of linear equations to solve for the key.

How do we know the key is 2x2? Well the problem actually wouldn't be solvable if it were any larger. Bruteforce would be infeasible, and there aren't enough plaintext ciphertext pairs in this problem to be able to solve for the key.

What letters are encrypted in the problem? This was an arbitrary consideration that fucked the problem. Only letters are encrypted, so all special characters and numbers are plaintext. Also, uppercase letters are encrypted as though they were their lowercase equivalents.