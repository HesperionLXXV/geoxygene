package fr.ign.cogit.geoxygene.appli.plugin.matching.dst.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.appli.FloatingProjectFrame;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.Population;
import fr.ign.cogit.geoxygene.feature.SchemaDefaultFeature;
import fr.ign.cogit.geoxygene.function.ConstantFunction;
import fr.ign.cogit.geoxygene.function.Function1D;
import fr.ign.cogit.geoxygene.function.LinearFunction;
import fr.ign.cogit.geoxygene.matching.dst.evidence.ChoiceType;
import fr.ign.cogit.geoxygene.matching.dst.evidence.EvidenceResult;
import fr.ign.cogit.geoxygene.matching.dst.geomatching.GeoMatching;
import fr.ign.cogit.geoxygene.matching.dst.geomatching.GeomHypothesis;
import fr.ign.cogit.geoxygene.matching.dst.sources.Source;
import fr.ign.cogit.geoxygene.matching.dst.sources.punctual.EuclidianDist;
import fr.ign.cogit.geoxygene.matching.dst.sources.semantic.WuPalmerDistance;
import fr.ign.cogit.geoxygene.matching.dst.sources.text.LevenshteinDist;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.AttributeType;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.FeatureType;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiPoint;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;
import fr.ign.cogit.geoxygene.style.Layer;
import fr.ign.cogit.geoxygene.style.LineSymbolizer;
import fr.ign.cogit.geoxygene.style.PointSymbolizer;


public class EvidenceGuiProcess {
  
  /** LOGGER. */
  private final static Logger LOGGER = Logger.getLogger(EvidenceGuiProcess.class.getName());
  private final static Logger DST_LOGGER = Logger.getLogger("DSTLogger");
  private final static Logger DST_NA_LOGGER = Logger.getLogger("DSTNonApparie");

  private FloatingProjectFrame projectFrame;
  
  private IPopulation<IFeature> pointReliefPop;
  private IPopulation<IFeature> oronymePop;
  private List<Source<IFeature, GeomHypothesis>> criteria;
  
  private Population<DefaultFeature> popLien;
  private FeatureType featureTypeLien;
  private SchemaDefaultFeature schema;

  private DebugPanel debugPanel;
  private CriterePanel criterePanel;

  

  // ===== Set params. =====
  private double distanceSelection = 500;

  private Layer layerPoint;
  private Layer layerRef;

  private List<Layer> listLayer = new ArrayList<Layer>();
  
  public EvidenceGuiProcess(FloatingProjectFrame projectFrame, double w, double h) {
    
    this.projectFrame = projectFrame;
    
    this.setUp();
    this.initFeatureType();
    
    
    // On zoom sur l'étendue maximale (limites du WGS84)
    try {
      projectFrame.getLayerViewPanel().getViewport().zoomToFullExtent();
    } catch (NoninvertibleTransformException e1) {
      e1.printStackTrace();
    }
    
    // CriterePanel
    criterePanel = new CriterePanel(this, criteria, this.distanceSelection);

    // DebugPanel
    debugPanel = new DebugPanel();
    debugPanel.setPopLien(popLien);
    debugPanel.initPanel();
    
    projectFrame.getLayerViewPanel().setPreferredSize(
        new Dimension((int) (w * 0.75), (int) (h * 0.8)));
    debugPanel.setPreferredSize(new Dimension((int) (w), (int) (h * 0.2)));
    criterePanel.setPreferredSize(new Dimension((int) (w * 0.25), (int) (h)));

    try {
      projectFrame.getInternalFrame().setMaximum(true);
    } catch (Exception e) {
      e.printStackTrace();
    }

    criterePanel.setBackground(Color.WHITE);
    
    projectFrame.addComponentInProjectFrame(new JScrollPane(criterePanel),
            BorderLayout.EAST);
    projectFrame.addComponentInProjectFrame(debugPanel, BorderLayout.SOUTH);

  }
  
