from matplotlib import pyplot as plt
import numpy as np

with open("pic.png2", "rb") as f:
    print(f.read(4)) # 'PNG2'
    print(f.read(6)) # 'width='
    width = int.from_bytes(f.read(2), byteorder="big")
    print(f.read(7)) # 'height='
    height = int.from_bytes(f.read(2), byteorder="big")
    data = []
    for i in range(height):
        row = []
        for j in range(width):
            row.append((int.from_bytes(f.read(1), byteorder='big'), int.from_bytes(f.read(1), byteorder='big'), int.from_bytes(f.read(1), byteorder='big')))
        data.append(row)

    image = np.array(data, dtype=np.uint8)
    plt.imshow(image, interpolation="none")
    plt.show()