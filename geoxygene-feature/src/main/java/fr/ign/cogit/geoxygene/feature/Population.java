/*
 * This file is part of the GeOxygene project source files. GeOxygene aims at
 * providing an open framework which implements OGC/ISO specifications for the
 * development and deployment of geographic (GIS) applications. It is a open
 * source contribution of the COGIT laboratory at the Institut Géographique
 * National (the French National Mapping Agency). See:
 * http://oxygene-project.sourceforge.net Copyright (C) 2005 Institut
 * Géographique National This library is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License,
 * or any later version. This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details. You should have received a copy of
 * the GNU Lesser General Public License along with this library (see file
 * LICENSE if present); if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package fr.ign.cogit.geoxygene.feature;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import fr.ign.cogit.geoxygene.api.feature.IDataSet;
import fr.ign.cogit.geoxygene.api.feature.IExtraction;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IEnvelope;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.schema.schemaConceptuelISOJeu.FeatureType;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Envelope;
import fr.ign.cogit.geoxygene.util.index.Tiling;

/**
 * Une population représente TOUS les objets d'une classe héritant de
 * FT_Feature.
 * 
 * <P>
 * Les objets qui la composent peuvent avoir une géometrie ou non. La population
 * peut être persistante ou non, associée à un index spatial ou non.
 * 
 * <P>
 * NB: une population existe indépendamment des ses éléments. Avant de charger
 * ses éléments, la population existe mais ne contient aucun élément.
 * 
 * <P>
 * Difference avec FT_FeatureCollection : une Population est une
 * FT_FeatureCollection possedant les proprietes suivantes.
 * <UL>
 * <LI>Lien vers DataSet.</LI>
 * <LI>Une population peut-etre persistante et exister independamment de ses
 * elements.</LI>
 * <LI>Une population contient TOUS les elements de la classe.</LI>
 * <LI>Un element ne peut appartenir qu'a une seule population (mais a plusieurs
 * FT_FeatureCollection).</LI>
 * <LI>Permet de gerer la persistence des elements de maniere efficace (via
 * chargeElement(), nouvelElement(), etc.)</LI>
 * <LI>Possede quelques attributs (nom classe, etc.).</LI>
 * </UL>
 * TODO Finir les annotations pour la persistance
 * 
 * @author Sébastien Mustière
 * @author Sandrine Balley
 * @author Julien Perret
 */
