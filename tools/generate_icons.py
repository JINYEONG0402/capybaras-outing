"""capybara.png로 앱 아이콘을 만듭니다. Pillow가 필요합니다.

실행: python tools/generate_icons.py
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SOURCE = os.path.join(ROOT, "assets", "capybara.png")
WEB = os.path.join(ROOT, "teavm", "web")
ANDROID = os.path.join(ROOT, "android", "src", "main", "res", "drawable", "ic_launcher.png")


def make(size):
    icon = Image.new("RGBA", (size, size), (124, 198, 242, 255))
    draw = ImageDraw.Draw(icon)
    draw.rectangle((0, int(size * 0.8), size, size), fill=(97, 184, 51, 255))
    capy = Image.open(SOURCE).convert("RGBA")
    height = int(size * 0.74)
    capy = capy.resize((int(capy.width * height / capy.height), height), Image.LANCZOS)
    icon.alpha_composite(capy, ((size - capy.width) // 2, int(size * 0.96) - height))
    return icon


os.makedirs(WEB, exist_ok=True)
make(180).convert("RGB").save(os.path.join(WEB, "apple-touch-icon.png"))
make(192).convert("RGB").save(os.path.join(WEB, "icon-192.png"))
make(512).convert("RGB").save(os.path.join(WEB, "icon-512.png"))
make(192).save(ANDROID)
print("icons written")
