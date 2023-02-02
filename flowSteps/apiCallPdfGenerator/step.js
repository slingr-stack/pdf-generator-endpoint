/**
 * This flow step will send generic request.
 *
 * @param {object} stepConfig.inputs
 * {text} method, This is used to config method.
 * {text} url, This is used to config external URL.
 * {Array[string]} pathVariables, This is used to config path variables.
 * {Array[string]} headers, This is used to config headers.
 * {Array[string]} params, This is used to config params.
 * {string} body, This is used to send body request.
 * {string} callbackData, This is used to send callback data.
 * {text} callbacks, This is used to send callbacks.
 * {boolean} followRedirects, This is used to config follow redirects.
 * {boolean} download, This is used to config download.
 * {boolean} fullResponse, This is used to config full response.
 * {number} connectionTimeout, Read timeout interval, in milliseconds.
 * {number} readTimeout, Connect timeout interval, in milliseconds.
 * @param {object} stepConfig.context {object} context
 */
step.apiCallPdfGenerator = function (stepConfig) {

	var headers = isObject(stepConfig.inputs.headers) ? stepConfig.inputs.headers : stringToObject(stepConfig.inputs.headers)
	var params = isObject(stepConfig.inputs.params) ? stepConfig.inputs.params : stringToObject(stepConfig.inputs.params)
	var body = isObject(stepConfig.inputs.body) ? stepConfig.inputs.body : JSON.parse(stepConfig.inputs.body);

	stepConfig.inputs.callbacks = stepConfig.inputs.callbacks ?
		eval("stepConfig.inputs.callbacks = {" + stepConfig.inputs.events + " : function(event, callbackData) {" + stepConfig.inputs.callbacks + "}}") : stepConfig.inputs.callbacks;

	stepConfig.inputs.callbackData = stepConfig.inputs.callbackData ? {record:stepConfig.inputs.callbackData} : stepConfig.inputs.callbackData;
sys.logs.error(stepConfig);
	var options = {
		path: parse(stepConfig.inputs.url.urlValue, stepConfig.inputs.url.paramsValue),
		params:params,
		headers:headers,
		body: body,
		followRedirects : stepConfig.inputs.followRedirects,
		forceDownload : stepConfig.inputs.events === "fileDownloaded" ? true : stepConfig.inputs.download,
		downloadSync : stepConfig.inputs.events === "fileDownloaded" ? false : stepConfig.inputs.download,
		fileName: stepConfig.inputs.fileName,
		fullResponse : stepConfig.inputs.fullResponse,
		connectionTimeout: stepConfig.inputs.connectionTimeout,
		readTimeout: stepConfig.inputs.readTimeout,
		defaultCallback: !!stepConfig.inputs.events
	}

	switch (stepConfig.inputs.url.method.toLowerCase()) {
		case 'get':
			return endpoint._get(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'post':
			return endpoint._post(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'delete':
			return endpoint._delete(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'put':
			return endpoint._put(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'connect':
			return endpoint._connect(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'head':
			return endpoint._head(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'options':
			return endpoint._options(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'patch':
			return endpoint._patch(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		case 'trace':
			return endpoint._trace(options, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
		default:
			return null;
	}

	switch (stepConfig.inputs.action) {
        case "app.endpoints.pdf-generator.generatePdf(template, data, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.generatePdf(template, data, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.mergeDocuments(documents, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.mergeDocuments(documents, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.splitDocument(fileId, interval, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.splitDocument(fileId, interval, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.replaceHeaderAndFooter(fileId, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.replaceHeaderAndFooter(fileId, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.fillForm(fileId, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.fillForm(fileId, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.fillFormSync(fileId, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.fillFormSync(fileId, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.replaceImages(fileId, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.replaceImages(fileId, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.addImages(fileId, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.addImages(fileId, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        case "app.endpoints.pdf-generator.convertPdfToImages(fileIds, dpi, settings, callbackData, callbacks)":
            return app.endpoints.pdfGenerator.convertPdfToImages(fileIds, dpi, settings, stepConfig.inputs.callbackData, stepConfig.inputs.callbacks);
        default:
            return null;
    }

};

var parse = function (url, pathVariables){

	var regex = /{([^}]*)}/g;

	if (!url.match(regex)){
		return url;
	}

	if(!pathVariables){
		sys.logs.error('No path variables have been received and the url contains curly brackets\'{}\'');
		throw new Error('Error please contact support.');
	}

	url = url.replace(regex, function(m, i) {
		return pathVariables[i] ? pathVariables[i] : m;
	})

	return url;
}
var isObject = function (obj) {
	return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);

var stringToObject = function (obj) {
	if (!!obj){
		var keyValue = obj.toString().split(',');
		var parseObj = {};
		for(var i = 0; i < keyValue.length; i++) {
			parseObj[keyValue[i].split('=')[0]] = keyValue[i].split('=')[1]
		}
		return parseObj;
	}
	return null;
};