  private void setUp() {

    criteria = new ArrayList<Source<IFeature, GeomHypothesis>>();

    // ====================================================================================

    // Distance euclidienne
    EuclidianDist source = new EuclidianDist();

    // Fonction EstApparie
    Function1D[] listFEA = new Function1D[2];
    LinearFunction f11 = new LinearFunction(-0.9 / 800, 1);
    f11.setDomainOfFunction(0., 800., true, false);
    listFEA[0] = f11;
    ConstantFunction f12 = new ConstantFunction(0.1);
    f12.setDomainOfFunction(800., 1500., true, true);
    listFEA[1] = f12;
    source.setMasseAppCi(listFEA);

    // Fonction NonApparie
    Function1D[] listFNA = new Function1D[3];
    ConstantFunction f21 = new ConstantFunction(0.);
    f21.setDomainOfFunction(0., 400., true, false);
    listFNA[0] = f21;
    LinearFunction f22 = new LinearFunction(0.8 / 400, -0.8);
    f22.setDomainOfFunction(400., 800., true, false);
    listFNA[1] = f22;
    ConstantFunction f23 = new ConstantFunction(0.8);
    f23.setDomainOfFunction(800., 1500., true, true);
    listFNA[2] = f23;
    source.setMasseAppPasCi(listFNA);

    // Fonction PrononcePas
    Function1D[] listFPP = new Function1D[3];
    LinearFunction f31 = new LinearFunction(0.45 / 400, 0.);
    f31.setDomainOfFunction(0., 400., true, false);
    listFPP[0] = f31;
    LinearFunction f32 = new LinearFunction(-0.35 / 400, 0.8);
    f32.setDomainOfFunction(400., 800., true, false);
    listFPP[1] = f32;
    ConstantFunction f33 = new ConstantFunction(0.1);
    f33.setDomainOfFunction(800., 1500., true, true);
    listFPP[2] = f33;
    source.setMasseIgnorance(listFPP);

    criteria.add(source);

    // ====================================================================================

    LevenshteinDist levenshteinSource = new LevenshteinDist("toponyme", "NOM");
    double t = 0.7;

    // Fonction EstApparie
    listFEA = new Function1D[2];
    f11 = new LinearFunction(-0.9 / t, 1);
    f11.setDomainOfFunction(0., t, true, false);
    listFEA[0] = f11;
    f12 = new ConstantFunction(0.1);
    f12.setDomainOfFunction(t, 3, true, true);
    listFEA[1] = f12;
    levenshteinSource.setMasseAppCi(listFEA);

    // Fonction NonApparie
    listFNA = new Function1D[2];
    f22 = new LinearFunction(0.5 / t, 0);
    f22.setDomainOfFunction(0., t, true, false);
    listFNA[0] = f22;
    f23 = new ConstantFunction(0.5);
    f23.setDomainOfFunction(t, 3, true, true);
    listFNA[1] = f23;
    levenshteinSource.setMasseAppPasCi(listFNA);

    // Fonction PrononcePas
    listFPP = new Function1D[2];
    f31 = new LinearFunction(0.4 / t, 0.);
    f31.setDomainOfFunction(0., t, true, false);
    listFPP[0] = f31;
    ConstantFunction fL32 = new ConstantFunction(0.4);
    fL32.setDomainOfFunction(t, 3, true, false);
    listFPP[1] = fL32;
    levenshteinSource.setMasseIgnorance(listFPP);

    criteria.add(levenshteinSource);

    // ====================================================================================
    WuPalmerDistance wuPalmerDistance = new WuPalmerDistance("./data/matching-test/FusionTopoCarto2.owl", "nature", "NATURE");
    t = 0.6;

    // Fonction EstApparie
    listFEA = new Function1D[2];
    f11 = new LinearFunction(-0.4 / t, 0.5);
    f11.setDomainOfFunction(0., t, true, false);
    listFEA[0] = f11;
    f12 = new ConstantFunction(0.1);
    f12.setDomainOfFunction(t, 3, true, true);
    listFEA[1] = f12;
    wuPalmerDistance.setMasseAppCi(listFEA);

    // Fonction NonApparie
    listFNA = new Function1D[2];
    f22 = new LinearFunction(0.8 / t, 0);
    f22.setDomainOfFunction(0., t, true, false);
    listFNA[0] = f22;
    f23 = new ConstantFunction(0.8);
    f23.setDomainOfFunction(t, 3, true, true);
    listFNA[1] = f23;
    wuPalmerDistance.setMasseAppPasCi(listFNA);

    // Fonction PrononcePas
    listFPP = new Function1D[2];
    f31 = new LinearFunction(-0.4 / t, 0.5);
    f31.setDomainOfFunction(0., t, true, false);
    listFPP[0] = f31;
    fL32 = new ConstantFunction(0.1);
    fL32.setDomainOfFunction(t, 3, true, false);
    listFPP[1] = fL32;
    wuPalmerDistance.setMasseIgnorance(listFPP);

    criteria.add(wuPalmerDistance);
  }
  
