import colorsys
import csv
import json
import re
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageOps

PROJECT_ROOT = Path(__file__).resolve().parents[2]
ART_SRC = PROJECT_ROOT / "art-src"
OUT = PROJECT_ROOT / "app/src/main/assets/art"
PINK = ART_SRC / "pink"
SAMPLE = ART_SRC / "onepixel/sprites"
KENNEY = ART_SRC / "kenney"
SCENES = ART_SRC / "scenes"
SPECIES_COUNT = 42
SCENE_COUNT = 6


def load(path):
    with Image.open(path) as image:
        return image.convert("RGBA")


rpg = load(ART_SRC / "rpg/roguelikeSheet_transparent.png")


def cell(x, y):
    return rpg.crop((1 + x * 17, 1 + y * 17, 17 + x * 17, 17 + y * 17))


def components(path, min_area=12):
    im = load(path)
    alpha = im.getchannel("A")
    px = alpha.load()
    seen = set()
    found = []
    for y in range(im.height):
        for x in range(im.width):
            if (x, y) in seen or px[x, y] < 32:
                continue
            queue, points = [(x, y)], []
            seen.add((x, y))
            while queue:
                a, b = queue.pop()
                points.append((a, b))
                for nx, ny in ((a - 1, b), (a + 1, b), (a, b - 1), (a, b + 1)):
                    if (0 <= nx < im.width and 0 <= ny < im.height
                            and (nx, ny) not in seen and px[nx, ny] >= 32):
                        seen.add((nx, ny))
                        queue.append((nx, ny))
            if len(points) >= min_area:
                xs, ys = zip(*points)
                box = (max(0, min(xs) - 1), max(0, min(ys) - 1),
                       min(im.width, max(xs) + 2), min(im.height, max(ys) + 2))
                found.append((box, im.crop(box)))
    found.sort(key=lambda item: (round((item[0][1] + item[0][3]) / 2 / 36), item[0][0]))
    return [sprite for _, sprite in found]


pink_sheets = {
    "Fishes1": components(PINK / "Fishes1_Color.png"),
    "Fishes2": components(PINK / "Fishes2_Color.png"),
}


def sample(name):
    return load(SAMPLE / f"{name}.png")


def species_source(reference):
    if reference.startswith("onepixel/"):
        return sample(reference.removeprefix("onepixel/"))

    match = re.fullmatch(r"pink/(Fishes[12])_(\d+)", reference)
    if match:
        return pink_sheets[match.group(1)][int(match.group(2))]

    if reference == "elthen/octopus_0_0":
        return load(ART_SRC / "elthen/octopus.png").crop((0, 0, 32, 32))

    match = re.fullmatch(r"kenney/rpg_(\d+)_(\d+)", reference)
    if match:
        x, y = int(match.group(1)), int(match.group(2))
        sprite = cell(x, y)
        return sprite.rotate(-55, expand=True) if (x, y) == (45, 24) else sprite

    raise ValueError(f"Unknown species source: {reference}")


