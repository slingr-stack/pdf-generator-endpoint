/**
 * This flow step will send generic request.
 *
 * @param {object} inputs
 * {string} template, This is used to generate the PDF.
 * {object} data, This is used to generate the PDF.
 * {object} settings, This is used to generate the PDF.
 * {string} callbackData, This is used to send callback data.
 * {text} callbacks, This is used to send callbacks.
 */
step.generatePDFPdfGenerator = function (inputs) {
	var inputs = {
		template: inputs.template,
		data: inputs.data,
		settings: inputs.settings,
		callbackData: inputs.callbackData || "",
		callbacks: inputs.callbacks || "",
	};
	inputs.callbacks = inputs.callbacks ?
		eval("inputs.callbacks = {" + inputs.events + " : function(event, callbackData) {" + inputs.callbacks + "}}") :
		inputs.callbacks;

	inputs.callbackData = inputs.callbackData ? {record: inputs.callbackData} : inputs.callbackData;
	return app.endpoints.pdfGenerator.generatePdf(inputs.template, inputs.data, inputs.settings, inputs.callbackData, inputs.callbacks);
};