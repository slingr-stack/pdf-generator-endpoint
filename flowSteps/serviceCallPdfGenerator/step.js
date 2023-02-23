/**
 * This flow step will send generic request.
 *
 * @param {object} inputs
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
 */
step.serviceCallPdfGenerator = function (inputs) {

	var inputsLogic = {
		headers: inputs.headers || [],
		params: inputs.params || [],
		body: inputs.body || {},
		download: inputs.download || false,
		fileName: inputs.fileName || "",
		fullResponse: inputs.fullResponse || false,
		connectionTimeout: inputs.connectionTimeout || 5000,
		readTimeout: inputs.readTimeout || 60000,
		url: {
			urlValue: inputs.url.urlValue ? inputs.url.urlValue.split(" ")[1] : "",
			paramsValue: inputs.url.paramsValue || []
		},
		method: inputs.url.urlValue ? inputs.url.urlValue.split(" ")[0] : ""
	};

	inputsLogic.headers = isObject(inputsLogic.headers) ? inputsLogic.headers : stringToObject(inputsLogic.headers);
	inputsLogic.params = isObject(inputsLogic.params) ? inputsLogic.params : stringToObject(inputsLogic.params);
	inputsLogic.body = isObject(inputsLogic.body) ? inputsLogic.body : JSON.parse(inputsLogic.body);


	var options = {
		path: parse(inputsLogic.url.urlValue, inputsLogic.url.paramsValue),
		params: inputsLogic.params,
		headers: inputsLogic.headers,
		body: inputsLogic.body,
		followRedirects : inputsLogic.followRedirects,
		forceDownload :inputsLogic.download,
		downloadSync : false,
		fileName: inputsLogic.fileName,
		fullResponse : inputsLogic.fullResponse,
		connectionTimeout: inputsLogic.connectionTimeout,
		readTimeout: inputsLogic.readTimeout
	}

	switch (inputs.method.toLowerCase()) {
		case 'get':
			return endpoint._get(options);
		case 'post':
			return endpoint._post(options);
		case 'delete':
			return endpoint._delete(options);
		case 'put':
			return endpoint._put(options);
		case 'connect':
			return endpoint._connect(options);
		case 'head':
			return endpoint._head(options);
		case 'options':
			return endpoint._options(options);
		case 'patch':
			return endpoint._patch(options);
		case 'trace':
			return endpoint._trace(options);
	}

	switch (inputs.action) {
		case "app.endpoints.pdf-generator.generatePdf(template, data, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.generatePdf(inputs.params['template'], inputs.params['data'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.mergeDocuments(documents, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.mergeDocuments(inputs.params['documents'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.splitDocument(fileId, interval, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.splitDocument(inputs.params['fileId'], inputs.params['interval'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.replaceHeaderAndFooter(fileId, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.replaceHeaderAndFooter(inputs.params['fileId'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.fillForm(fileId, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.fillForm(inputs.params['fileId'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.fillFormSync(fileId, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.fillFormSync(inputs.params['fileId'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.replaceImages(fileId, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.replaceImages(inputs.params['fileId'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.addImages(fileId, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.addImages(inputs.params['fileId'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
		case "app.endpoints.pdf-generator.convertPdfToImages(fileIds, dpi, settings, callbackData, callbacks)":
			return app.endpoints.pdfGenerator.convertPdfToImages(inputs.params['fileIds'], inputs.params['dpi'], inputs.params['settings'], inputs.callbackData, inputs.callbacks);
	}

	return null;
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