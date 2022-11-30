var fs = require("fs");
var path = require("path");
var util = require("../utilities");


module.exports = function(context){

    console.log("Started change cordova build js!");
    var buildPath = path.join("platforms","ios","cordova","lib","build.js");
    var build = fs.readFileSync(buildPath, "utf8");

    var regex = /(var Q[\s|\S]*'archive'.*\n.*\n)(.*\n)([\s|\S]*};)/gm;
    
    build = build.replace(regex,util.replacer);

    fs.writeFileSync(buildPath, build);
    console.log("Ended change cordova build js!");
};
