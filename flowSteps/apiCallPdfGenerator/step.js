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
 * @param {object} stepConfig.output {object} output
 */
step.apiCallPdfGenerator = function (stepConfig) {

	var inputs = {
		headers: stepConfig.inputs.headers || [],
		params: stepConfig.inputs.params || [],
		body: stepConfig.inputs.body || {},
		callbackData: stepConfig.inputs.callbackData || "",
		callbacks: stepConfig.inputs.callbacks || "",
		followRedirects: stepConfig.inputs.followRedirects || false,
		download: stepConfig.inputs.download || false,
		fileName: stepConfig.inputs.fileName || "",
		fullResponse: stepConfig.inputs.fullResponse || false,
		connectionTimeout: stepConfig.inputs.connectionTimeout || 5000,
		readTimeout: stepConfig.inputs.readTimeout || 60000,
		events: stepConfig.inputs.events || "",
		url: stepConfig.inputs.url || {
			urlValue: "",
			paramsValue: [],
			method: ""
		},
		action: stepConfig.inputs.action || ""
	};

	inputs.headers = isObject(inputs.headers) ? inputs.headers : stringToObject(inputs.headers);
	inputs.params = isObject(inputs.params) ? inputs.params : stringToObject(inputs.params);
	inputs.body = isObject(inputs.body) ? inputs.body : JSON.parse(inputs.body);

	inputs.callbacks = inputs.callbacks ?
		eval("inputs.callbacks = {" + inputs.events + " : function(event, callbackData) {" + inputs.callbacks + "}}") :
		inputs.callbacks;

	inputs.callbackData = inputs.callbackData ? {record: inputs.callbackData} : inputs.callbackData;

	var options = {
		path: parse(inputs.url.urlValue, inputs.url.paramsValue),
		params: inputs.params,
		headers: inputs.headers,
		body: inputs.body,
		followRedirects : inputs.followRedirects,
		forceDownload : inputs.events === "fileDownloaded" ? true : inputs.download,
		downloadSync : inputs.events === "fileDownloaded" ? false : inputs.download,
		fileName: inputs.fileName,
		fullResponse : inputs.fullResponse,
		connectionTimeout: inputs.connectionTimeout,
		readTimeout: inputs.readTimeout,
		defaultCallback: !!inputs.events
	};

	switch (inputs.url.method.toLowerCase()) {
		case 'get':
			return endpoint._get(options, inputs.callbackData, inputs.callbacks);
		case 'post':
			return endpoint._post(options, inputs.callbackData, inputs.callbacks);
		case 'delete':
			return endpoint._delete(options, inputs.callbackData, inputs.callbacks);
		case 'put':
			return endpoint._put(options, inputs.callbackData, inputs.callbacks);
		case 'connect':
			return endpoint._connect(options, inputs.callbackData, inputs.callbacks);
		case 'head':
			return endpoint._head(options, inputs.callbackData, inputs.callbacks);
		case 'options':
			return endpoint._options(options, inputs.callbackData, inputs.callbacks);
		case 'patch':
			return endpoint._patch(options, inputs.callbackData, inputs.callbacks);
		case 'trace':
			return endpoint._trace(options, inputs.callbackData, inputs.callbacks);
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