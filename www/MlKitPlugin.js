var exec = require('cordova/exec');

exports.getText = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'getText', [options]);
};
exports.getLabel = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'getLabel', [options]);
};
exports.getFace = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'getFace', [options]);
};
exports.identifyLang = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'identifyLang', [options]);
};


exports.reply = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'reply', [options]);
};

exports.addMessage = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'addMessage', [options]);
};

exports.removeMessage = function (success, error,options) {

    exec(success, error, 'MlKitPlugin', 'removeMessage', [options]);
};