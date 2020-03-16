# Zero
* **Event:** UTCTF
* **Problem Type:** Forensics
* **Point Value / Difficulty:** Easy
* **(Optional) Tools Required / Used:** Online unicode stego tool
​
## Steps​
#### Step 1
The problem name is a reference to Unicode zero-width characters, characters which appear invisible in text when next to the Latin alphabet. The text in the file being generic Lorem ipsum should hint that there is more to meet the eye than just the words you see.

#### Step 2
Zero-width characters can be used to encode information in files, so if search on Google you should be able to find a tool like [this](https://330k.github.io/misc_tools/unicode_steganography.html), which you can just paste the text into and decode the flag.