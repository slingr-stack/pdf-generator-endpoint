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
	var inputsLogic = {
		template: inputs.template,
		data: inputs.data,
		settings: inputs.settings
	};

	return app.endpoints.pdfGenerator.generatePdf(inputsLogic.template, inputsLogic.data, inputsLogic.settings);
};