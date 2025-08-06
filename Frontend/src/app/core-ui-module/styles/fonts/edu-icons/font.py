import xml.etree.ElementTree as ET

tree = ET.parse("Edu-Icons.svg")
root = tree.getroot()
# Extract default namespace
ns = {'ns': root.tag.split("}")[0].strip("{")}
print(ns)
glyph_names = set()

# Find all glyph elements
for glyph in root.findall(".//ns:glyph" if ns else ".//glyph", namespaces=ns):
    name = glyph.attrib.get("glyph-name")
    if name:
        glyph_names.add(name)

# Format output
print("[")
for name in glyph_names:
    print(f"'{name}',")
print("]")
