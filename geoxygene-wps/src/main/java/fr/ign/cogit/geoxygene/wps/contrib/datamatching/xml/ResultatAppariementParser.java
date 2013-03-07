/**
*
*        This software is released under the licence CeCILL
* 
*        see Licence_CeCILL_V2-fr.txt
*        see Licence_CeCILL_V2-en.txt
* 
*        see <a href="http://www.cecill.info/">http://www.cecill.info</a>
* 
* 
* @copyright IGN
*
*/
package fr.ign.cogit.geoxygene.wps.contrib.datamatching.xml;

import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;
import org.geotools.GML;
import org.geotools.GML.Version;

import fr.ign.cogit.geoxygene.contrib.appariement.reseaux.ResultatAppariement;
import fr.ign.cogit.geoxygene.contrib.appariement.reseaux.ResultatStatAppariement;
import fr.ign.cogit.geoxygene.contrib.appariement.reseaux.ResultatStatEvaluationAppariement;


/**
 * Parser for the ResultatAppariement object.
 * 
 * 
 * @author M.-D. Van Damme
 * @version 1.6
 */
public class ResultatAppariementParser {
  
  /** LOGGER. */
  private final static Logger LOGGER = Logger.getLogger(ResultatAppariementParser.class.getName());
  
  /**
   * Génère une réponse XML à partir du résultat de l'appariement de réseau.
   * 
   * @param resultatAppariement
   *            L'objet résultat de l'appariement
   * @return Le XML correspondant
   */
  public String generateXMLResponse(ResultatAppariement resultatAppariement) throws Exception {
    
    LOGGER.info("------------------------------------------------------------------------");
    LOGGER.info("Start generating resultat appariement xml response.");
    
    StringBuffer result = new StringBuffer();
    
    // TODO Links data set
    // coming soon ....

    // Start document
    result.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    result.append("<NetworkDataMatchingResult>");
    
    // Stats results.
    ResultatStatAppariement resultatStatAppariement = resultatAppariement.getResultStat();
    
    // Edges evaluation of the less detailed network
    result.append("<EdgesEvaluationRef>");
    ResultatStatEvaluationAppariement edgesEvaluationRef = resultatStatAppariement.getEdgesEvaluationRef();
    result.append("<TotalNumber>" + edgesEvaluationRef.getTotalNumber() + "</TotalNumber>");
    result.append("<OkNumber>" + edgesEvaluationRef.getOkNumber() + "</OkNumber>");
    result.append("<KoNumber>" + edgesEvaluationRef.getKoNumber() + "</KoNumber>");
    result.append("<DoubtfulNumber>" + edgesEvaluationRef.getDoubtfulNumber() + "</DoubtfulNumber>");
    result.append("</EdgesEvaluationRef>");
    
    // Nodes evaluation of the less detailed network
    result.append("<NodesEvaluationRef>");
    ResultatStatEvaluationAppariement nodesEvaluationRef = resultatStatAppariement.getNodesEvaluationRef();
    result.append("<TotalNumber>" + nodesEvaluationRef.getTotalNumber() + "</TotalNumber>");
    result.append("<OkNumber>" + nodesEvaluationRef.getOkNumber() + "</OkNumber>");
    result.append("<KoNumber>" + nodesEvaluationRef.getKoNumber() + "</KoNumber>");
    result.append("<DoubtfulNumber>" + nodesEvaluationRef.getDoubtfulNumber() + "</DoubtfulNumber>");
    result.append("</NodesEvaluationRef>");
    
    // Edges evaluation of the less detailed network
    result.append("<EdgesEvaluationComp>");
    ResultatStatEvaluationAppariement edgesEvaluationComp = resultatStatAppariement.getEdgesEvaluationComp();
    result.append("<TotalNumber>" + edgesEvaluationComp.getTotalNumber() + "</TotalNumber>");
    result.append("<OkNumber>" + edgesEvaluationComp.getOkNumber() + "</OkNumber>");
    result.append("<KoNumber>" + edgesEvaluationComp.getKoNumber() + "</KoNumber>");
    result.append("<DoubtfulNumber>" + edgesEvaluationComp.getDoubtfulNumber() + "</DoubtfulNumber>");
    result.append("</EdgesEvaluationComp>");
    
    // Nodes evaluation of the less detailed network
    result.append("<NodesEvaluationComp>");
    ResultatStatEvaluationAppariement nodesEvaluationComp = resultatStatAppariement.getNodesEvaluationComp();
    result.append("<TotalNumber>" + nodesEvaluationComp.getTotalNumber() + "</TotalNumber>");
    result.append("<OkNumber>" + nodesEvaluationComp.getOkNumber() + "</OkNumber>");
    result.append("<KoNumber>" + nodesEvaluationComp.getKoNumber() + "</KoNumber>");
    result.append("<DoubtfulNumber>" + nodesEvaluationComp.getDoubtfulNumber() + "</DoubtfulNumber>");
    result.append("</NodesEvaluationComp>");
    
    // GML Network Matched
    result.append("<NetworkMatched>");
    try {
      
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      GML encode = new GML(Version.WFS1_0);
      encode.setNamespace("geotools", "http://geotools.org");
      encode.encode(output, resultatAppariement.getNetworkMatched());
      
      String buffer = output.toString();
      int begin = buffer.indexOf("wfs:FeatureCollection");
      buffer = buffer.substring(begin - 1, buffer.length() - 1);
          
      // Add to the document
      result.append(buffer);
    
    } catch (Exception e) {
      e.printStackTrace();
    }
    result.append("</NetworkMatched>");
    
    // End document
    result.append("</NetworkDataMatchingResult>");
    
    LOGGER.info("End generating resultat appariement xml response.");
    LOGGER.info("------------------------------------------------------------------------");
    
    // Return the document
    return result.toString();
  }

}