@Entity
public class Population<Feat extends IFeature> extends
    FT_FeatureCollection<Feat> implements IPopulation<Feat> {

  /** Identifiant. Correspond au "cogitID" des tables du SGBD. */
  protected int id;

  @Override
  @Id
  public int getId() {
    return this.id;
  }

  @Override
  public void setId(int I) {
    this.id = I;
  }

  // /////////////////////////////////////////////////////
  // Constructeurs / Chargement / persistance
  // /////////////////////////////////////////////////////
  /**
   * Constructeur par défaut. Sauf besoins particuliers, utiliser plutôt l'autre
   * constructeur
   */
  public Population() {
    this.persistant = false;
  }

  @SuppressWarnings("unchecked")
  public Population(final Population<? extends IFeature> p) {
    super();
    this.setNom(p.getNom());
    this.setNomClasse(p.getNom());
    SchemaDefaultFeature schema = null;
    if (p.getFeatureType() != null) {
      this.setFeatureType(((FeatureType) p.getFeatureType()).copie());
    }
    for (int i = 0; i < p.size(); i++) {
      if (DefaultFeature.class.isAssignableFrom(p.get(i).getClass())) {
        DefaultFeature f = new DefaultFeature((DefaultFeature) p.get(i));
        if (schema == null) {
          schema = new SchemaDefaultFeature(
              ((DefaultFeature) p.get(i)).getSchema());
        }
        f.setSchema(schema);
        f.setFeatureType(this.featureType);
        this.add((Feat) f);
      } else {
        try {
          IFeature f = p.get(i).getClass().getConstructor(p.get(i).getClass())
              .newInstance(p.get(i));
          this.add((Feat) f);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Constructeur à partir du nom de la population
   * @param nom nom de la population.
   */
  public Population(String nom) {
    this();
    this.setNom(nom);
  }

  /**
   * Constructeur d'une population. Une population peut être persistante ou non
   * (la population elle-même est alors rendue persistante dans ce
   * constructeur). Une population a un nom logique (utile pour naviguer entre
   * populations). Les éléments d'une population se réalisent dans une classe
   * concrete (classeElements).
   * <p>
   * <b>NB :</b> lors la construction, auncun élément n'est affectée à la
   * population, cela doit être fait à partir d'elements peristant avec
   * chargeElements, ou a partir d'objets Java avec les setElements
   * @param persistance si vrai, alors la population est persistante
   * @param nomLogique nom de la population
   * @param classeElements classe des éléments de la population
   * @param drapeauGeom vrai si les éléments de la population portent une
   *          géométrie, faux sinon
   */
  @SuppressWarnings("unchecked")
  public Population(boolean persistance, String nomLogique,
      Class<?> classeElements, boolean drapeauGeom) {
    this.setPersistant(persistance);
    this.setNom(nomLogique);
    this.setClasse((Class<Feat>) classeElements);
    this.flagGeom = drapeauGeom;
    if (persistance) {
      DataSet.db.makePersistent(this);
    }
  }

  /**
   * Constructeur le plus adapté à l'utilisation des Populations dotées d'un
   * lien vers le FeatureType correspondant.
   * 
   * @param ft
   */
  public Population(FeatureType ft) {
    this.setFeatureType(ft);
    this.setNom(ft.getTypeName());
    this.setNomClasse(ft.getNomClasse());
  }

  /**
   * Constructeur d'une population. Une population peut être persistante ou non
   * (la population elle-même est alors rendue persistante dans ce
   * constructeur). Une population a un nom logique (utile pour naviguer entre
   * populations). Les éléments d'une population se réalisent dans une classe
   * concrete (nom_classe_elements).
   * <p>
   * <b>NB :</b> lors la construction, auncun élément n'est affecté à la
   * population, cela doit être fait à partir d'elements peristant avec
   * chargeElements, ou a partir d'objets Java avec les setElements
   */
  public Population(FeatureType ft, boolean persistance, boolean drapeauGeom) {
    this.setFeatureType(ft);
    this.setNom(ft.getTypeName());
    this.setNomClasse(ft.getNomClasse());
    this.setPersistant(persistance);
    this.flagGeom = drapeauGeom;
    if (persistance) {
      DataSet.db.makePersistent(this);
    }
  }

  /**
   * @param ft
   * @param persistance
   */
  public Population(FeatureType ft, boolean persistance) {
    /** attention nom de classe sans package, ca ne marche pas* */
    this.setFeatureType(ft);
    this.setNom(ft.getTypeName());
    this.setNomClasse(ft.getNomClasse());
    this.setPersistant(persistance);
    this.flagGeom = true;
    if (persistance) {
      DataSet.db.makePersistent(this);
    }
  }

  @Override
  public void chargeElements() {
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- Chargement des elements de la population  " + this.getNom()); //$NON-NLS-1$
    }
    if (!this.getPersistant()) {
      FT_FeatureCollection.logger
          .warn("----- ATTENTION : Aucune instance n'est chargee dans la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .warn("-----             La population n'est pas persistante"); //$NON-NLS-1$
      return;
    }
    try {
      this.elements = DataSet.db.loadAll(this.classe);
    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("----- ATTENTION : Chargement impossible de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("-----             Sans doute un probleme avec le SGBD, ou table inexistante, ou pas de mapping "); //$NON-NLS-1$
      // e.printStackTrace();
      return;
    }
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- " + this.size() + " instances chargees dans la population"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  @Override
  public void chargeElementsPartie(IGeometry geom) {
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- Chargement des elements de la population  " + this.getNom()); //$NON-NLS-1$
    }
    if (!this.getPersistant()) {
      FT_FeatureCollection.logger
          .warn("----- ATTENTION : Aucune instance n'est chargee dans la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .warn("-----             La population n'est pas persistante"); //$NON-NLS-1$
      return;
    }
    if (!this.hasGeom()) {
      FT_FeatureCollection.logger
          .warn("----- ATTENTION : Aucune instance n'est chargee dans la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .warn("-----             Les éléments de la population n'ont pas de géométrie"); //$NON-NLS-1$
      return;
    }
    try {
      this.elements = DataSet.db.loadAllFeatures(this.getClasse(), geom)
          .getElements();
    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("----- ATTENTION : Chargement impossible de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("-----             La classe n'est peut-être pas indexée dans le SGBD"); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("-----             ou table inexistante, ou pas de mapping ou probleme avec le SGBD "); //$NON-NLS-1$
      return;
    }
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("   " + this.size() + " instances chargees dans la population"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  @Override
  public void chargeElementsProches(IPopulation<Feat> pop, double dist) {
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- Chargement des elements de la population  " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .info("-- à moins de " + dist + " de ceux de la population   " + pop.getNom()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (!this.getPersistant()) {
      FT_FeatureCollection.logger
          .warn("----- ATTENTION : Aucune instance n'est chargee dans la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .warn("-----             La population n'est pas persistante"); //$NON-NLS-1$
      return;
    }
    try {
      Iterator<Feat> itPop = pop.getElements().iterator();
      Collection<Feat> selectionTotale = new HashSet<Feat>();
      while (itPop.hasNext()) {
        Feat objet = itPop.next();
        IFeatureCollection<Feat> selection = DataSet.db.loadAllFeatures(
            this.classe, objet.getGeom(), dist);
        selectionTotale.addAll(selection.getElements());
      }
      this.elements = new ArrayList<Feat>(selectionTotale);
    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("----- ATTENTION : Chargement impossible de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("-----             Sans doute un probleme avec le SGBD, ou table inexistante, ou pas de mapping "); //$NON-NLS-1$
      e.printStackTrace();
      return;
    }
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- " + this.size() + " instances chargees dans la population"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  @Override
  public IPopulation<Feat> selectionElementsProchesGenerale(
      IPopulation<Feat> pop, double dist) {

    Population<Feat> popTemporaire = new Population<Feat>();
    Population<Feat> popResultat = new Population<Feat>(false, this.getNom(),
        this.getClasse(), true);

    if (this.featureType != null) {
      popResultat.setFeatureType(this.featureType);
    }
    Set<Feat> selectionUnObjet, selectionTotale = new HashSet<Feat>();

    popTemporaire.addCollection(this);
    popTemporaire.initSpatialIndex(Tiling.class, true, 20);
    if (FT_FeatureCollection.logger.isDebugEnabled()) {
      FT_FeatureCollection.logger
          .debug("Fin indexation " + (new Time(System.currentTimeMillis())).toString()); //$NON-NLS-1$
    }

    Iterator<Feat> itPop = pop.getElements().iterator();
    while (itPop.hasNext()) {
      Feat objet = itPop.next();
      IEnvelope enveloppe = objet.getGeom().envelope();
      double xmin = enveloppe.getLowerCorner().getX() - dist;
      double xmax = enveloppe.getUpperCorner().getX() + dist;
      double ymin = enveloppe.getLowerCorner().getY() - dist;
      double ymax = enveloppe.getUpperCorner().getY() + dist;
      enveloppe = new GM_Envelope(xmin, xmax, ymin, ymax);
      Collection<Feat> selection = popTemporaire.select(enveloppe);
      Iterator<Feat> itSel = selection.iterator();
      selectionUnObjet = new HashSet<Feat>();
      while (itSel.hasNext()) {
        Feat objetSel = itSel.next();
        // if
        // (Distances.premiereComposanteHausdorff((GM_LineString)objetSel.getGeom(),(GM_LineString)objet.getGeom())<dist)
        if (objetSel.getGeom().distance(objet.getGeom()) < dist) {
          selectionUnObjet.add(objetSel);
        }
      }
      popTemporaire.getElements().removeAll(selectionUnObjet);
      selectionTotale.addAll(selectionUnObjet);
    }
    popResultat.setElements(selectionTotale);
    return popResultat;
  }

  /**
   * Renvoie une population avec tous les éléments de this situés à moins de
   * "dist" des éléments de la population pop.
   */
  @Override
  public IPopulation<Feat> selectionLargeElementsProches(IPopulation<Feat> pop,
      double dist) {
    Population<Feat> popTemporaire = new Population<Feat>();
    Population<Feat> popResultat = new Population<Feat>(false, this.getNom(),
        this.getClasse(), true);

    popTemporaire.addCollection(this);
    popTemporaire.initSpatialIndex(Tiling.class, true);
    Iterator<Feat> itPop = pop.getElements().iterator();
    while (itPop.hasNext()) {
      Feat objet = itPop.next();
      IEnvelope enveloppe = objet.getGeom().envelope();
      double xmin = enveloppe.getLowerCorner().getX() - dist;
      double xmax = enveloppe.getUpperCorner().getX() + dist;
      double ymin = enveloppe.getLowerCorner().getY() - dist;
      double ymax = enveloppe.getUpperCorner().getY() + dist;
      enveloppe = new GM_Envelope(xmin, xmax, ymin, ymax);
      Collection<Feat> selection = popTemporaire.select(enveloppe);
      popTemporaire.getElements().removeAll(selection);
      popResultat.addAll(selection);
    }
    return popResultat;
  }

  @Override
  public void chargeElementsPartie(IExtraction zoneExtraction) {
    this.chargeElementsPartie(zoneExtraction.getGeom());
  }

  @Override
  public void detruitPopulation() {
    if (!this.getPersistant()) {
      return;
    }
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("Destruction de la population des " + this.getNom()); //$NON-NLS-1$
    }
    DataSet.db.deletePersistent(this);
  }

  // /////////////////////////////////////////////////////
  // Attributs décrivant la population
  // /////////////////////////////////////////////////////
  /**
   * Nom logique des éléments de la population. La seule contrainte est de ne
   * pas dépasser 255 caractères, les accents et espaces sont autorisés. A
   * priori, on met le nom des éléments au singulier. Exemple:
   * "Tronçon de route"
   */
  protected String nom;

  @Override
  public String getNom() {
    return this.nom;
  }

  @Override
  public void setNom(String S) {
    this.nom = S;
  }

  /**
   * Booléen spécifiant si la population est persistente ou non (vrai par
   * défaut).
   */
  // NB pour dévelopeurs : laisser 'true' par défaut.
  // Sinon, cela pose des problèmes au chargement (un thème persistant chargé a
  // son attribut persistant à false).
  protected boolean persistant = true;

  @Override
  public boolean getPersistant() {
    return this.persistant;
  }

  @Override
  public void setPersistant(boolean b) {
    this.persistant = b;
  }

  // /////////////////////////////////////////////////////
  // Relations avec les thèmes et les étéments
  // /////////////////////////////////////////////////////
  /**
   * DataSet auquel apparient la population (une population appartient à un seul
   * DataSet).
   */
  protected IDataSet<?> dataSet;

  @Override
  @ManyToOne
  public IDataSet<?> getDataSet() {
    return this.dataSet;
  }

  @Override
  public void setDataSet(IDataSet<?> O) {
    IDataSet<?> old = this.dataSet;
    this.dataSet = O;
    if (old != null) {
      old.getPopulations().remove(this);
    }
    if (O != null) {
      this.dataSetID = O.getId();
      if (!(O.getPopulations().contains(this))) {
        O.getPopulations().add(this);
      }
    } else {
      this.dataSetID = 0;
    }
  }

  private int dataSetID;

  @Override
  public void setDataSetID(int I) {
    this.dataSetID = I;
  }

  @Override
  @Transient
  public int getDataSetID() {
    return this.dataSetID;
  }

  // ////////////////////////////////////////////////
  // Methodes surchargeant des trucs de FT_FeatureCollection, avec une gestion
  // de la persistance
  @Override
  public void enleveElement(Feat O) {
    super.remove(O);
    if (this.getPersistant()) {
      DataSet.db.deletePersistent(O);
    }
  }

  private static int idNouvelElement = 0;

  public static void setIdNouvelElement(int idNouvelElement) {
    Population.idNouvelElement = idNouvelElement;
  }

  public static int getIdNouvelElement() {
    return Population.idNouvelElement;
  }

  @Override
  public Feat nouvelElement() {
    return this.nouvelElement(null);
  }

  @Override
  public Feat nouvelElement(IGeometry geom) {
    try {
      Feat elem = this.getClasse().getConstructor().newInstance();
      elem.setId(++Population.idNouvelElement);
      elem.setGeom(geom);
      elem.setPopulation(this);
      super.add(elem);
      if (this.getPersistant()) {
        DataSet.db.makePersistent(elem);
      }
      return elem;
    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("ATTENTION : problème à la création d'un élément de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("            Soit la classe des éléments est non valide : " + this.getNomClasse()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("               Causes possibles : la classe n'existe pas? n'est pas compilée? est abstraite?"); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("            Soit problème à la mise à jour de l'index "); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("               Causes possibles : mise à jour automatique de l'index, mais l'objet n'a pas encore de géométrie"); //$NON-NLS-1$
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Feat nouvelElement(Class<?>[] signature, Object[] param) {
    try {
      Feat elem = this.getClasse().getConstructor(signature).newInstance(param);
      elem.setId(++Population.idNouvelElement);
      super.add(elem);
      if (this.getPersistant()) {
        DataSet.db.makePersistent(elem);
      }
      return elem;
    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("ATTENTION : problème à la création d'un élément de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("            Classe des éléments non valide : " + this.getNomClasse()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("            Causes possibles : la classe n'existe pas? n'est pas compilée?"); //$NON-NLS-1$
      return null;
    }
  }

  // ////////////////////////////////////////////////
  // Copie de population
  @Override
  public void copiePopulation(IPopulation<Feat> populationACopier) {
    this.setElements(populationACopier.getElements());
    this.setClasse(populationACopier.getClasse());
    this.setFlagGeom(populationACopier.getFlagGeom());
    this.setNom(populationACopier.getNom());
    this.setNomClasse(populationACopier.getNomClasse());
  }

  // //////////////////////////////////////////////////////////////////////////////
  @Override
  public void chargeElementsAvecMetadonnees() {
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- Chargement des elements de la population  " + this.getNom()); //$NON-NLS-1$
    }
    // MdDataSet datasetContexte = (MdDataSet)this.getDataSet();
    //
    // // je cherche via le featureType et le schema Conceptuel si on est dans
    // le cadre d'un
    // // dataset particulier. Si oui je me raccroche aux populations existantes
    // de ce dataset
    //
    //
    // if (datasetContexte==null){
    // if (this.getFeatureType().getSchema()!=null){
    // if (this.getFeatureType().getSchema().getDataset()!=null){
    // datasetContexte = this.getFeatureType().getSchema().getDataset();
    // // ce dataset avait-il déjà des populations ?
    //
    // }
    // else System.out.println("Vous êtes hors du contexte d'un MdDataSet");
    // }
    // else
    // System.out.println("Vous êtes hors du contexte d'un SchemaConceptuelJeu");
    // }
    //
    // //
    // // j'ai trouvé le MdDataSet dans lequel je travaille.
    // // Je regarde si ses populations ont été initialisées. Si oui,
    // // je prends la place de l'une d'elles. Si non, je les initialise
    // // et je prends la place de l'une d'elles.
    // //
    if (!this.getPersistant()) {
      FT_FeatureCollection.logger
          .warn("----- ATTENTION : Aucune instance n'est chargee dans la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .warn("-----             La population n'est pas persistante"); //$NON-NLS-1$
      return;
    }
    try {
      if (FT_FeatureCollection.logger.isDebugEnabled()) {
        FT_FeatureCollection.logger.debug("debut"); //$NON-NLS-1$
      }
      IFeatureCollection<Feat> coll = DataSet.db
          .loadAllFeatures((FeatureType) this.getFeatureType());
      if (FT_FeatureCollection.logger.isDebugEnabled()) {
        FT_FeatureCollection.logger.debug("milieu"); //$NON-NLS-1$
      }
      this.addUniqueCollection(coll);
      if (FT_FeatureCollection.logger.isDebugEnabled()) {
        FT_FeatureCollection.logger.debug("fin"); //$NON-NLS-1$
      }

    } catch (Exception e) {
      FT_FeatureCollection.logger
          .error("----- ATTENTION : Chargement impossible de la population " + this.getNom()); //$NON-NLS-1$
      FT_FeatureCollection.logger
          .error("-----             Sans doute un probleme avec ORACLE, ou table inexistante, ou pas de mapping "); //$NON-NLS-1$
      e.printStackTrace();
      return;
    }
    if (FT_FeatureCollection.logger.isInfoEnabled()) {
      FT_FeatureCollection.logger
          .info("-- " + this.size() + " instances chargees dans la population"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  @Override
  public boolean add(Feat feat) {
    feat.setPopulation(this);
    return super.add(feat);

  }
}
