const fs = require('fs-extra');
const { glob } = require('glob');
const path = require('path');
const JavaScriptObfuscator = require('javascript-obfuscator');

// Directories
const inputDir = './src/main/resources/static/js';
const outputDir = './target/classes/static/js';

// Obfuscation options
const obfuscationOptions = {
    compact: true,
    controlFlowFlattening: true,
    controlFlowFlatteningThreshold: 0.75,
    deadCodeInjection: true,
    deadCodeInjectionThreshold: 0.4,
    stringArray: true,
    stringArrayEncoding: ['base64'],
    stringArrayThreshold: 0.75,
    transformObjectKeys: true,
    identifierNamesGenerator: 'hexadecimal',
    unicodeEscapeSequence: true
};

// Process each .js file
(async () => {
    try {
        console.log('Glob module:', glob);
        const files = await glob(`${inputDir}/**/*.js`);
        for (const file of files) {
            // Skip files already obfuscated
            if (file.includes('.obfuscated.')) continue;

            console.log(`Processing ${file}`);
            const jsContent = fs.readFileSync(file, 'utf8');

            // Obfuscate the JavaScript
            const obfuscatedResult = JavaScriptObfuscator.obfuscate(jsContent, obfuscationOptions);
            const obfuscatedJs = obfuscatedResult.getObfuscatedCode();

            // Write the obfuscated file
            const relativePath = path.relative(inputDir, file);
            const outputFile = path.join(outputDir, relativePath.replace('.js', '.obfuscated.js'));
            fs.ensureDirSync(path.dirname(outputFile));
            fs.writeFileSync(outputFile, obfuscatedJs, 'utf8');
            console.log(`Written obfuscated file to ${outputFile}`);
        }
        console.log('Obfuscation complete.');
    } catch (err) {
        console.error('Error processing files:', err);
        process.exit(1);
    }
})();