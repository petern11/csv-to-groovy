"use client"

import React, { useState } from 'react';
import Papa from 'papaparse';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Upload } from 'lucide-react';

const CsvMapper = () => {
  // Initialize all state variables at the top
  const defaultBaseUrl = 'https://www.campus.sanofi/dk';
  const defaultTemplate = 'magnolia-ai-sanofi-lm:pages/vaxDetail';
  const defaultDamPath = '/migration-images';
  const defaultParentPath = '/';

  const [csvData, setCsvData] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [mappings, setMappings] = useState({});
  const [groovyScript, setGroovyScript] = useState('');
  const [error, setError] = useState('');
  const [parentPath, setParentPath] = useState(defaultParentPath);
  const [templateName, setTemplateName] = useState(defaultTemplate);
  const [damPath, setDamPath] = useState(defaultDamPath);
  const [baseUrl, setBaseUrl] = useState(defaultBaseUrl);

  const cmsFields = [
    'title',
    'abstract',
    'tags',
    'textContent'
  ];

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    try {
      const text = await file.text();
      Papa.parse(text, {
        header: true,
        skipEmptyLines: true,
        complete: (results) => {
          if (results.errors.length > 0) {
            setError('Error parsing CSV: ' + results.errors[0].message);
            return;
          }
          setCsvData(results.data);
          setCsvHeaders(results.meta.fields);
          
          const initialMappings = {};
          results.meta.fields.forEach(header => {
            initialMappings[header] = '';
          });
          setMappings(initialMappings);
        },
        error: (error) => {
          setError('Error parsing CSV: ' + error.message);
        }
      });
    } catch (error) {
      setError('Error reading file: ' + error.message);
    }
  };

  const handleMappingChange = (csvHeader, cmsField) => {
    setMappings(prev => ({
      ...prev,
      [csvHeader]: cmsField
    }));
  };

  const formatGroovyMap = (obj) => {
    const entries = Object.entries(obj)
      .map(([key, value]) => `${key}: ${value}`)
      .join(',\n            ');
    return `[${entries}]`;
  };

  const generateGroovyScript = () => {
    if (!csvData) return;

    const formatValue = (value) => {
      if (typeof value === 'string') {
        return `"""${value
          .replace(/"/g, '\\"')
          .replace(/\$/g, '\\$')}"""`
      }
      return value;
    };

    // Store current state values in local variables
    const currentBaseUrl = baseUrl;
    const currentParentPath = parentPath;
    const currentDamPath = damPath;
    const currentTemplate = templateName;

    const groovyScriptContent = `import info.magnolia.context.MgnlContext
import info.magnolia.jcr.util.NodeUtil
import javax.jcr.Node
import javax.jcr.Binary
import java.net.URL
import java.net.URLEncoder
import org.apache.commons.io.IOUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.jcr.nodetype.NodeType

// Define paths and configuration
def parentPath = "${currentParentPath.replace(/\/$/, '')}"
def damPath = "${currentDamPath.replace(/\/$/, '')}"
def sourceBaseUrl = "${currentBaseUrl}"  // Base URL for image downloads

// Get workspaces
def ctx = MgnlContext.getInstance()
def workspace = ctx.getJCRSession('website')
def damWorkspace = ctx.getJCRSession('dam')
def rootNode = workspace.getRootNode()
def damRootNode = damWorkspace.getRootNode()

// Create DAM folder if it doesn't exist
def createDamFolder = { path ->
    def folder = NodeUtil.createPath(damRootNode, path, NodeType.NT_FOLDER)
    return folder
}

// Helper function to download and create image in DAM
def processImage = { imageSrc, targetFolder ->
    try {
        // Handle both relative and absolute URLs
        def imageUrl = imageSrc.startsWith('http') ? imageSrc : "\${sourceBaseUrl}\${imageSrc.startsWith('/') ? '' : '/'}\${imageSrc}"
        
        // Create a safe filename from the URL, handling Magnolia paths
        def fileName = imageSrc.contains('/dam/') ? 
            imageSrc.split('/dam/')[1].split('/jcr:content')[0].tokenize('/')[-1] :
            imageSrc.tokenize('/')[-1]
            
        fileName = URLEncoder.encode(fileName, 'UTF-8')
            .replace('+', '-')
            .replace('%', '-')
            .take(50) // Limit filename length

        // Create unique filename if needed
        def uniqueFileName = fileName
        def counter = 1
        while (damWorkspace.itemExists(targetFolder.path + '/' + uniqueFileName)) {
            def nameWithoutExt = fileName.contains('.') ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName
            def extension = fileName.contains('.') ? fileName.substring(fileName.lastIndexOf('.')) : ''
            uniqueFileName = nameWithoutExt + "-\${counter}" + extension
            counter++
        }

        // Download image
        def connection = new URL(imageUrl).openConnection()
        connection.setRequestProperty('User-Agent', 'Mozilla/5.0')
        def stream = connection.inputStream
        def bytes = IOUtils.toByteArray(stream)
        stream.close()

        // Create image node
        def imageNode = targetFolder.addNode(uniqueFileName, 'mgnl:asset')
        def contentNode = imageNode.addNode('jcr:content', 'mgnl:resource')
        
        // Set binary data
        Binary binary = damWorkspace.valueFactory.createBinary(new ByteArrayInputStream(bytes))
        contentNode.setProperty('jcr:data', binary)
        
        // Set metadata
        contentNode.setProperty('jcr:mimeType', connection.contentType)
        contentNode.setProperty('size', bytes.length)
        
        damWorkspace.save()
        return imageNode.path
        
    } catch (Exception e) {
        println "Failed to process image: \${imageSrc}"
        println "Error: \${e.message}"
        return null
    }
}

// Helper function to process HTML content and update image sources
def processHtmlContent = { content, damFolder ->
    if (!content) return content
    
    def doc = Jsoup.parse(content)
    doc.select('img').each { img ->
        def src = img.attr('src')
        def dataSrc = img.attr('data-src')
        
        // Prefer data-src if available, otherwise use src
        def imagePath = dataSrc ?: src
        
        if (imagePath) {
            def newPath = processImage(imagePath, damFolder)
            if (newPath) {
                img.attr('src', newPath)
                img.removeAttr('data-src') // Clean up data-src
            }
        }
    }
    return doc.body().html()
}

// Create DAM folder for this migration
def damFolder = createDamFolder(damPath)

// Define the data to process
def pagesData = [
${csvData.map(row => {
  const mappedFields = Object.entries(mappings)
    .filter(([_, cmsField]) => cmsField)
    .reduce((acc, [csvHeader, cmsField]) => {
      acc[cmsField] = formatValue(row[csvHeader]);
      return acc;
    }, {});
  
  return `    [
        pageName: "${row[Object.keys(mappings)[0]].toLowerCase().replace(/[^a-z0-9]/g, '-')}",
        properties: ${formatGroovyMap(mappedFields)}
    ]`;
}).join(',\n')}
]

// Process each page
pagesData.each { pageData ->
    // Create page node
    def page = NodeUtil.createPath(rootNode, parentPath + "/" + pageData.pageName, "mgnl:page")
    page.setProperty("mgnl:template", "${currentTemplate}")

    // Set page properties
    pageData.properties.each { field, value ->
        if (field == 'tags' && value) {
            def tagList = value.split(',')
            page.setProperty(field, tagList as String[])
        } else if (field == 'textContent' || field == 'abstract') {
            // Process HTML content for these fields
            def processedContent = processHtmlContent(value, damFolder)
            page.setProperty(field, processedContent)
        } else {
            page.setProperty(field, value)
        }
    }

    // Get the unwrapped node
    def pageNode = NodeUtil.unwrap(page)
}

// Save all changes
workspace.save()
damWorkspace.save()`;

    setGroovyScript(groovyScriptContent);
  };

  return (
    <div className="w-full max-w-4xl mx-auto p-4 space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>CSV to CMS Field Mapper with Image Migration</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Source URL Input */}
            <div className="space-y-2">
              <label className="block text-sm font-medium">Source Magnolia URL</label>
              <input
                type="text"
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="https://source-magnolia.example.com"
              />
            </div>

            {/* Parent Path Input */}
            <div className="space-y-2">
              <label className="block text-sm font-medium">Parent Path</label>
              <input
                type="text"
                value={parentPath}
                onChange={(e) => setParentPath(e.target.value)}
                className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="/path/to/parent"
              />
            </div>

            {/* DAM Path Input */}
            <div className="space-y-2">
              <label className="block text-sm font-medium">DAM Path for Images</label>
              <input
                type="text"
                value={damPath}
                onChange={(e) => setDamPath(e.target.value)}
                className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="/migration-images"
              />
            </div>

            {/* Template Name Input */}
            <div className="space-y-2">
              <label className="block text-sm font-medium">Template Name</label>
              <input
                type="text"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="standard-vaxiplace-page"
              />
            </div>

            {/* File Upload */}
            <div className="border-2 border-dashed rounded-lg p-6 text-center">
              <label className="cursor-pointer">
                <input
                  type="file"
                  accept=".csv"
                  onChange={handleFileUpload}
                  className="hidden"
                />
                <div className="flex flex-col items-center">
                  <Upload className="h-12 w-12 text-gray-400" />
                  <span className="mt-2">Upload CSV file</span>
                </div>
              </label>
            </div>

            {/* Error Display */}
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            {/* Mapping Interface */}
            {csvHeaders.length > 0 && (
              <div className="space-y-4">
                <h3 className="font-bold">Map CSV Fields to CMS Fields</h3>
                <div className="grid gap-4">
                  {csvHeaders.map(header => (
                    <div key={header} className="flex items-center gap-4">
                      <span className="w-1/3">{header}</span>
                      <select
                        className="w-2/3 p-2 border rounded"
                        value={mappings[header]}
                        onChange={(e) => handleMappingChange(header, e.target.value)}
                      >
                        <option value="">Select CMS Field</option>
                        {cmsFields.map(field => (
                          <option key={field} value={field}>
                            {field}
                          </option>
                        ))}
                      </select>
                    </div>
                  ))}
                </div>

                <button
                  onClick={generateGroovyScript}
                  className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                  Generate Groovy Script
                </button>
              </div>
            )}

            {/* Generated Script Display */}
            {groovyScript && (
              <div className="mt-4">
                <div className="flex justify-between items-center mb-2">
                  <h3 className="font-bold">Generated Groovy Script</h3>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(groovyScript);
                      const button = document.activeElement;
                      if (button) {
                        const originalText = button.textContent;
                        button.textContent = 'Copied!';
                        setTimeout(() => {
                          button.textContent = originalText;
                        }, 2000);
                      }
                    }}
                    className="px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded border flex items-center gap-2"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75" />
                    </svg>
                    Copy
                  </button>
                </div>
                <pre className="bg-gray-100 p-4 rounded overflow-auto mt-2">
                  {groovyScript}
                </pre>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default CsvMapper;