  private void initFeatureType() {

    // Créer un featuretype pour les liens
    featureTypeLien = new FeatureType();
    featureTypeLien.setTypeName("Liens");
    featureTypeLien.setGeometryType(ILineString.class);

    AttributeType bdcNature = new AttributeType("nature1", "String");
    AttributeType bdcToponyme = new AttributeType("bdcToponyme", "String");
    AttributeType nbCandidat = new AttributeType("NbCandidat", "int");
    AttributeType bdnNature = new AttributeType("NATURE2", "String");
    AttributeType bdnToponyme = new AttributeType("bdnToponyme", "String");
    AttributeType pignistic = new AttributeType("MaxPign", "double");
    featureTypeLien.addFeatureAttribute(bdcNature);
    featureTypeLien.addFeatureAttribute(bdcToponyme);
    featureTypeLien.addFeatureAttribute(nbCandidat);
    featureTypeLien.addFeatureAttribute(bdnNature);
    featureTypeLien.addFeatureAttribute(bdnToponyme);
    featureTypeLien.addFeatureAttribute(pignistic);

    // Création d'un schéma associé au featureType
    schema = new SchemaDefaultFeature();
    schema.setFeatureType(featureTypeLien);

    featureTypeLien.setSchema(schema);

    Map<Integer, String[]> attLookup = new HashMap<Integer, String[]>(0);
    attLookup.put(new Integer(0), new String[] { bdcNature.getNomField(),
        bdcNature.getMemberName() });
    attLookup.put(new Integer(1), new String[] { bdcToponyme.getNomField(),
        bdcToponyme.getMemberName() });
    attLookup.put(new Integer(2), new String[] { nbCandidat.getNomField(),
        nbCandidat.getMemberName() });
    attLookup.put(new Integer(3), new String[] { bdnNature.getNomField(),
        bdnNature.getMemberName() });
    attLookup.put(new Integer(4), new String[] { bdnToponyme.getNomField(),
        bdnToponyme.getMemberName() });
    attLookup.put(new Integer(5), new String[] { pignistic.getNomField(),
        pignistic.getMemberName() });
    schema.setAttLookup(attLookup);

    // Création de la population
    popLien = new Population<DefaultFeature>(false, "Liens",
        DefaultFeature.class, true);
    popLien.setFeatureType(featureTypeLien);

  }
  
  
  public FloatingProjectFrame getProjectFrame() {
    return this.projectFrame;
  }
  
  public void setDistanceSelection(double d) {
    distanceSelection = d;
  }

  public FeatureType getFeatureTypeLien() {
    return featureTypeLien;
  }

  public Population<DefaultFeature> getPopLien() {
    return popLien;
  }

