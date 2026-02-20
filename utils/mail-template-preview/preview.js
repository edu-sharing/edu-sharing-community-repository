const fs = require("fs");
const path = require("path");
const { XMLParser } = require("fast-xml-parser");

// ---------------- CONFIG ----------------

const basePath = path.join(__dirname, "../../config/defaults/src/main/resources/mailtemplates/templates.xml");
const basePathDe = path.join(__dirname, "../../config/defaults/src/main/resources/mailtemplates/templates_de_DE.xml");
const overridePathDe = path.join(__dirname, "../../../deploy/repository/config/defaults/src/main/resources/mailtemplates/templates_de_DE_override.xml");

// Root folder to resolve image paths
const imageRoot = __dirname + "../../../Backend/services/webapp/src/main/webapp";

const templateName = process.argv[2];

if (!templateName) {
    console.error("Usage: node preview-template.js <templateName>");
    process.exit(1);
}

// ---------------- XML PARSER ----------------

const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "",
    cdataPropName: "cdata",
    trimValues: false
});

// ---------------- HELPERS ----------------

function loadTemplates(file) {
    if (!fs.existsSync(file)) {
        console.info(`Skipping ${file}: missing`);
        return [];
    }

    try {
        const xml = fs.readFileSync(file, "utf-8");
        const parsed = parser.parse(xml);

        if (!parsed.templates?.template) return [];

        let t = parsed.templates.template;
        if (!Array.isArray(t)) t = [t];

        return t;

    } catch (e) {
        console.warn(`Skipping ${file}: ${e.message}`);
        return [];
    }
}

function mergeTemplates(base, override) {
    const map = new Map();

    base.forEach(t => map.set(t.name, t));
    override.forEach(t => map.set(t.name, t));

    return Array.from(map.values());
}

function findTemplate(list, name) {
    return list.find(t => t.name === name);
}

function extractCdata(node) {
    if (!node) return "";

    if (typeof node === "string") return node;

    if (node.cdata !== undefined) return node.cdata;

    return "";
}

// ---------------- IMAGE RESOLUTION ----------------

function getMime(file) {
    const ext = path.extname(file).toLowerCase();
    switch (ext) {
        case ".png": return "image/png";
        case ".jpg":
        case ".jpeg": return "image/jpeg";
        case ".gif": return "image/gif";
        case ".svg": return "image/svg+xml";
        case ".webp": return "image/webp";
        default: return "application/octet-stream";
    }
}

function resolveImages(html) {
    return html.replace(/{{image:([^}]+)}}/g, (_, imgPath) => {
        try {
            const clean = imgPath.trim();
            const full = path.join(imageRoot, clean);

            if (!fs.existsSync(full)) {
                console.warn("Image not found:", full);
                return "";
            }

            const buffer = fs.readFileSync(full);
            const mime = getMime(full);
            const base64 = buffer.toString("base64");

            return `data:${mime};base64,${base64}`;

        } catch (e) {
            console.warn("Image resolve error:", imgPath, e.message);
            return "";
        }
    });
}

// ---------------- LOAD + MERGE ----------------

const baseTemplates = mergeTemplates(
    loadTemplates(basePath),
    loadTemplates(basePathDe)
);
const overrideTemplates = loadTemplates(overridePathDe);
const templates = mergeTemplates(baseTemplates, overrideTemplates);

// ---------------- RESOLVE ----------------

const header = findTemplate(templates, "header");
const footer = findTemplate(templates, "footer");
const stylesheet = findTemplate(templates, "stylesheet");
const main = findTemplate(templates, templateName);

if (!main) {
    console.error(`Template "${templateName}" not found`);
    process.exit(1);
}

// ---------------- BUILD HTML ----------------

let bodyHtml =
    extractCdata(header?.message) +
    extractCdata(main?.message);

bodyHtml = resolveImages(bodyHtml);

const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
${extractCdata(stylesheet?.style)}
</style>
</head>
<body>

<div class="content">
${bodyHtml}
</div>

<div class="footer">
${resolveImages(extractCdata(footer?.message))}
</div>

</body>
</html>`;

// ---------------- WRITE OUTPUT ----------------

const out = path.join(__dirname, `preview-${templateName}.html`);
fs.writeFileSync(out, html, "utf-8");

console.log(`Preview written: ${out}`);