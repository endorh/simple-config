import PIL
import numpy as np
import os
import sys

path = sys.argv[1]
list_im = [os.path.join(path, name) for name in os.listdir(path)
           if name.endswith(".png") or name.endswith(".jpg")]

imgs = [PIL.Image.open(i) for i in list_im]

min_shape = sorted([(np.sum(i.size), i.size) for i in imgs])[0][1]
imgs_comb = np.hstack([np.asarray(i.resize(min_shape)) for i in imgs])

imgs_comb = PIL.Image.fromarray(imgs_comb)
dest = os.path.join(os.path.dirname(sys.argv[1]), sys.argv[2])
imgs_comb.save(dest)

print(f"Saved {dest}")