  public void setPopLien(Population<DefaultFeature> popLien) {
    this.popLien = popLien;
  }

  public DebugPanel getDebugPanel() {
    return this.debugPanel;
  }

  public void initLien() {
    if (pointReliefPop == null)
      return;
    for (IFeature point : pointReliefPop) {
      DefaultFeature lien = popLien.nouvelElement();
      lien.setSchema(schema);
      Object[] attributes = new Object[] { point.getAttribute("nature"),
          point.getAttribute("toponyme") };
      lien.setAttributes(attributes);
    }
  }

  public List<Layer> getListLayer() {
    return listLayer;
  }
  
  
  public void runEvidenceMatching() {
    DST_LOGGER.info("==================================================================");
    DST_LOGGER.info("   START   ");
    DST_LOGGER.info("==================================================================");

    // DATASET
    pointReliefPop = criterePanel.getJeu1();
    oronymePop = criterePanel.getJeu2();
    if (pointReliefPop == null) {
      javax.swing.JOptionPane.showMessageDialog(null,
          "You need to select one layer for dataset 1.");
      return;
    }
    if (oronymePop == null) {
      javax.swing.JOptionPane.showMessageDialog(null,
        "You need to select one layer for dataset 2.");
      return;
    }

    // SLD dataset1
    layerPoint = this.projectFrame.getLayer(criterePanel.getLayerNameDataset1());
    PointSymbolizer symbolizerP1 = (PointSymbolizer) layerPoint.getSymbolizer();
    symbolizerP1.setUnitOfMeasurePixel();
    symbolizerP1.getGraphic().setSize(6);
    symbolizerP1.getGraphic().getMarks().get(0).setWellKnownName("square");
    symbolizerP1.getGraphic().getMarks().get(0).getStroke().setStrokeWidth(1);
    symbolizerP1.getGraphic().getMarks().get(0).getStroke()
        .setColor(new Color(74, 163, 202));
    symbolizerP1.getGraphic().getMarks().get(0).getFill()
        .setColor(new Color(169, 209, 116));

    // SLD dataset 2
    layerRef = this.projectFrame.getLayer(criterePanel.getLayerNameDataset2());
    PointSymbolizer symbolizerP2 = (PointSymbolizer) layerRef.getSymbolizer();
    symbolizerP2.setUnitOfMeasurePixel();
    symbolizerP2.getGraphic().setSize(6);
    symbolizerP2.getGraphic().getMarks().get(0).setWellKnownName("square");
    symbolizerP2.getGraphic().getMarks().get(0).getStroke().setStrokeWidth(1);
    symbolizerP2.getGraphic().getMarks().get(0).getStroke()
        .setColor(new Color(210, 67, 71));
    symbolizerP2.getGraphic().getMarks().get(0).getFill()
        .setColor(new Color(241, 208, 188));

    // Select distance
    this.distanceSelection = criterePanel.getDistanceSelection();
    DST_LOGGER.info("Distance sélection candidat =  " + distanceSelection);

    // Set LIEN
  
    boolean closed = false;
    GeoMatching matching = new GeoMatching();
  
    popLien = new Population<DefaultFeature>(false, "Liens",
        DefaultFeature.class, true);
    popLien.setFeatureType(featureTypeLien);

    try {
      // Boucle sur les données de la base de référence
      for (IFeature point : pointReliefPop) {

        // On cherche les candidats
        IPopulation<IFeature> popRef = new Population<IFeature>("Ref");
        popRef.setFeatureType(point.getFeatureType());
        popRef.add(point);
  
        IPopulation<IFeature> candidat = oronymePop
            .selectionElementsProchesGenerale(popRef, distanceSelection);
        int nbCandidat = candidat.size();
        // DST_LOGGER.info("   " + candidat.getElements().size() +
        // " candidat(s)");
        // System.out.println("Nb candidat = " + nb);
        // System.out.println(point.getFeatureType().getFeatureAttributes().size()
        // + ", " + candidat.getFeatureType().getFeatureAttributes().size() +
        // " -- ");
  
        /*
         * for (int i = 0; i <
         * point.getFeatureType().getFeatureAttributes().size(); i++) {
         * System.out
         * .print(point.getFeatureType().getFeatureAttributes().get(i).
         * getMemberName()+", "); } System.out.print(" -- "); for (int i = 0; i
         * < candidat.getFeatureType().getFeatureAttributes().size(); i++) {
         * System
         * .out.print(candidat.getFeatureType().getFeatureAttributes().get(
         * i).getMemberName()+", "); } System.out.println("");
         */

        // On lance l'appariement
        if (nbCandidat > 0) {
          EvidenceResult<GeomHypothesis> result = matching.runAppriou(criteria,
              point, candidat.getElements(), ChoiceType.PIGNISTIC, closed);
  
          if (result != null && result.getHypothesis().size() > 0) {
  
            IDirectPosition coordBDC = ((GM_Point) point.getGeom())
                .getPosition();
  
            for (int k = 0; k < result.getHypothesis().size(); k++) {
              IDirectPosition coordBDN = null;
              if (result.getHypothesis().get(0).getGeom() instanceof GM_MultiPoint) {
                GM_MultiPoint pointN = (GM_MultiPoint) result.getHypothesis()
                    .get(0).getGeom();
                coordBDN = pointN.getList().get(0).getPosition();
              } else {
                coordBDN = ((GM_Point) result.getHypothesis().get(0).getGeom())
                    .getPosition();
              }
  
              // System.out.println(coordBDC.toString() + ", " +
              // coordBDN.toString());
              GM_LineString line = new GM_LineString(coordBDC, coordBDN);
              DefaultFeature lien = popLien.nouvelElement(line);
              lien.setSchema(schema);
              Object[] attributes = new Object[] {
                  point.getAttribute("nature"), point.getAttribute("toponyme"),
                  nbCandidat,
                  result.getHypothesis().get(k).getAttribute("NATURE"),
                  result.getHypothesis().get(k).getAttribute("NOM"),
                  result.getValue() };
              lien.setAttributes(attributes);
              // System.out.println("Nb attr = " +
              // result.getHypothesis().get(0).getFeatureType().getFeatureAttributes().size());
            }
  
            DST_LOGGER.info("   " + result.getHypothesis().size()
                + " nb results.");
            DST_LOGGER.info("   1 lien.");
  
          }
  
        } else {
  
          DST_NA_LOGGER.info("Toponyme " + point.getAttribute("toponyme") + "("
              + point.getAttribute("nature") + ")");
          DST_NA_LOGGER.info("   nb candidat = " + nbCandidat);
          if (nbCandidat > 0) {
            for (int i = 0; i < nbCandidat; i++) {
              DST_NA_LOGGER.info("   candidat = "
                  + candidat.get(i).getAttribute("NOM"));
            }
          }
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    Layer layerLien = this.projectFrame.addUserLayer(popLien, "Lien", null);
    LineSymbolizer symbolizerL1 = (LineSymbolizer) layerLien.getSymbolizer();
    symbolizerL1.getStroke().setStrokeWidth(5);
    symbolizerL1.getStroke().setStroke(new Color(203, 24, 29));

    listLayer.add(layerLien);

    // On rafraichit le debugPanel
    debugPanel.setPopLien(popLien);
    debugPanel.refreshDebug();

    // Boutons actifs
    criterePanel.setEnableMatriceConfusionButton(true);
    criterePanel.setEnableConfigLayerButton(true);

    LOGGER.info("--------------------------------------------------------");
    DST_LOGGER.info("");
    DST_LOGGER.info("Nb appariement = " + popLien.size() + " / "
        + pointReliefPop.size());
    LOGGER.info("Fin du process");
    LOGGER.info("--------------------------------------------------------");
  
  }
}
