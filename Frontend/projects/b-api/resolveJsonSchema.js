const { execSync } = require('child_process');

const file = process.argv[2];
const output = execSync(`npx redocly bundle ${file} --ext json`, {
    maxBuffer: 1024 * 1024 * 200,
});
process.stdout.write(output);
