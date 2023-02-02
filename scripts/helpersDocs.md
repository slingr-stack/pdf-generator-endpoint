# Javascript API

The Javascript API of the pdf-generator endpoint has three pieces:

- **HTTP requests**: These allows to make regular HTTP requests.
- **Shortcuts**: These are helpers to make HTTP request to the API in a more convenient way.
- **Additional Helpers**: These helpers provide additional features that facilitate or improves the endpoint usage in SLINGR.

## HTTP requests
You can make `` requests to the [pdf-generator API](API_URL_HERE) like this:
```javascript
var response = app.endpoints.pdf-generator.mergeDocuments(documents)
```

Please take a look at the documentation of the [HTTP endpoint](https://github.com/slingr-stack/http-endpoint#javascript-api)
for more information about generic requests.

## Shortcuts

Instead of having to use the generic HTTP methods, you can (and should) make use of the helpers provided in the endpoint:
<details>
    <summary>Click here to see all the helpers</summary>

<br>

* FUNCTION: 'generatePdf'
```javascript
app.endpoints.pdf-generator.generatePdf(template, data, settings, callbackData, callbacks)
```
---
* FUNCTION: 'mergeDocuments'
```javascript
app.endpoints.pdf-generator.mergeDocuments(documents, callbackData, callbacks)
```
---
* FUNCTION: 'splitDocument'
```javascript
app.endpoints.pdf-generator.splitDocument(fileId, interval, callbackData, callbacks)
```
---
* FUNCTION: 'replaceHeaderAndFooter'
```javascript
app.endpoints.pdf-generator.replaceHeaderAndFooter(fileId, settings, callbackData, callbacks)
```
---
* FUNCTION: 'fillForm'
```javascript
app.endpoints.pdf-generator.fillForm(fileId, settings, callbackData, callbacks)
```
---
* FUNCTION: 'fillFormSync'
```javascript
app.endpoints.pdf-generator.fillFormSync(fileId, settings, callbackData, callbacks)
```
---
* FUNCTION: 'replaceImages'
```javascript
app.endpoints.pdf-generator.replaceImages(fileId, settings, callbackData, callbacks)
```
---
* FUNCTION: 'addImages'
```javascript
app.endpoints.pdf-generator.addImages(fileId, settings, callbackData, callbacks)
```
---
* FUNCTION: 'convertPdfToImages'
```javascript
app.endpoints.pdf-generator.convertPdfToImages(fileIds, dpi, settings, callbackData, callbacks)
```
---

</details>
    
## Flow Step

As an alternative option to using scripts, you can make use of Flows and Flow Steps specifically created for the endpoint: 
<details>
    <summary>Click here to see the Flow Steps</summary>

<br>



### Generic Flow Step

Generic flow step for full use of the entire endpoint and its services.

<h3>Inputs</h3>

<table>
    <thead>
    <tr>
        <th>Label</th>
        <th>Type</th>
        <th>Required</th>
        <th>Default</th>
        <th>Visility</th>
        <th>Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>URL</td>
        <td>choice</td>
        <td>yes</td>
        <td> - </td>
        <td>Always</td>
        <td>
            This is the http method to be used against the endpoint. <br>
            Possible values are: <br>
            <i><strong></strong></i>
        </td>
    </tr>
    <tr>
        <td>Path</td>
        <td>choice</td>
        <td>yes</td>
        <td> - </td>
        <td>Always</td>
        <td>
            The url to which this endpoint will send the request. This is the exact service to which the http request will be made. <br>
            Possible values are: <br>
            <i><strong>/generatePdf/{template}/{data}/{settings}<br>/mergeDocuments/{documents}<br>/splitDocument/{fileId}/{interval}<br>/replaceHeaderAndFooter/{fileId}/{settings}<br>/fillForm/{fileId}/{settings}<br>/fillFormSync/{fileId}/{settings}<br>/replaceImages/{fileId}/{settings}<br>/addImages/{fileId}/{settings}<br>/convertPdfToImages/{fileIds}/{dpi}/{settings}<br></strong></i>
        </td>
    </tr>
    <tr>
        <td>Headers</td>
        <td>text</td>
        <td>no</td>
        <td> - </td>
        <td>Always</td>
        <td>
            Used when you want to have a custom http header for the query.
        </td>
    </tr>
    <tr>
        <td>Params</td>
        <td>text</td>
        <td>no</td>
        <td> - </td>
        <td>Always</td>
        <td>
            Used when you want to have a custom query params for the http call.
        </td>
    </tr>
    <tr>
        <td>Event</td>
        <td>dropDown</td>
        <td>no</td>
        <td> - </td>
        <td>Always</td>
        <td>
            Used to define event after the call.
        </td>
    </tr>
    <tr>
        <td>Callback data</td>
        <td>textarea</td>
        <td>no</td>
        <td> - </td>
        <td> Event is Callback </td>
        <td>
            This is an object you can send that you will get back when the function is processed.
        </td>
    </tr>
    <tr>
        <td>Callbacks</td>
        <td>Script</td>
        <td>no</td>
        <td> - </td>
        <td> Event is Callback </td>
        <td>
            This is a map where you can listen for different function
        </td>
    </tr>
    <tr>
        <td>Override Settings</td>
        <td>boolean</td>
        <td>no</td>
        <td> false </td>
        <td>Always</td>
        <td></td>
    </tr>
    <tr>
        <td>Follow Redirect</td>
        <td>boolean</td>
        <td>no</td>
        <td> false </td>
        <td> overrideSettings </td>
        <td>Indicates that the resource has to be downloaded into a file instead of returning it in the response.</td>
    </tr>
    <tr>
        <td>Download</td>
        <td>boolean</td>
        <td>no</td>
        <td> false </td>
        <td> overrideSettings </td>
        <td>If true the method won't return until the file has been downloaded and it will return all the information of the file.</td>
    </tr>
    <tr>
        <td>File name</td>
        <td>text</td>
        <td>no</td>
        <td></td>
        <td> overrideSettings </td>
        <td>If provided, the file will be stored with this name. If empty the file name will be calculated from the URL.</td>
    </tr>
    <tr>
        <td>Full response</td>
        <td> boolean </td>
        <td>no</td>
        <td> false </td>
        <td> overrideSettings </td>
        <td>Include extended information about response</td>
    </tr>
    <tr>
        <td>Conection Timeout</td>
        <td> number </td>
        <td>no</td>
        <td> 5000 </td>
        <td> overrideSettings </td>
        <td>Connect timeout interval, in milliseconds (0 = infinity).</td>
    </tr>
    <tr>
        <td>Read Timeout</td>
        <td> number </td>
        <td>no</td>
        <td> 60000 </td>
        <td> overrideSettings </td>
        <td>Read timeout interval, in milliseconds (0 = infinity).</td>
    </tr>
    </tbody>
</table>

<h3>Outputs</h3>

<table>
    <thead>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>response</td>
        <td>object</td>
        <td>
            Object resulting from the response to the endpoint call.
        </td>
    </tr>
    </tbody>
</table>


</details>

For more information about how shortcuts or flow steps works, and how they are generated, take a look at the [slingr-helpgen tool](https://github.com/slingr-stack/slingr-helpgen).

## Additional Helpers
*MANUALLY ADD THE DOCUMENTATION OF THESE HELPERS HERE...*