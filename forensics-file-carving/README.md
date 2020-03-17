# Observe Closely 
* **Event:** UTCTF 2020
* **Problem: Type:** Forensics
* **Difficulty:** Easy

## Background
Given an image, and told that there are a couple of twists, you are somehow expected to 
find a flag...

#### Solution
This problem is known as Forensics File-Carving. The image you are given is not only an 
image, it is an image with a compressed file attached to the end of it. This image is 
still valid when you open it because the file appended occurs after the image 'end' file 
bytes, which an image viewing program does not read after, therefore you can safely add 
more bytes after an image's ending bytes. In this case, since the given image is a JPG 
(which you may discover by using the 'file' command on this image), you can locate the 
end of the image by searching for the JPG magic bytes found at the end of an image - 0xFF 
and 0xD9. Locating the end of this image, you will discover there are more bytes! If you 
isolate these bytes after the end of the image and move them to their own file and run 
the 'file' command, you will discover that you have a ZIP archive file. Unzipping this 
file gives you a binary, and executing the binary will give you the flag.

As for the twists in this problem, if you think that the flag can be extracted by using a 
steganograpy tool, you are sorely mistaken. You will instead receive a taunting message.
