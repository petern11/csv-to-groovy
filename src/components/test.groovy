import info.magnolia.context.MgnlContext
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
def parentPath = "/vax"
def damPath = "/migration-images"
def sourceBaseUrl = "https://www.campus.sanofi/dk"  // Base URL for image downloads

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
        println('image found - processImage')
        // Handle both relative and absolute URLs
        def imageUrl = imageSrc.startsWith('http') ? imageSrc : "${sourceBaseUrl}${imageSrc.startsWith('/') ? '' : '/'}${imageSrc}"
        
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
            uniqueFileName = nameWithoutExt + "-${counter}" + extension
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
        println "Failed to process image: ${imageSrc}"
        println "Error: ${e.message}"
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
    [
        pageName: "cholera",
        properties: [title: """Cholera""",
            abstract: """Cholera is an acute, diarrhoeal illness caused by infection of the intestine with the bacterium Vibrio cholerae and is spread by ingestion of contaminated food or water. The infection is often mild or without symptoms, but can sometimes be severe. Approximately 1 in 10 infected persons have severe disease. In these people, rapid loss of body fluids leads to dehydration and shock. Without treatment, death can occur within hours.1""",
            tags: """travel, another""",
            textContent: """<div class=\"vaccine-disease-article-bg\">
  <div class=\"row\">
  <div class=\"col-xs-12 col-sm-5 col-md-4\">
 

  <div class=\"sticky-nav js-sticky-nav\" style=\"width: 400px;\">
  <div class=\"vaccine-disease-nav\">
  <ul class=\"vaccine-disease-nav-list\">
  <li>
  <a href=\"#Overview\">Overview</a>
  </li>
  <li>
  <a href=\"#Signs-and-symptoms\">Signs and symptoms</a>
  </li>
  <li>
  <a href=\"#Diagnosis\">Diagnosis</a>
  </li>
  <li>
  <a href=\"#Prevention\">Prevention</a>
  </li>
  <li>
  <a href=\"#Bacteriology\">Bacteriology</a>
  </li>
  <li>
  <a href=\"#Epidemiology\">Epidemiology</a>
  </li>
  <li>
  <a href=\"#Treatment\">Treatment</a>
  </li>
  </ul>
  </div><div class=\"vaccine-disease-nav-dropdown mobile-dropdown\">
     <select>
      <option value=\"#Overview\">
        Overview
       </option><option value=\"#Signs-and-symptoms\">
        Signs and symptoms
       </option><option value=\"#Diagnosis\">
        Diagnosis
       </option><option value=\"#Prevention\">
        Prevention
       </option><option value=\"#Bacteriology\">
        Bacteriology
       </option><option value=\"#Epidemiology\">
        Epidemiology
       </option><option value=\"#Treatment\">
        Treatment
       </option>
     </select>
    </div>
  </div>
 

  </div>
 

  <div class=\"col-xs-12 col-sm-7 col-md-8\">
 

  <div class=\"vaccine-disease-article-offset article-body\">
 

  <div class=\"article-tools\">
  <div class=\"row\">
     <div class=\"col-xs-4 col-md-12\">
        <span class=\"flag flag-favourite flag-favourite-86 js-flag-favourite-86 action-flag\"><a title=\"\" href=\"/flag/flag/favourite/86?destination=/disease/cholera&amp;token=RgzQEgT7-Dj6qVfVP6-NVG2vHwyTU501S_tGxSkNsQ0\" class=\"use-ajax\" rel=\"nofollow\" data-once=\"ajax\"><i class=\"icon icon-favourite\"></i></a></span>
 

      </div>
   <div class=\"col-xs-4 col-md-12\">
    <a href=\"/print/pdf/node/86\" target=\"_blank\">
     <span class=\"icon icon-print\"></span>
     <span class=\"text\">
      PRINT</span>
    </a>
   </div>
  </div>
 </div>
 

  <div class=\"scroll-container\">
  
  <div data-scroll=\"Overview\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Overview
  </h3>
  
  <p>Cholera is an acute, diarrhoeal illness caused by infection of the intestine with the bacterium <em>Vibrio cholerae </em>and is spread by ingestion of contaminated food or water. The infection is often mild or without symptoms, but can sometimes be severe. Approximately 1 in 10 infected persons have severe disease. In these people, rapid loss of body fluids leads to dehydration and shock. Without treatment, death can occur within hours.<sup>1</sup></p>
 

 <p>Researchers have estimated that each year there are 1.3 to 4.0 million cases of cholera, and 21,000 to 143,000 deaths worldwide due to cholera. Cholera vaccines can be used in conjunction with improvements in water and sanitation to control cholera outbreaks and for prevention in areas known to be high risk for cholera. Most cases can be successfully treated with oral rehydration solution, however, severe cases need rapid treatment with intravenous fluids and antibiotics.<sup>2</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Signs-and-symptoms\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Signs and symptoms
  </h3>
  
  <p>The incubation period for the cholera is 12 hours to 5 days. Most people infected with <em>V. cholerae</em> do not develop any symptoms, although the bacteria are present in their faeces for 1-10 days after infection and are shed back into the environment, potentially infecting other people. Of those who develop symptoms, these are mild to moderate and can be treated with oral rehydration fluids.<sup>2</sup></p>
 

 <p>However, others will develop acute watery diarrhoea (also called ‘rice water stools’) with severe dehydration which can lead to death if untreated. Other symptoms of severe disease include vomiting, tachycardia, loss of skin elasticity, low blood pressure, muscle cramps, restlessness or irritability.<sup>1</sup></p>
 

 

  </div>
  <div class=\"col-xs-12 col-sm-10 col-md-9\">
  
  </div>
  </div>
  </div>
  
  
  
  
  <div data-scroll=\"Diagnosis\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Diagnosis
  </h3>
  
  <p>It is almost impossible to distinguish a single patient with cholera from a patient infected by another pathogen that causes acute watery diarrhoea without testing a stool sample. Isolation and identification of <em>Vibrio cholerae</em> serogroup O1 or O139 by culture of a stool specimen remains the gold standard for the laboratory diagnosis of cholera. In areas with limited to no laboratory testing, the Crystal VC® dipstick rapid test can provide an early warning to public health officials that an outbreak of cholera is occurring. However, due to low sensitivity and specificity of the test, it should be confirmed using traditional culture-based methods suitable for the isolation and identification of <em>V. cholerae</em>.<sup>1</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Prevention\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Prevention
  </h3>
  
  <p>A combination of surveillance, provision of safe water, sanitation and hygiene promotion and oral cholera vaccines are used to prevent the disease.<sup>2&nbsp;&nbsp;</sup>Vaccination against cholera is not an official requirement for entry into any foreign country. Cholera vaccination is not routinely recommended as the risk to travellers is very low, despite the endemicity of cholera in some countries often visited by Australians. Careful and sensible selection of food and water is of far greater importance to the traveller than cholera vaccination.<sup>3</sup></p>
 

 <p>An oral cholera vaccine is available in Australia for adults and children aged 2 years and older, and should be considered for the following travellers:<sup>3</sup></p>
 

 <ul>
  <li>travellers with a high risk of exposure, including, humanitarian aid workers deployed where there is an endemic outbreak of cholera</li>
  <li>travellers to areas where cholera exists who have a high risk of acquiring diarrhoeal disease due to an underlying medical condition (eg. achlorhydria)</li>
  <li>travellers to areas where cholera exists who have a higher risk of severe complications from diarrhoeal disease (eg. inflammatory bowel disease, poorly controlled or complicated diabetes, HIV or other immunocompromising conditions, significant cardiovascular disease).</li>
 </ul>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Bacteriology\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Bacteriology
  </h3>
  
  <p><em>Vibrio cholerae</em> is a motile, curved gram-negative bacillus with more than 150 serogroups based on differences in the O antigens. Cholera is caused by enterotoxin-producing <em>V. cholerae</em> of serogroups O1 and O139. The bacteria can survive under unfavourable conditions in a viable dormant state and are transmitted predominantly by ingestion of faecally contaminated food or water.<sup>3&nbsp;&nbsp;</sup>Only toxigenic strains of serogroups O1 and O139 have caused widespread epidemics and are reportable to the WHO as “cholera”. Serogroup O1 includes two biotypes (classical and El Tor), each of which includes two distinct serotypes, Inaba and Ogawa.<sup>1</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Epidemiology\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Epidemiology
  </h3>
  
  <p>Cholera can be endemic or epidemic. A cholera outbreak/epidemic can occur in both endemic countries and in countries where cholera does not regularly occur.<sup>2</sup> It is considered endemic in Haiti, south and south-east Asia and sub-saharan Africa.<sup>3&nbsp;</sup></p>
 

 <p>Almost all the cases of cholera reported in Australia (about 2 to 6 cases a year) occur in individuals who have been infected in endemic areas overseas. In 1977, a locally acquired case led to the discovery of <em>V. cholerae</em> in some rivers of the Queensland coast , which indicated that health workers should be aware that sporadic cases may occur rarely following contact with estuarine waters. All cases of cholera reported since the commencement of the National Notifiable Diseases Surveillance System in 1991 have been acquired outside Australia, except for 1 case in 1996 which was laboratory-acquired and 3 cases in 2006 reported in Sydney, which were associated with consumption of raw imported whitebait in patients who had no history of recent travel to known cholera-endemic areas.<sup>3</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  
  
  <div data-scroll=\"Treatment\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Treatment
  </h3>
  
  <p>The majority of people who develop cholera can be treated successfully through prompt administration of oral rehydration solution (ORS). Severely dehydrated patients are at risk of shock and require the rapid administration of intravenous fluids. These patients are also given appropriate antibiotics to diminish the duration of diarrhoea, reduce the volume of rehydration fluids needed, and shorten the amount and duration of <em>V. cholerae</em> excretion in their stool.<sup>2</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  
  
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-sm-offset-1\">
  <div class=\"citations-sources accordian-component\">
  <div class=\"citations-sources-heading accordian-toggle\">
  References
  <span class=\"icon-closed icon icon-dropdown\"></span>
  <span class=\"icon-open icon icon-chevron-up\"></span>
  </div>
 

  <div class=\"accordian-expand\">
  <p>1. Cholera. Centers for Disease Control and Prevention. www.cdc.gov/cholera/index.html. (Accessed August 2021)<br>
 2. Cholera. World Health Organization Fact Sheet. www.who.int/en/news-room/fact-sheets/detail/cholera (Accessed August 2021).<br>
 3. Australian Immunisation Handbook 10th edition. Australian Govt. Department of health. https://immunisationhandbook.health.gov.au/vaccine-preventable-diseases/cholera (Accessed August 2021)</p>
 

 <p>MAT-AU-2101800&nbsp; &nbsp;Date of preparation&nbsp; August 2021<br>
 &nbsp;</p>
 

  </div>
 </div>
 

  <div class=\"article-tags\">
  <strong>Tags:</strong>
  
  <a href=\"/tags/travel\" hreflang=\"en\" shenjian-fields=\"\">travel</a>, <a href=\"/tags/adults\" hreflang=\"en\">adults</a>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>"""]
    ],
    [
        pageName: "diphtheria",
        properties: [title: """Diphtheria""",
            abstract: """Diphtheria is an infectious disease caused by the bacterium Corynebacterium diphtheriae, which primarily infects the throat and upper airways, and produces a toxin affecting other organs.""",
            tags: """pediatric""",
            textContent: """<div class=\"vaccine-disease-article-bg\">
  <div class=\"row\">
  <div class=\"col-xs-12 col-sm-5 col-md-4\">
 

  <div class=\"sticky-nav js-sticky-nav\" style=\"width: 400px;\">
  <div class=\"vaccine-disease-nav\">
  <ul class=\"vaccine-disease-nav-list\">
  <li>
  <a href=\"#Overview\">Overview</a>
  </li>
  <li>
  <a href=\"#Signs-and-symptoms\">Signs and symptoms</a>
  </li>
  <li>
  <a href=\"#Diagnosis\">Diagnosis</a>
  </li>
  <li>
  <a href=\"#Prevention\">Prevention</a>
  </li>
  <li>
  <a href=\"#Bacteriology\">Bacteriology</a>
  </li>
  <li>
  <a href=\"#Epidemiology\">Epidemiology</a>
  </li>
  <li>
  <a href=\"#Treatment\">Treatment</a>
  </li>
  </ul>
  </div><div class=\"vaccine-disease-nav-dropdown mobile-dropdown\">
     <select>
      <option value=\"#Overview\">
        Overview
       </option><option value=\"#Signs-and-symptoms\">
        Signs and symptoms
       </option><option value=\"#Diagnosis\">
        Diagnosis
       </option><option value=\"#Prevention\">
        Prevention
       </option><option value=\"#Bacteriology\">
        Bacteriology
       </option><option value=\"#Epidemiology\">
        Epidemiology
       </option><option value=\"#Treatment\">
        Treatment
       </option>
     </select>
    </div>
  </div>
 

  </div>
 

  <div class=\"col-xs-12 col-sm-7 col-md-8\">
 

  <div class=\"vaccine-disease-article-offset article-body\">
 

  <div class=\"article-tools\">
  <div class=\"row\">
     <div class=\"col-xs-4 col-md-12\">
        <span class=\"flag flag-favourite flag-favourite-85 js-flag-favourite-85 action-flag\"><a title=\"\" href=\"/flag/flag/favourite/85?destination=/disease/diphtheria&amp;token=EqjiuXJBtB-YFm7Dj0soFdwe6thZ9vxaPxpDdWSw45U\" class=\"use-ajax\" rel=\"nofollow\" data-once=\"ajax\"><i class=\"icon icon-favourite\"></i></a></span>
 

      </div>
   <div class=\"col-xs-4 col-md-12\">
    <a href=\"/print/pdf/node/85\" target=\"_blank\">
     <span class=\"icon icon-print\"></span>
     <span class=\"text\">
      PRINT</span>
    </a>
   </div>
  </div>
 </div>
 

  <div class=\"scroll-container\">
  
  <div data-scroll=\"Overview\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Overview
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Diphtheria is an infectious disease caused by the bacterium Corynebacterium diphtheriae, which primarily infects the throat and upper airways, and produces a toxin affecting other organs. The inflammatory exudate that forms a greyish or green membrane in the upper respiratory tract can cause acute severe respiratory obstruction. The disease spreads by aerosol transmission or by direct contact with skin lesions or articles soiled by infected persons.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">&nbsp;Vaccines are recommended for infants, children, teenagers and adults to prevent diphtheria.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">&nbsp;Treatment involves administering diphtheria antitoxin to neutralize the effects of the toxin, as well as antibiotics to kill the bacteria.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Signs-and-symptoms\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Signs and symptoms
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Infection can cause respiratory or cutaneous diphtheria, and in rare cases can lead to systemic diphtheria. Depending on the anatomical location, respiratory disease may be nasal, pharyngeal, or laryngeal, or any combination of these. Pharyngeal diphtheria is the most common form. Respiratory diphtheria usually occurs after an incubation period 2 to 5 days.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">&nbsp;Diphtheria spreads by aerosol transmission or by direct contact with skin lesions or articles soiled by infected persons.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup></span></span></span></span></p>
 

 <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Respiratory diphtheria has a gradual onset of symptoms which can include mild fever, sore throat, difficulty swallowing, malaise and loss of appetite. The hallmark of respiratory diphtheria is a pseudomembrane that appears within 2–3 days of illness over the mucous lining of the upper respiratory tract. The pseudomembrane is firm, fleshy, grey, and adherent; it typically will bleed after attempts to remove or dislodge it. Fatal airway obstruction can result if the pseudomembrane extends into the larynx or trachea or if a piece of it becomes dislodged.&nbsp;Diphtheria is fatal in 5 - 10% of cases.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">3</span></span></span></span></sup><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">&nbsp;Absorption of diphtheria toxin into the bloodstream results in toxic damage to organs such as the heart, kidneys and peripheral nerves.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup>&nbsp;</span></span></span></span></p>
 

 <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">The disease is communicable for up to 4 weeks, but carriers may shed organisms for longer.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  <div class=\"col-xs-12 col-sm-10 col-md-9\">
  
  </div>
  </div>
  </div>
  
  
  
  
  <div data-scroll=\"Diagnosis\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Diagnosis
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Diphtheria is usually diagnosed by the presence of clinical symptoms characteristic of the disease. However, bacterial culture is the mainstay of etiological diagnosis. Material for culture should be obtained preferably from the edges of the mucosal lesions and inoculated onto appropriate selective media. Identification of C. diphtheriae should be based on detection of the diphtheria toxin gene directly in clinical specimens using polymerase chain reaction techniques.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Prevention\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Prevention
  </h3>
  
  <p style=\"margin-top:13px; margin-bottom:13px\"><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Diphtheria can be prevented through immunisation. Diphtheria toxoid containing vaccines are available in Australia only in combination with tetanus, with or without other antigens such as<br>
 pertussis, inactivated poliomyelitis, hepatitis B and Haemophilus influenzae type b. Vaccines containing diphtheria toxoid are recommended for infants, children, teens and adults to prevent diphtheria. The formulations available for children aged &lt;10 years include Hexaxim, Infanrix, Infanrix hexa, Vaxelis, Infanrix IPV, Quadracel and Tripacel. Reduced antigen formulations for adults, adolescents and children aged ≥10 years include ADT Booster, Adacel, Adacel Polio, Boostrix, Boostrix-IPV.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup></span></span></span></span></p>
 

 <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">For close contacts of patients, age appropriate diphtheria toxoid booster should be given along with appropriate prophylactic antibiotics to prevent spread of infection.&nbsp;&nbsp;See the Immunisation Handbook for further information on recommendations.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Bacteriology\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Bacteriology
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Diphtheria is an acute illness caused by toxigenic strains of Corynebacterium diphtheriae, a Gram-positive, non-sporing, non-capsulate bacillus. The exotoxin produced by C. diphtheriae acts locally on the mucous membranes of the respiratory tract or, less commonly, on damaged skin, to produce an adherent pseudomembrane. Systemically, the toxin acts on cells of the myocardium, nervous system and adrenals.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">1</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  </div>
  </div>
  
  
  <div data-scroll=\"Epidemiology\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Epidemiology
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Vaccination against diphtheria has reduced the mortality and morbidity of diphtheria dramatically, however diphtheria is still a significant child health problem in countries with poor vaccination coverage.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup>&nbsp;</span></span></span></span></p>
 

 <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">In the decade between 1926 and 1935 over 4,000 deaths from diphtheria were reported in Australia. A vaccine for diphtheria was introduced in Australia in 1932, and since then both cases and deaths have virtually disappeared. Serosurveillance data indicate more than 99% of Australian children are immune to diphtheria. However, waning immunity amongst adults may result in this population being susceptible, with the most likely source of exposure being through overseas travel to countries where diphtheria remains endemic.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">4</span></span></span></span></sup>&nbsp;</span></span></span></span></p>
 

 <p><span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Between 1999 and 2019 there were two deaths reported in unvaccinated adults, who had both acquired the infection in Australia. In 2022 a case of respiratory diphtheria was reported in an unvaccinated toddler and a 6-year old child who was a close contact. Otherwise the majority of cases reported in Australia were acquired overseas</span></span></span></span></span>.<sup>1</sup></p>
 

 

  </div>
  </div>
  </div>
  
  
  
  
  <div data-scroll=\"Treatment\">
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-md-9 vaccine-disease-article-row\">
 

  <h3>
  Treatment
  </h3>
  
  <p><span style=\"font-size:11pt\"><span style=\"background:white\"><span style=\"line-height:normal\"><span style=\"font-family:Calibri,sans-serif\"><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Urgent treatment of diphtheria is mandatory to reduce complications and mortality. The mainstay of treatment is intramuscular or intravenous administration of diphtheria antitoxin. Antibiotics (penicillin or erythromycin) should be given for 14 days to limit further bacterial growth and the duration of corynebacterial carriage that often persists even after clinical recovery.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">2</span></span></span></span></sup><span style=\"font-size:11.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">&nbsp;Patient should be placed in isolation. Respiratory support and airway maintenance should also be administered as needed.</span></span></span></span><sup><span style=\"font-size:8.5pt\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">3</span></span></span></span></sup></span></span></span></span></p>
 

 

  </div>
  </div>
  </div>
  
  
  
  
  <div class=\"row center-xs\">
  <div class=\"col-xs-12 col-sm-10 col-sm-offset-1\">
  <div class=\"citations-sources accordian-component\">
  <div class=\"citations-sources-heading accordian-toggle\">
  References
  <span class=\"icon-closed icon icon-dropdown\"></span>
  <span class=\"icon-open icon icon-chevron-up\"></span>
  </div>
 

  <div class=\"accordian-expand\">
  <p class=\"MsoEndnoteText\" style=\"margin: 0cm 0cm 0.0001pt;\">1.&nbsp;<span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Australian Immunisation Handbook, Diphtheria. Available at: https://immunisationhandbook.health.gov.au/vaccine-preventable-diseases/diphtheria. Accessed March 1 2024</span></span></span></span></span><br>
 2. <span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">Diphtheria vaccine – World Health Organization (WHO) position paper. WHO. Weekly epidemiological record No. 3. 2006; 81: 24–31.</span></span></span></span></span><br>
 3. <span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">CDC. Yellow Book. Chapter 4 Diphtheria. </span></span></span></span></span><span style=\"font-size:11.0pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Calibri&quot;,sans-serif\"><a href=\"https://wwwnc.cdc.gov/travel/yellowbook/2020/travel-related-infectious-diseases/diphtheria\" style=\"color:#0563c1; text-decoration:underline\" data-lightbox=\"leavesite\"><span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"letter-spacing:-.35pt\">https://wwwnc.cdc.gov/travel/yellowbook/2020/travel-related-infectious-diseases/diphtheria</span></span></span></span></a></span></span></span><span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">. Accessed March 1 2024</span></span></span></span></span></p>
 

 <p class=\"MsoEndnoteText\" style=\"margin: 0cm 0cm 0.0001pt;\">4. <span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">NNDSS Annual Report Writing Group. Australia's notifiable disease status, 2011: Annual report of the National Notifiable Diseases Surveillance System. Communicable Diseases Intelligence. 2013;37:E313-393.</span></span></span></span></span></p>
 

 <p class=\"MsoEndnoteText\" style=\"margin: 0cm 0cm 0.0001pt;\">&nbsp;</p>
 

 <p class=\"MsoEndnoteText\" style=\"margin: 0cm 0cm 0.0001pt;\"><span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">MAT-AU-2400462-1.0 </span></span></span></span></span>&nbsp; &nbsp;<span style=\"font-size:11.5pt\"><span style=\"line-height:107%\"><span style=\"font-family:&quot;Helvetica&quot;,sans-serif\"><span style=\"color:#666666\"><span style=\"letter-spacing:-.35pt\">March 2024</span></span></span></span></span></p>
 

  </div>
 </div>
 

  <div class=\"article-tags\">
  <strong>Tags:</strong>
  
  <a href=\"/tags/pediatric\" hreflang=\"en\" shenjian-fields=\"\">pediatric</a>, <a href=\"/tags/children\" hreflang=\"en\">children</a>, <a href=\"/tags/adults\" hreflang=\"en\">adults</a>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>
  </div>"""]
    ],
    [
        pageName: "image-test",
        properties: [title: """Image test""",
            abstract: """BLorem ipsum dolor sit amet, consectetur adipiscing elit. Donec semper erat at nisi sodales tincidunt. Aliquam vitae vestibulum justo. Donec scelerisque mi tellus, nec venenatis felis elementum vitae. Etiam luctus dolor eget arcu vestibulum sodales. Aenean luctus metus et ornare suscipit. Morbi magna dui, sollicitudin vitae hendrerit nec, ultrices et lacus. Phasellus efficitur lectus velit, vitae venenatis nibh cursus vel. Aliquam erat volutpat. Donec ut leo euismod, consequat lacus at, vulputate justo. Suspendisse consequat in elit at congue. Vestibulum id arcu vitae diam volutpat elementum.""",
            tags: """Testing""",
            textContent: """<div class=\"ui-container container lg:max-w-6xl mt-8\">
<div class=\"article__lead space-y-space-600 mt-2 md:mt-0\">
<div>
</div>
<h1 class=\"article__title type-h1 \"> Livskvalitet og vurdering af symptomer hos KOL-patienter
</h1>
<div class=\"article__author \">
<span>Forfatter :</span> <span>Sanofi</span>
<div>Editorial Team</div>
</div>
</div>
<div class=\"ui-section py-space-600 \">
<div class=\"relative object-cover\">
<picture>
<source srcset=\"/.imaging/webp/sanofi-templates/article-hero-w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/header-banner.webp/jcr:content/header-banner.webp\" media=\"(min-width: 768px)\" width=\"1920\" height=\"565\">
<source srcset=\"/.imaging/webp/sanofi-templates/article-hero-w768/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/card-image.webp/jcr:content/card-image.webp\" media=\"(max-width: 768px)\" width=\"768\" height=\"565\">
<img class=\"image relative object-cover lazyloaded\" data-src=\"/.imaging/webp/sanofi-templates/article-hero-w768/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/card-image.webp/jcr:content/card-image.webp\" alt=\"\" src=\"/.imaging/webp/sanofi-templates/article-hero-w768/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/card-image.webp/jcr:content/card-image.webp\">
</picture>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div>
<div class=\"max-w-[750px] mx-auto\">
<img class=\"image max-w-[750px] mx-auto lazyloaded\" data-src=\"/dam/jcr:d5d384c7-0bae-406f-84d3-cb8fee4735bc/Quality%20of%20life_highlight-quality_DK.png\" alt=\"Highlight\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/highlight.webp/jcr:content/Quality%20of%20life_highlight-quality_DK.png 1920w\" src=\"/dam/jcr:d5d384c7-0bae-406f-84d3-cb8fee4735bc/Quality%20of%20life_highlight-quality_DK.png\">
</div>
</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">

<h2>SGRQ måler den kvalitative virkning af KOL<sup>1,a</sup></h2>
<p>Nogle af de områder, der evalueres af SGRQ, omfatter<sup>2</sup>:</p>

</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div id=\"icons\" x-data=\"\" x-intersect:enter.full.margin.-100px=\"\$store.sectionId.addSection('icons');\" x-intersect:leave.full.margin.-100px=\"\$store.sectionId.removeSection('icons');\"></div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div class=\"grid-tag\">
<div class=\"uigrid grid gap-space-600 grid-cols-1 md:grid-cols-4 lg:grid-cols-4 \">
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:bb425016-106c-4d13-b65f-90e2c2349078/Cough-icon@4x.webp\" alt=\"Cough\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 1920w\" src=\"/dam/jcr:bb425016-106c-4d13-b65f-90e2c2349078/Cough-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Hoste</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:640ad13c-8082-484b-9337-b7e690ce658f/Spectrum-icon@2x.webp\" alt=\"Spectrum\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 1920w\" src=\"/dam/jcr:640ad13c-8082-484b-9337-b7e690ce658f/Spectrum-icon@2x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Slimproduktion</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:533a6369-3ea0-412d-bdb2-87b140b1e9d7/Breathlessness-iconC.webp\" alt=\"Breathlessness\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 1920w\" src=\"/dam/jcr:533a6369-3ea0-412d-bdb2-87b140b1e9d7/Breathlessness-iconC.webp\">
<div class=\"image--caption type-eyebrow-sm\">Åndenød</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:044ce4dd-0852-4f61-8bb5-70d9819ebd3a/Wheezing-icon@4x.webp\" alt=\"Wheezing\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Wheezing-icon-4x.webp/jcr:content/Wheezing-icon@4x.webp 1920w\" src=\"/dam/jcr:044ce4dd-0852-4f61-8bb5-70d9819ebd3a/Wheezing-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Pibende vejrtrækning</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div class=\"grid-tag\">
<div class=\"uigrid grid gap-space-600 grid-cols-1 md:grid-cols-4 lg:grid-cols-4 \">
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:52c6c555-e39b-4621-ac31-64821b557483/exacerbation-icon@4x.webp\" alt=\"exacerbation\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/exacerbation-icon-4x.webp/jcr:content/exacerbation-icon@4x.webp 1920w\" src=\"/dam/jcr:52c6c555-e39b-4621-ac31-64821b557483/exacerbation-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Eksacerbationer</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:637c2aaa-a052-461a-a7ef-470f0288ba72/WorkActivities-icon@4x.webp\" alt=\"WorkActivities\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/WorkActivities-icon-4x.webp/jcr:content/WorkActivities-icon@4x.webp 1920w\" src=\"/dam/jcr:637c2aaa-a052-461a-a7ef-470f0288ba72/WorkActivities-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Evnen til at arbejde</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:dcd89b5a-1c07-4bfe-8079-2a024b53ff0a/DailyActivities-icon@4x.webp\" alt=\"DailyActivities\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/DailyActivities-icon-4x.webp/jcr:content/DailyActivities-icon@4x.webp 1920w\" src=\"/dam/jcr:dcd89b5a-1c07-4bfe-8079-2a024b53ff0a/DailyActivities-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Daglige aktiviteter</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:2e27a9e3-22c3-423d-afaa-be1de6108566/MedicationUse-icon@4x.webp\" alt=\"MedicationUse\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/MedicationUse-icon-4x.webp/jcr:content/MedicationUse-icon@4x.webp 1920w\" src=\"/dam/jcr:2e27a9e3-22c3-423d-afaa-be1de6108566/MedicationUse-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Medicinering</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">

<p>*Såsom at gå ovenpå eller klæde sig på.</p>
<h2>E-RS-COPD vurderer KOL-symptomer<sup>b</sup></h2>
<p>Nogle områder, der er evalueret af E-RS-COPD omfatter<sup>2</sup>:</p>

</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div id=\"icons\" x-data=\"\" x-intersect:enter.full.margin.-100px=\"\$store.sectionId.addSection('icons');\" x-intersect:leave.full.margin.-100px=\"\$store.sectionId.removeSection('icons');\"></div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div class=\"grid-tag\">
<div class=\"uigrid grid gap-space-600 grid-cols-1 md:grid-cols-4 lg:grid-cols-4 \">
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:bb425016-106c-4d13-b65f-90e2c2349078/Cough-icon@4x.webp\" alt=\"Cough\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Cough-icon-4x.webp/jcr:content/Cough-icon@4x.webp 1920w\" src=\"/dam/jcr:bb425016-106c-4d13-b65f-90e2c2349078/Cough-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Hoste</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:640ad13c-8082-484b-9337-b7e690ce658f/Spectrum-icon@2x.webp\" alt=\"Spectrum\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Spectrum-icon-2x.webp/jcr:content/Spectrum-icon@2x.webp 1920w\" src=\"/dam/jcr:640ad13c-8082-484b-9337-b7e690ce658f/Spectrum-icon@2x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Slimproduktion</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:533a6369-3ea0-412d-bdb2-87b140b1e9d7/Breathlessness-iconC.webp\" alt=\"Breathlessness\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/Breathlessness-iconC.webp/jcr:content/Breathlessness-iconC.webp 1920w\" src=\"/dam/jcr:533a6369-3ea0-412d-bdb2-87b140b1e9d7/Breathlessness-iconC.webp\">
<div class=\"image--caption type-eyebrow-sm\">Åndenød</div>
</div>
</div>
<div>
<div class=\"max-w-[300px] mx-auto\">
<img class=\"image max-w-[300px] mx-auto lazyloaded\" data-src=\"/dam/jcr:bfbf6191-7b5e-4bfd-b812-d23936874de7/ChestCongestion-icon@4x.webp\" alt=\"ChestCongestion\" srcset=\"/.imaging/webp/sanofi-templates/w256/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 256w,/.imaging/webp/sanofi-templates/w384/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 384w,/.imaging/webp/sanofi-templates/w640/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 640w,/.imaging/webp/sanofi-templates/w750/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 750w,/.imaging/webp/sanofi-templates/w828/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 828w,/.imaging/webp/sanofi-templates/w1080/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 1080w,/.imaging/webp/sanofi-templates/w1920/dam/campus-sanofi-dk/DK-articles/respiratory/copd/quality-of-life-and-symptom-measurement-in-copd-patients/ChestCongestion-icon-4x.webp/jcr:content/ChestCongestion-icon@4x.webp 1920w\" src=\"/dam/jcr:bfbf6191-7b5e-4bfd-b812-d23936874de7/ChestCongestion-icon@4x.webp\">
<div class=\"image--caption type-eyebrow-sm\">Trykken i brystet</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">

<p>Dette er ikke en udtømmende liste over punkter, der er vurderet af SGRQ og E-RS-COPD.</p>

</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">

<p class=\"type-legal\"><sup>a&nbsp;</sup>SGRQ er et spørgeskema med 50 elementer designet til at måle den kvalitative indvirkning af KOL på det generelle helbred, dagligdagen og det opfattede velvære. Højere score betyder større sygdommens sværhedsgrad.<sup>1</sup><br><sup>b</sup> E-RS-COPD er et 11-punkts patientrapporteret scoringsværktøj, der måler effekten af behandling på sværhedsgraden af respiratoriske symptomer hos patienter med stabil KOL. Højere score betyder større sygdomssværhedsgrad.<sup>3</sup></p>
<p class=\"type-legal\">KOL, kronisk obstruktiv lungesygdom; E-RS-COPD, Evaluering af luftvejssymptomer ved KOL; QoL, livskvalitet; SGRQ, St George’s Respiratory Questionnaire</p>
<p class=\"type-legal\"><sup>*</sup> Type 2 inflammation hos KOL er defineret ved forhøjet biomarkør. (Eosinofile celler i blodet &gt;300 µL eller &gt;2% eller eosinofile celler i sputum på &gt; 3%)</p>

</div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<div class=\"divider horizontal \"></div>
</div>
</div>
<div data-nosnippet=\"\" class=\"leadin\">
<div class=\"ui-section py-space-600 \">
<style>
.box-container, img[alt=\"Highlight\"]{
background: #e4e4e4 !important;
padding: 32px !important;
}
.image--caption{
margin-top: 20px;
text-align: center;
display: block;
}
#icons + div .image, #icons + div + div .image{
width: 200px;
height: auto;
}
@media only screen and (max-width: 500px) {
.box-container, img[alt=\"Highlight\"]{
padding: 12px !important;
}
}
</style> </div>
</div>
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">
<h3>Referencer</h3>
<ol>
<li>
<p class=\"type-legal\">Jones PW. St George’s Respiratory Questionnaire: MCID. COPD. 2005 Mar;2(1):75-79.</p>
</li>
<li>
<p class=\"type-legal\">Evidera website. EXACT and E-RS:COPD content. Accessed [February 9, 2024]. https://www.evidera.com/what-we-do/patient-centered-research/coa-instrument-management-services/exact-program/ exact-content/</p>
</li>
<li>
<p class=\"type-legal\">Leidy NK, Bushnell DM, Thach C, Hache C, Gutzwiller FS. Interpreting Evaluating Respiratory Symptoms in COPD diary scores in clinical trials: terminology, methods, and recommendations. Chronic Obstr Pulm Dis. 2022;9(4):576-590.</p>
</li>
</ol>
</div>
</div>
<div class=\"ui-section py-space-600 my-4\">
<div class=\"ui-content-list \" data-gtm-section=\"featuredArticles\">
<div class=\"flex flex-row justify-between\">
<div class=\"type-h3 content-list-title\">
Dette kunne også være af interesse
</div>
<div class=\"hidden lg:flex space-x-space-500\">
</div>
</div>
<div class=\"divider horizontal mt-3 mb-8\"></div>
<div class=\"content-list-text\">
<div ax-load=\"\" x-data=\"carousel({items: 1, responsive: {768: 2, 1034: 3}})\" class=\"carousel \" :dir=\"config.dir\" dir=\"ltr\">
<div class=\"carousel__wrapper relative flex flex-col pt-2 mb-10 \">
<div class=\"overflow-hidden\" dir=\"ltr\">
<div class=\"tns-outer\" id=\"tns1-ow\"><div class=\"tns-nav\" aria-label=\"Carousel Pagination\" style=\"display: none;\"><button type=\"button\" data-nav=\"0\" aria-controls=\"tns1\" style=\"display:none\" aria-label=\"Carousel Page 1 (Current Slide)\" class=\"tns-nav-active\"></button><button type=\"button\" data-nav=\"1\" tabindex=\"-1\" aria-controls=\"tns1\" style=\"display:none\" aria-label=\"Carousel Page 2\"></button><button type=\"button\" data-nav=\"2\" tabindex=\"-1\" aria-controls=\"tns1\" style=\"display:none\" aria-label=\"Carousel Page 3\"></button></div><div class=\"tns-liveregion tns-visually-hidden\" aria-live=\"polite\" aria-atomic=\"true\">slide <span class=\"current\">1 to 3</span>  of 3</div><div id=\"tns1-mw\" class=\"tns-ovh\"><div class=\"tns-inner\" id=\"tns1-iw\"><div class=\"slides overflow-hidden flex flex-row   tns-slider tns-carousel tns-subpixel tns-calc tns-horizontal\" :dir=\"config.dir\" x-ref=\"slides\" dir=\"ltr\" id=\"tns1\" style=\"transition-duration: 0s; transform: translate3d(0%, 0px, 0px);\">
<div class=\"carousel__slide tns-item tns-slide-active\" x-ref=\"slide\" id=\"tns1-item0\">
<a href=\"/dk/article/respiratory/copd/ar/role-of-type-2-inflammation\" class=\"card__container \" data-gtm-type=\"article_card\">
<div class=\"relative\">
<div class=\"card__image w-full bg-slate-100\">
<img class=\"image--base card__image w-full bg-slate-100 lazyloaded\" data-src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp\" alt=\"Betydningen af type 2-inflammation*\" srcset=\"/.imaging/webp/sanofi-templates/w220-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 220w,/.imaging/webp/sanofi-templates/w256-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 256w,/.imaging/webp/sanofi-templates/w360-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 360w,/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 384w,/.imaging/webp/sanofi-templates/w640-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 640w,/.imaging/webp/sanofi-templates/w750-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 752w,/.imaging/webp/sanofi-templates/w828-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 828w,/.imaging/webp/sanofi-templates/w1080-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 1080w,/.imaging/webp/sanofi-templates/w1920-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp 1920w\" sizes=\"(min-width: 1184px) 33.3vw, (min-width: 1034px) and (max-width: 1183px) 22vw, (min-width: 768px) and (max-width: 1033px) 34vw, 100vw\" src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/role-of-type-2-inflammation/card-image.webp/jcr:content/card-image.webp\" width=\"400\" height=\"300\" link=\"\">
</div>
</div>
<div class=\"card__content \">
<div class=\"card__meta flex justify-between\">
<div class=\"space-y-2\">
<div class=\"card__categories \">
</div>
</div>
</div>
<h2 class=\"heading card__heading line-clamp-3\">
Betydningen af type 2-inflammation
</h2>
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text card__description rich-text line-clamp-3\">
</div>
<div class=\"link !mt-6\" href=\"/dk/article/respiratory/copd/ar/role-of-type-2-inflammation\" type=\"solid\">
Læs mere
<span class=\"icon link-icon ml-2 rtl:mr-0 rtl:transform rtl:rotate-180 items-center\" role=\"button\" aria-label=\"link for Læs mere\">
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 448 512\"><!--! Font Awesome Free 6.4.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License) Copyright 2023 Fonticons, Inc. --><path d=\"M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.8 224 32 224c-17.7 0-32 14.3-32 32s14.3 32 32 32l306.7 0L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z\"></path></svg> </span>
</div>
</div>
</a>
</div>
<div class=\"carousel__slide tns-item tns-slide-active\" x-ref=\"slide\" id=\"tns1-item1\">
<a href=\"/dk/article/respiratory/copd/ar/one-minute-to-ignite-new-thinking-in-copd\" class=\"card__container \" data-gtm-type=\"article_card\">
<div class=\"relative\">
<div class=\"card__image w-full bg-slate-100\">
<img class=\"image--base card__image w-full bg-slate-100 lazyloaded\" data-src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp\" alt=\"Reflekter over ny tilgang til KOL\" srcset=\"/.imaging/webp/sanofi-templates/w220-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 220w,/.imaging/webp/sanofi-templates/w256-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 256w,/.imaging/webp/sanofi-templates/w360-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 360w,/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 384w,/.imaging/webp/sanofi-templates/w640-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 640w,/.imaging/webp/sanofi-templates/w750-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 752w,/.imaging/webp/sanofi-templates/w828-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 828w,/.imaging/webp/sanofi-templates/w1080-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 1080w,/.imaging/webp/sanofi-templates/w1920-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp 1920w\" sizes=\"(min-width: 1184px) 33.3vw, (min-width: 1034px) and (max-width: 1183px) 22vw, (min-width: 768px) and (max-width: 1033px) 34vw, 100vw\" src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/one-minute-to-ignite-new-thinking-in-copd/ignite_new_thinking_in_copd_card-image.webp/jcr:content/OVERVIEW%20BLOCK_300%20x%20225px_ignite_new_thinking_in_copd.webp\" width=\"400\" height=\"300\" link=\"\">
</div>
</div>
<div class=\"card__content \">
<div class=\"card__meta flex justify-between\">
<div class=\"space-y-2\">
<div class=\"card__categories \">
</div>
</div>
</div>
<h2 class=\"heading card__heading line-clamp-3\">
Reflekter over ny tilgang til KOL
</h2>
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text card__description rich-text line-clamp-3\">
<p>At få en forståelse for type 2 inflammation<sup>*</sup> kan belyse hvorfor nogle KOL-patienter fortsætter med at få eksacerbationer.&nbsp;Se denne video og lær mere.<sup>1</sup></p>
</div>
<div class=\"link !mt-6\" href=\"/dk/article/respiratory/copd/ar/one-minute-to-ignite-new-thinking-in-copd\" type=\"solid\">
Læs mere
<span class=\"icon link-icon ml-2 rtl:mr-0 rtl:transform rtl:rotate-180 items-center\" role=\"button\" aria-label=\"link for Læs mere\">
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 448 512\"><!--! Font Awesome Free 6.4.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License) Copyright 2023 Fonticons, Inc. --><path d=\"M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.8 224 32 224c-17.7 0-32 14.3-32 32s14.3 32 32 32l306.7 0L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z\"></path></svg> </span>
</div>
</div>
</a>
</div>
<div class=\"carousel__slide tns-item tns-slide-active\" x-ref=\"slide\" id=\"tns1-item2\">
<a href=\"/dk/article/respiratory/copd/ar/the-impact-of-exacerbations-in-copd\" class=\"card__container \" data-gtm-type=\"article_card\">
<div class=\"relative\">
<div class=\"card__image w-full bg-slate-100\">
<img class=\"image--base card__image w-full bg-slate-100 lazyloaded\" data-src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp\" alt=\"Betydningen af eksacerbationer ved KOL\" srcset=\"/.imaging/webp/sanofi-templates/w220-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 220w,/.imaging/webp/sanofi-templates/w256-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 256w,/.imaging/webp/sanofi-templates/w360-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 360w,/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 384w,/.imaging/webp/sanofi-templates/w640-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 640w,/.imaging/webp/sanofi-templates/w750-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 752w,/.imaging/webp/sanofi-templates/w828-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 828w,/.imaging/webp/sanofi-templates/w1080-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 1080w,/.imaging/webp/sanofi-templates/w1920-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp 1920w\" sizes=\"(min-width: 1184px) 33.3vw, (min-width: 1034px) and (max-width: 1183px) 22vw, (min-width: 768px) and (max-width: 1033px) 34vw, 100vw\" src=\"/.imaging/webp/sanofi-templates/w384-4-3/dam/campus-sanofi-dk/DK-articles/respiratory/copd/the-impact-of-exacerbations-in-copd/Media_card.webp/jcr:content/Media_card.webp\" width=\"400\" height=\"300\" link=\"\">
</div>
</div>
<div class=\"card__content \">
<div class=\"card__meta flex justify-between\">
<div class=\"space-y-2\">
<div class=\"card__categories \">
</div>
</div>
</div>
<h2 class=\"heading card__heading line-clamp-3\">
Betydningen af eksacerbationer ved KOL
</h2>
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text card__description rich-text line-clamp-3\">
</div>
<div class=\"link !mt-6\" href=\"/dk/article/respiratory/copd/ar/the-impact-of-exacerbations-in-copd\" type=\"solid\">
Læs mere
<span class=\"icon link-icon ml-2 rtl:mr-0 rtl:transform rtl:rotate-180 items-center\" role=\"button\" aria-label=\"link for Læs mere\">
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 448 512\"><!--! Font Awesome Free 6.4.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License) Copyright 2023 Fonticons, Inc. --><path d=\"M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.8 224 32 224c-17.7 0-32 14.3-32 32s14.3 32 32 32l306.7 0L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z\"></path></svg> </span>
</div>
</div>
</a>
</div>
</div></div></div></div>
</div>
<template x-if=\"isActive\">
<div class=\"carousel__pagination_numbers flex ltr:justify-start rtl:justify-end px-4 pt-8 md:hidden\" x-ref=\"pagination\">
<span x-text=\"paginationText\"></span>
</div>
</template>
<template x-if=\"isActive\">
<div class=\"carousel__pagination_buttons flex flex-1 justify-center pt-8 rtl:flex-row-reverse hidden md:flex\" x-ref=\"pagination\">
<template x-for=\"page in totalPages\">
<button class=\"w-4 h-4 rounded-full mr-2 focus:outline-none transition-all duration-200\" :class=\"page === currentPage ? 'active-pagination-button' : 'inactive-pagination-button'\" @click=\"goToPage(page)\"></button>
</template>
</div>
</template>
<template x-if=\"isActive\">
<div class=\"carousel__nav absolute right-0 -bottom-3 flex justify-between w-24 rtl:flex-row-reverse \">
<button class=\"carousel__navButton focus:outline-none\" @click=\"goToPreviousPage()\" :disabled=\"!hasPrevPage\">
<span class=\"icon inline-flex items-center\" role=\"button\" aria-label=\"go to previous page\">
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 448 512\"><!--! Font Awesome Free 6.4.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License) Copyright 2023 Fonticons, Inc. --><path d=\"M9.4 233.4c-12.5 12.5-12.5 32.8 0 45.3l160 160c12.5 12.5 32.8 12.5 45.3 0s12.5-32.8 0-45.3L109.2 288 416 288c17.7 0 32-14.3 32-32s-14.3-32-32-32l-306.7 0L214.6 118.6c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0l-160 160z\"></path></svg> </span>
</button>
<button class=\"carousel__navButton focus:outline-none\" @click=\"goToNextPage()\" :disabled=\"!hasNextPage\">
<span class=\"icon inline-flex items-center\" role=\"button\" aria-label=\"go to next page\">
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 448 512\"><!--! Font Awesome Free 6.4.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License) Copyright 2023 Fonticons, Inc. --><path d=\"M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.8 224 32 224c-17.7 0-32 14.3-32 32s14.3 32 32 32l306.7 0L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z\"></path></svg> </span>
</button>
</div>
</template>
</div>
</div>
</div>
</div>
</div>
<div class=\"ui-section py-space-600 \">
<div ax-load=\"\" x-data=\"richText\" class=\"rich-text \">
MAT-BE-2400702
</div>
</div>
</div>"""]
    ]
]

// Process each page
pagesData.each { pageData ->
    // Create page node
    def page = NodeUtil.createPath(rootNode, parentPath + "/" + pageData.pageName, "mgnl:page")
    page.setProperty("mgnl:template", "magnolia-ai-sanofi-lm:pages/vaxDetail")

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
damWorkspace.save()