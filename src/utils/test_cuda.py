import torch

# Створюємо випадковий тензор
tensor = torch.randn(3, 3)

# Переносимо його на GPU
tensor_gpu = tensor.to("cuda")

print("Tensor on GPU:", tensor_gpu)
print("Current device:", torch.cuda.current_device())
