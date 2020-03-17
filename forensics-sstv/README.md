# 1 Frame per Minute 
* **Event:** UTCTF 20202
* **Problem: Type:** Forensics
* **Difficulty:** Easy

## Background
SSTV is an archaic way of transmitting images via Radio Wave. Thus, given an SSTV 
transmission file, you are expected to extract an image, and see what happens from there.

#### Solution
You must find a program (qsstv is recommended) which can recreate an image from an SSTV 
transmission, you must also identify which mode of SSTV is used to encode the image. You 
can figure out the mode based on a hint given in the description: 'SSTV in a mode called 
Martian?' There is a mode called 'Martin' which is similar to 'Martian' and is the mode 
needed to decode the image. Once you have decoded the image, you will see that the flag 
is cleared printed in the image.
