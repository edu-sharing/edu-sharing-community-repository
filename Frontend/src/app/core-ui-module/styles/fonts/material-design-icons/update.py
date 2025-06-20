from fontTools.ttLib import TTFont
import os

# Load your TTF font
font_old = []

with open("codepoints-old", "r") as file:
    for line in file:
        words = line.strip().split()

        if words:  # ensure the line is not empty
            font_old.append(words[0])

print(font_old)
font_new = TTFont("MaterialSymbolsOutlined-Regular.ttf")

# Get glyph names
font_new = font_new.getGlyphOrder()

only_in_old = set(font_old) - set(font_new)
print('Only in old: ')
for search_string in only_in_old:
    hits = 0
    search_dir = "/home/torsten/.edusharing/enterprise/link/maven/fixes/10.0/repository/Frontend/src"

    for root, dirs, files in os.walk(search_dir):
        for filename in files:
            if filename.endswith(".ts") or filename.endswith('.html'):
                filepath = os.path.join(root, filename)
                try:
                    with open(filepath, 'r', encoding='utf-8') as file:
                        for i, line in enumerate(file, start=1):
                            if '"' + search_string + '"'  in line or "'" + search_string + "'" in line:
                                if hits == 0:
                                    print("\n\n" + search_string)
                                if hits < 5:
                                    print(f"{filepath} (line {i}): {line.strip()}")
                                hits+=1
                except (UnicodeDecodeError, PermissionError) as e:
                    pass

only_in_new = set(font_new) - set(font_old)

print('readonly icons=[')
for name in font_new:
    print("'" + name + "',")
print(']')
# Print them
#for name in font_new:
#    print(name)
