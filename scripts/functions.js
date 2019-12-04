/////////////////////
// Public API
/////////////////////

endpoint.generatePdf = function (template, data, settings, callbackData, callbacks) {
    if (!settings || typeof settings != 'object') {
        settings = {};
    }
    var options = {
        template: template,
        data: data,
        settings: settings
    };
    return endpoint._generatePdf(options, callbackData, callbacks);
};

endpoint.mergeDocuments = function (documents, callbackData, callbacks) {
    for (var i in documents) {
        var doc = documents[i];
        if (!doc.file || (doc.start && doc.end && parseInt(doc.start) > parseInt(doc.end))) {
            throw 'Invalid document settings for ' + JSON.stringify(doc);
        }
    }
    var options = {
        documents: documents
    };
    return endpoint._mergeDocuments(options, callbackData, callbacks);
};


endpoint.splitDocument = function (fileId, interval, callbackData, callbacks) {
    var options = {
        fileId: fileId,
        interval: interval
    };
    return endpoint._splitDocument(options, callbackData, callbacks);
};

endpoint.replaceHeaderAndFooter = function (fileId, settings, callbackData, callbacks) {
    var options = {
        fileId: fileId,
        settings: settings
    };
    return endpoint._replaceHeaderAndFooter(options, callbackData, callbacks);
};

endpoint.fillForm = function(fileId, settings, callbackData, callbacks) {

    var options = {
        fileId: fileId,
        settings: settings || {}
    };
    return endpoint._fillForm(options, callbackData, callbacks);
};

endpoint.replaceImages = function(fileId, settings, callbackData, callbacks) {

    var options = {
        fileId: fileId,
        settings: settings || {}
    };
    return endpoint._replaceImages(options, callbackData, callbacks);
};


endpoint.addImages = function(fileId, settings, callbackData, callbacks) {

    var options = {
        fileId: fileId,
        settings: settings || {}
    };
    return endpoint._addImages(options, callbackData, callbacks);
};