def normalized(sprite):
    sprite = sprite.crop(sprite.getbbox())
    scale = max(1, min(76 // sprite.width, 68 // sprite.height))
    resized = sprite.resize((sprite.width * scale, sprite.height * scale), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    canvas.paste(resized, ((96 - resized.width) // 2, (96 - resized.height) // 2), resized)
    return canvas


def shiny(sprite):
    result = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
    source, target = sprite.load(), result.load()
    for y in range(sprite.height):
        for x in range(sprite.width):
            r, g, b, a = source[x, y]
            if not a:
                continue
            if max(r, g, b) < 65:
                target[x, y] = (24, 46, 51, a)
            else:
                h, lightness, saturation = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
                hue = .47 if (x + y) % 7 in (0, 1) else .115
                rr, gg, bb = colorsys.hls_to_rgb(
                    hue, min(.82, lightness * .9 + .18), .58)
                target[x, y] = (int(rr * 255), int(gg * 255), int(bb * 255), a)
    draw = ImageDraw.Draw(result)
    for x, y, color in [(12, 18, "#ffe29a"), (80, 24, "#8ee6d2"), (78, 76, "#ffe29a")]:
        draw.rectangle((x - 1, y - 5, x + 1, y + 5), fill=color)
        draw.rectangle((x - 5, y - 1, x + 5, y + 1), fill=color)
        draw.point((x, y), fill="#fff9dc")
    return result


def make_panel(out, name, dark=False, gold=False, pale=False):
    panel = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    coords = [(2, 3), (3, 3), (4, 3), (2, 4), (3, 4), (4, 4), (2, 5), (3, 5), (4, 5)]
    for i, (x, y) in enumerate(coords):
        panel.paste(cell(x, y), ((i % 3) * 16, (i // 3) * 16))
    if dark or gold or pale:
        px = panel.load()
        for y in range(48):
            for x in range(48):
                r, g, b, a = px[x, y]
                if a and b > r and g > r:
                    color = ((29, 53, 59, a) if dark else
                             ((210, 165, 79, a) if gold else (219, 226, 208, a)))
                    px[x, y] = color
    panel.save(out / name)


TILES = KENNEY / "Tiles"


def tile(index):
    return load(TILES / f"tile_{index:04d}.png")


def paste(base, sprite, x, y):
    base.paste(sprite, (int(x), int(y)), sprite)


def tint(sprite, color=.65, brightness=.85):
    return ImageEnhance.Brightness(ImageEnhance.Color(sprite).enhance(color)).enhance(brightness)


def sky(top, bottom):
    im = Image.new("RGBA", (320, 180))
    draw = ImageDraw.Draw(im)
    for y in range(180):
        t = y / 179
        color = tuple(round(top[i] * (1 - t) + bottom[i] * t) for i in range(3)) + (255,)
        draw.line((0, y, 320, y), fill=color)
    return im


def forest_base(crop, brightness, color):
    im = load(ART_SRC / "forest/Background.png")
    im = im.crop(tuple(crop)).resize((320, 180), Image.Resampling.NEAREST)
    return ImageEnhance.Color(ImageEnhance.Brightness(im).enhance(brightness)).enhance(color)


def water(im, y):
    surface = tint(tile(34).crop((3, 0, 15, 6)), .52, .72)
    draw = ImageDraw.Draw(im)
    draw.rectangle((0, y, 320, 180), fill="#447f87")
    for x in range(-6, 320, 12):
        paste(im, surface, x, y)
    for i in range(24):
        x = (i * 47 + 19) % 310
        yy = y + 13 + (i * 29) % max(20, 167 - y)
        draw.line((x, yy, min(319, x + 8 + (i % 4) * 4), yy), fill="#62999a", width=1)


def ground(im, y, width):
    for x in range(0, width, 18):
        paste(im, tile(0), x, y)
        for yy in range(y + 18, 180, 18):
            paste(im, tile(4), x, yy)


def dock(im, x, y, length):
    for i in range(length):
        paste(im, tile(49), x + i * 18, y)
    for xx in (x + 8, x + (length - 1) * 18 + 8):
        paste(im, tile(89), xx, y + 18)
        paste(im, tile(89), xx, y + 36)


def mountains(im):
    mountain = ImageOps.colorize(
        tile(6).crop((3, 3, 15, 15)).convert("L"), black="#294d4c", white="#789187"
    ).convert("RGBA")
    pattern = Image.new("RGBA", (320, 180))
    for x in range(0, 320, 12):
        for y in range(24, 110, 12):
            paste(pattern, mountain, x, y)
    mask = Image.new("L", (320, 180), 0)
    draw = ImageDraw.Draw(mask)
    draw.polygon([(0, 99), (45, 48), (77, 77), (119, 31), (176, 99)], fill=190)
    draw.polygon([(97, 99), (176, 45), (213, 72), (258, 38), (320, 93), (320, 110), (0, 110)], fill=255)
    im.paste(pattern, (0, 0), mask)
    draw = ImageDraw.Draw(im)
    draw.polygon([(101, 49), (119, 31), (137, 49), (126, 45), (119, 50), (112, 44)], fill="#d4dfcf")
    draw.rectangle((0, 99, 320, 106), fill="#375e58")


def lotus_pads(im, positions):
    for x, y in positions:
        pad = Image.new("RGBA", (22, 10), (0, 0, 0, 0))
        draw = ImageDraw.Draw(pad)
        draw.ellipse((0, 1, 21, 9), fill="#5c9970")
        draw.polygon([(11, 5), (21, 1), (21, 6)], fill=(0, 0, 0, 0))
        paste(im, pad, x, y)


def tiles(im, layer):
    for item in layer["items"]:
        sprite = tile(item["tile"])
        if "color" in layer or "brightness" in layer:
            sprite = tint(sprite, layer.get("color", .65), layer.get("brightness", .85))
        paste(im, sprite, item["x"], item["y"])


def tile_grid(im, layer):
    for x in range(*layer["x_range"]):
        for y in range(*layer["y_range"]):
            paste(im, tile(layer["tile"]), x, y)


def skyline(im, layer):
    draw = ImageDraw.Draw(im)
    baseline = layer["baseline"]
    for building in layer["buildings"]:
        x, height = building["x"], building["height"]
        draw.rectangle((x, baseline - height, x + 17, baseline), fill="#586b6a")
        draw.rectangle((x + 4, baseline + 5 - height, x + 7, baseline + 8 - height), fill="#efc777")


def polygons(im, items):
    draw = ImageDraw.Draw(im)
    for item in items:
        draw.polygon([tuple(point) for point in item["points"]], fill=item["fill"])


def background(spec):
    if spec["type"] == "forest":
        return forest_base(spec["crop"], spec["brightness"], spec["color"])
    if spec["type"] == "sky":
        return sky(spec["top"], spec["bottom"])
    raise ValueError(f"Unknown background type: {spec['type']}")


def build_scene(spec):
    if spec["size"] != [320, 180]:
        raise ValueError("Scene canvas must remain 320x180")
    im = background(spec["background"])
    for layer in spec["layers"]:
        kind = layer["type"]
        if kind == "water":
            water(im, layer["y"])
        elif kind == "ground":
            ground(im, layer["y"], layer["width"])
        elif kind == "dock":
            dock(im, layer["x"], layer["y"], layer["length"])
        elif kind == "mountains":
            mountains(im)
        elif kind == "lotus_pads":
            lotus_pads(im, layer["positions"])
        elif kind == "tiles":
            tiles(im, layer)
        elif kind == "tile_grid":
            tile_grid(im, layer)
        elif kind == "skyline":
            skyline(im, layer)
        elif kind == "polygons":
            polygons(im, layer["items"])
        else:
            raise ValueError(f"Unknown scene layer type: {kind}")
    return im


def read_species_rows():
    with (ART_SRC / "species_map.csv").open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source)
        if reader.fieldnames != ["id", "name", "source"]:
            raise ValueError("species_map.csv must have id,name,source columns")
        rows = list(reader)
    if len(rows) != SPECIES_COUNT or [int(row["id"]) for row in rows] != list(range(SPECIES_COUNT)):
        raise ValueError("species_map.csv must contain contiguous catalog IDs 00-41")
    return rows


def write_attribution(out):
    notice = (ART_SRC / "NOTICE.txt").read_text(encoding="utf-8").rstrip()
    forest_license = (ART_SRC / "forest/license.txt").read_text(encoding="utf-8")
    onepixel_license = (ART_SRC / "onepixel/README.txt").read_text(encoding="utf-8")
    attribution = (notice + "\n\nORIGINAL edermunizz LICENSE\n" + forest_license
                   + "\n\nORIGINAL onepixelmore LICENSE\n" + onepixel_license)
    (out / "ATTRIBUTION.txt").write_text(attribution, encoding="utf-8")


def generate(out=OUT):
    out.mkdir(parents=True, exist_ok=True)
    rows = read_species_rows()
    for row in rows:
        index = int(row["id"])
        normal = normalized(species_source(row["source"]))
        normal.save(out / f"species_{index:02d}.png")
        shiny(normal).save(out / f"species_{index:02d}_s.png")

    for legacy in out.glob("species_*_l.png"):
        legacy.unlink()

    fishing = load(PINK / "Fishing.png")
    fishing.crop((0, 0, 49, 50)).save(out / "angler_idle.png")
    fishing.crop((50, 0, 99, 50)).save(out / "angler_bite.png")
    completed = load(PINK / "Fishing_Completed.png")
    completed.crop((0, 0, 30, 48)).save(out / "angler_catch.png")
    sample("hook").save(out / "hook.png")

    make_panel(out, "ui_panel.png")
    make_panel(out, "ui_dark.png", dark=True)
    make_panel(out, "ui_gold.png", gold=True)
    make_panel(out, "ui_slot.png", pale=True)

    for index in range(SCENE_COUNT):
        spec = json.loads((SCENES / f"scene_{index}.json").read_text(encoding="utf-8"))
        build_scene(spec).convert("RGB").save(out / f"scene_{index}.png")

    shutil.copyfile(ART_SRC / "species_map.csv", out / "species_map.csv")
    write_attribution(out)
    print(f"Generated {len(list(out.glob('*.png')))} PNG assets in {out}")


if __name__ == "__main__":
    generate()
