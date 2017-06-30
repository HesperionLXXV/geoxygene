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

package fr.ign.cogit.geoxygene.style;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import fr.ign.cogit.geoxygene.api.feature.IDataSet;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.feature.DataSet;
import fr.ign.cogit.geoxygene.feature.FT_Coverage;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;

/**
 * @author Julien Perret
 */
@XmlAccessorType(XmlAccessType.FIELD)
/*
 * @XmlType(name = "", propOrder = { "name", //"description",
 * "layerFeatureConstraints", "styles" })
 */
@XmlRootElement(name = "NamedLayer")
public class NamedLayer extends AbstractLayer {
  @XmlTransient
  StyledLayerDescriptor sld = null;

  /**
   */
  public NamedLayer() {
    super();
  }

  /**
   * @param layerName
   */
  public NamedLayer(StyledLayerDescriptor sld, String layerName) {
    super();
    this.sld = sld;
    this.setName(layerName);
  }

  /**
   * @return the sld
   */
  public StyledLayerDescriptor getSld() {
    return this.sld;
  }

  /**
   * @param sld the sld to set
   */
  public final void setSld(StyledLayerDescriptor sld) {
    this.sld = sld;
    System.err.println("set layer " + this.getName() + " SLD = " + this.sld);
  }

  @SuppressWarnings("unchecked")
  @Override
  public IFeatureCollection<? extends IFeature> getFeatureCollection() {
    /*
     * TODO Récupèrer la population à partir d'un vrai DataSet Pour l'instant,
     * on utilise un singleton de DataSet qu'il faut donc avoir remplit au
     * préalable...
     */
    DataSet dataset = (this.sld != null) ? this.sld.getDataSet()
        : DataSet.getInstance();
    IFeatureCollection<IFeature> pop = (IFeatureCollection<IFeature>) dataset
        .getPopulation(this.getName());
    if (pop == null) {
      pop = new FT_FeatureCollection<IFeature>();
      IDataSet<?> dataSet = dataset.getComposant(this.getName());
      if (dataSet == null)
        return null;
      if (dataSet.getPopulations() == null)
        return null;
      for (IPopulation<? extends IFeature> population : dataSet
          .getPopulations()) {
        pop.addCollection((FT_FeatureCollection<IFeature>) population);
      }
    }
    return pop;
  }

  @Override
  public String toString() {
    String result = "NamedLayer " + this.getName() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
    for (Style style : this.getStyles()) {
      result += "\tStyle " + style + "\n"; //$NON-NLS-1$//$NON-NLS-2$
    }
    return result;
  }

  @SuppressWarnings("nls")
  @Override
  public void destroy() {
    if (this.getFeatureCollection() != null) {
      if (this.getFeatureCollection().hasSpatialIndex()) {
        this.getFeatureCollection().getSpatialIndex().clear();
      }
      for (IFeature feat : this.getFeatureCollection()) {
        if (feat instanceof FT_Coverage) {
          ((FT_Coverage) feat).coverage().dispose(true);
        }
        feat.setPopulation(null);
        feat.setGeom(null);
        feat.setFeatureType(null);
      }
      DataSet dataset = (this.sld != null) ? this.sld.getDataSet()
          : DataSet.getInstance();
      dataset.removePopulation(dataset.getPopulation(this.getName()));
    }
    this.sld = null;
  }

}
