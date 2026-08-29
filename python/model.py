import torch
import torch.nn as nn


class ResBlock(nn.Module):
    """Pre-activation residual block: BN -> ReLU -> Conv -> BN -> ReLU -> Conv + skip."""
    def __init__(self, channels: int):
        super().__init__()
        self.bn1 = nn.BatchNorm2d(channels)
        self.conv1 = nn.Conv2d(channels, channels, kernel_size=3, padding=1, bias=False)
        self.bn2 = nn.BatchNorm2d(channels)
        self.conv2 = nn.Conv2d(channels, channels, kernel_size=3, padding=1, bias=False)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        residual = x
        x = torch.relu(self.bn1(x))
        x = self.conv1(x)
        x = torch.relu(self.bn2(x))
        x = self.conv2(x)
        return x + residual


class PolicyNetwork(nn.Module):
    def __init__(self, channels: list = None, num_res_blocks: int = 3):
        super().__init__()
        if channels is None:
            channels = [17, 64]

        self.stem = nn.Sequential(
            nn.Conv2d(channels[0], channels[1], kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(channels[1]),
            nn.ReLU(inplace=True),
        )

        res_ch = channels[1]
        self.res_blocks = nn.Sequential(*[ResBlock(res_ch) for _ in range(num_res_blocks)])

        self.head = nn.Sequential(
            nn.BatchNorm2d(res_ch),
            nn.ReLU(inplace=True),
            nn.Conv2d(res_ch, 128, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.Flatten(),
            nn.Linear(128 * 8 * 8, 1024),
            nn.ReLU(inplace=True),
            nn.Linear(1024, 4096),
        )
        self._init_weights()

    def _init_weights(self):
        for m in self.modules():
            if isinstance(m, nn.Conv2d):
                nn.init.kaiming_normal_(m.weight, mode="fan_out", nonlinearity="relu")
            elif isinstance(m, nn.BatchNorm2d):
                nn.init.constant_(m.weight, 1)
                nn.init.constant_(m.bias, 0)
            elif isinstance(m, nn.Linear):
                nn.init.kaiming_normal_(m.weight, mode="fan_in", nonlinearity="relu")
                nn.init.constant_(m.bias, 0)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = self.stem(x)
        x = self.res_blocks(x)
        x = self.head(x)
        return x
