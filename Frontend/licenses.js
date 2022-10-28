var checker = require('license-checker');
var fs = require('fs');

checker.init({
    start: './',
    production: true,
    direct: true,
}, function(err, packages) {
    if (err) {
        //Handle error
    } else {
        console.log(packages);
        var keys=Object.keys(packages);
        var result='Lists of ' + keys.length + ' third-party dependencies.\n';
        console.log(keys);
        for(var i=0;i<keys.length;i++){
            const pack = packages[keys[i]];
            let name = keys[i].split('@');
            delete name[name.length - 1];
            name = name.join('@');
            name = name.substring(0, name.length - 1);
            result += '     (' + pack.licenses + ') ' + name + ' (' + keys[i];
            if(pack.url) {
                result += ' - ' + pack.url;
            }
            result += ')\n';
        }
        fs.mkdirSync('dist/WEB-INF/licenses', {recursive: true});
        fs.writeFileSync('dist/WEB-INF/licenses/THIRD-PARTY-edu_sharing-community-repository-frontend.txt',result);
        console.log(result);
    }
});
