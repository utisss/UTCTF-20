# .PNG2
* **Event:** UTCTF
* **Problem Type:** Reverse Engineering
* **Point Value / Difficulty:** Medium
* **(Optional) Tools Required / Used:** None



## Steps​
#### Step 1
From the problem description, you can guess that this is a custom file format that you'll have to decipher. Opening the file in any text editor you can see the strings 'PNG2', 'width=', and 'height=' at the beginning of the file. You can then see that the width and height are probably the 16 bits after the 'width=' and 'height='.

#### Step 2
Next you have to guess what the rest of the file is, and the prompt tells you that the first pixel is #7F7F7F. You can see that these are the first bytes written after the header, so you can assume that the pixels are written in RGB format, 1 byte to each color. Now the only question is row or column major order? And if you just make the intuitive guess that it is row major you would be right, or if you guessed incorrectly you would easily see the image is wrong.

#### Step 3
Now that we have all of the information we need about the file format, we just need to turn the bytes into an image. Its pretty easy to create an image in Python using either Pillow or matplotlib, so you can see my script in 'solution.py' in the repo. Simply read in the bytes, and create the image. Once you see the image, the flag is just sitting there ready to be copied.
