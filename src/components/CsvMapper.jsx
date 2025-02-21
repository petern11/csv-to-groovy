"use client"

import React, { useState } from 'react';
import Papa from 'papaparse';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Upload } from 'lucide-react';

const CsvMapper = () => {
  const [csvData, setCsvData] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [mappings, setMappings] = useState({});
  const [groovyScript, setGroovyScript] = useState('');
  const [error, setError] = useState('');
  const [parentPath, setParentPath] = useState('/vax');
  const [templateName, setTemplateName] = useState('magnolia-ai-sanofi-lm:pages/vaxDetail');

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
          
          // Initialize mappings with empty values
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

  const generateGroovyScript = () => {
    if (!csvData) return;

    // Helper function to format field value
    const formatValue = (value) => {
      if (typeof value === 'string') {
        return `"""${value
          .replace(/"/g, '\\"')
          .replace(/\$/g, '\\$')}"""`
      }
      return value;
    };

    const formatGroovyMap = (obj) => {
      const entries = Object.entries(obj)
        .map(([key, value]) => `${key}: ${value}`)
        .join(',\n            ');
      return `[${entries}]`;
    };

    const script = `import info.magnolia.context.MgnlContext
import info.magnolia.jcr.util.NodeUtil
import javax.jcr.Node
import info.magnolia.jcr.util.SessionUtil

// Define parent path
def parentPath = "${parentPath.replace(/\/$/, '')}"  // Remove trailing slash if present

// Get website workspace
def ctx = MgnlContext.getInstance()
def workspace = ctx.getJCRSession('website')
def rootNode = workspace.getRootNode()

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
    page.setProperty("mgnl:template", "${templateName}")

    // Set page properties
    pageData.properties.each { field, value ->
        if (field == 'tags' && value) {
            def tagList = value.split(',')
            page.setProperty(field, tagList as String[])
        } else {
            page.setProperty(field, value)
        }
    }

    // Get the unwrapped node
    def pageNode = NodeUtil.unwrap(page)
}

// Save all changes
workspace.save()
println("DONE")`;

    setGroovyScript(script);
  };

  return (
    <div className="w-full max-w-4xl mx-auto p-4 space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>CSV to CMS Field Mapper</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
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