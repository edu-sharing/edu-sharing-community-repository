var checker = require('license-checker');
var fs = require('fs');
var htmlencode = require('htmlencode');

checker.init({
    start: './',
    production: true,
    direct: true,
}, function(err, packages) {
    if (err) {
        //Handle error
    } else {
        console.log(packages);
        var result="";
        var keys=Object.keys(packages);
        console.log(keys);
        for(var i=0;i<keys.length;i++){
            var package=packages[keys[i]];
            result+="<h3>"+keys[i]+"</h3>";
            result+="<h4>License: "+package.licenses+"</h4>";
            console.log(package.licenseFile);
            if(package.licenseFile) {
                var text = fs.readFileSync(package.licenseFile);
                if(text) {
                    result += "<pre>" + htmlencode.htmlEncode(''+text) + "</pre>";
                }
            }
        }
        fs.writeFileSync("licenses.html",result);
        console.log(result);
        //The sorted package data
        //as an Object
    }
});