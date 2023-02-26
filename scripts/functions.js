////////////////////////////////////////////////////////////////////////////
//                                                                        //
//             This file was generated with "slingr-helpgen"              //
//                                                                        //
//               For more info, check the following links:                //
//             https://www.npmjs.com/package/slingr-helpgen               //
//           https://github.com/slingr-stack/slingr-helpgen               //
//                                                                        //
////////////////////////////////////////////////////////////////////////////

endpoint.generatePdf = {};

endpoint.mergeDocuments = {};

endpoint.splitDocument = {};

endpoint.replaceHeaderAndFooter = {};

endpoint.fillForm = {};

endpoint.fillFormSync = {};

endpoint.replaceImages = {};

endpoint.addImages = {};

endpoint.convertPdfToImages = {};

endpoint.generatePdf = function(template, data, settings, callbackData, callbacks) {
    if (!settings || typeof settings != 'object') {
        settings = {};
    }
    if (!template || !data) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [template,data].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: generatePdf');
    var options = {template: template, data: data, settings: settings};
    return endpoint._generatePdf(options, callbackData, callbacks);
};

endpoint.mergeDocuments = function(documents, callbackData, callbacks) {
    if (!documents) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [documents].');
        return;
    }
    for (var i in documents) {
        var doc = documents[i];
        if (!doc.file || (doc.start && doc.end && parseInt(doc.start) > parseInt(doc.end))) {
            throw 'Invalid document settings for ' + JSON.stringify(doc);
        }
    }
    sys.logs.debug('[pdf-generator] from: mergeDocuments');
    var options = {documents: documents};
    return endpoint._mergeDocuments(options, callbackData, callbacks);
};

endpoint.splitDocument = function(fileId, interval, callbackData, callbacks) {
    if (!fileId || !interval) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId,interval].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: splitDocument');
    var options = {fileId: fileId, interval: interval};
    return endpoint._splitDocument(options, callbackData, callbacks);
};

endpoint.replaceHeaderAndFooter = function(fileId, settings, callbackData, callbacks) {
    if (!fileId || !settings) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId,settings].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: replaceHeaderAndFooter');
    var options = {fileId: fileId, settings: settings};
    return endpoint._replaceHeaderAndFooter(options, callbackData, callbacks);
};

endpoint.fillForm = function(fileId, settings, callbackData, callbacks) {
    if (!fileId) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: fillForm');
    var options = {fileId: fileId, settings: settings || {}};
    return endpoint._fillForm(options, callbackData, callbacks);
};

endpoint.fillFormSync = function(fileId, settings, callbackData, callbacks) {
    if (!fileId) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: fillFormSync');
    var options = {fileId: fileId, sync: true, settings: settings || {}};
    return endpoint._fillFormSync(options, callbackData, callbacks);
};

endpoint.replaceImages = function(fileId, settings, callbackData, callbacks) {
    if (!fileId) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: replaceImages');
    var options = {fileId: fileId, settings: settings || {}};
    return endpoint._replaceImages(options, callbackData, callbacks);
};

endpoint.addImages = function(fileId, settings, callbackData, callbacks) {
    if (!fileId) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileId].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: addImages');
    var options = {fileId: fileId, settings: settings || {}};
    return endpoint._addImages(options, callbackData, callbacks);
};

endpoint.convertPdfToImages = function(fileIds, dpi, settings, callbackData, callbacks) {
    if (!fileIds || !dpi) {
        sys.logs.error('Invalid argument received. This helper should receive the following parameters as non-empty strings: [fileIds,dpi].');
        return;
    }
    sys.logs.debug('[pdf-generator] from: convertPdfToImages');
    var options = {fileIds: fileIds, dpi: dpi, settings: settings || {}};
    return endpoint._convertPdfToImages(options, callbackData, callbacks);
};

////////////////////////////////////
// Public API - Generic Functions //
////////////////////////////////////

endpoint.get = function(url, httpOptions, callbackData, callbacks) {
    var options = checkHttpOptions(url, httpOptions);
    return endpoint._get(options, callbackData, callbacks);
};

endpoint.post = function(url, httpOptions, callbackData, callbacks) {
    options = checkHttpOptions(url, httpOptions);
    return endpoint._post(options, callbackData, callbacks);
};

endpoint.put = function(url, httpOptions, callbackData, callbacks) {
    options = checkHttpOptions(url, httpOptions);
    return endpoint._put(options, callbackData, callbacks);
};

endpoint.patch = function(url, httpOptions, callbackData, callbacks) {
    options = checkHttpOptions(url, httpOptions);
    return endpoint._patch(options, callbackData, callbacks);
};

endpoint.delete = function(url, httpOptions, callbackData, callbacks) {
    var options = checkHttpOptions(url, httpOptions);
    return endpoint._delete(options, callbackData, callbacks);
};

endpoint.head = function(url, httpOptions, callbackData, callbacks) {
    var options = checkHttpOptions(url, httpOptions);
    return endpoint._head(options, callbackData, callbacks);
};

endpoint.options = function(url, httpOptions, callbackData, callbacks) {
    var options = checkHttpOptions(url, httpOptions);
    return endpoint._options(options, callbackData, callbacks);
};

endpoint.utils = {};

endpoint.utils.parseTimestamp = function(dateString) {
    if (!dateString) {
        return null;
    }
    var dt = dateString.split(/[: T\-]/).map(parseFloat);
    return new Date(dt[0], dt[1] - 1, dt[2], dt[3] || 0, dt[4] || 0, dt[5] || 0, 0);
};

endpoint.utils.formatTimestamp = function(date) {
    if (!date) {
        return null;
    }
    var pad = function(number) {
        var r = String(number);
        if ( r.length === 1 ) {
            r = '0' + r;
        }
        return r;
    };
    return date.getUTCFullYear()
        + '-' + pad( date.getUTCMonth() + 1 )
        + '-' + pad( date.getUTCDate() )
        + 'T' + pad( date.getUTCHours() )
        + ':' + pad( date.getUTCMinutes() )
        + ':' + pad( date.getUTCSeconds() )
        + '.' + String( (date.getUTCMilliseconds()/1000).toFixed(3) ).slice( 2, 5 )
        + 'Z';
};

///////////////////////
//  Private helpers  //
///////////////////////

var checkHttpOptions = function (url, options) {
    options = options || {};
    if (!!url) {
        if (isObject(url)) {
            // take the 'url' parameter as the options
            options = url || {};
        } else {
            if (!!options.path || !!options.params || !!options.body) {
                // options contains the http package format
                options.path = url;
            } else {
                // create html package
                options = {
                    path: url,
                    body: options
                }
            }
        }
    }
    return options;
};

var isObject = function (obj) {
    return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);

var parse = function (str) {
    try {
        if (arguments.length > 1) {
            var args = arguments[1], i = 0;
            return str.replace(/(:(?:\w|-)+)/g, () => {
                if (typeof (args[i]) != 'string') throw new Error('Invalid type of argument: [' + args[i] + '] for url [' + str + '].');
                return args[i++];
            });
        } else {
            if (str) {
                return str;
            }
            throw new Error('No arguments nor url were received when calling the helper. Please check it\'s definition.');
        }
    } catch (err) {
        sys.logs.error('Some unexpected error happened during the parse of the url for this helper.')
        throw err;
    }
}