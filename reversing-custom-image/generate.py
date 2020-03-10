from PIL import Image
import numpy as np

im = Image.open("pic.png")

pixels = np.array(im.getdata())

width, height = im.size
print(width, height)

with open("out.txt", "wb") as out:
    out.write(b'PNG2')
    out.write(b'width=')
    out.write(width.to_bytes(2, byteorder="big"))
    out.write(b'height=')
    out.write(height.to_bytes(2, byteorder="big"))
    for r, g, b in pixels:
        out.write(bytes([r, g, b]))