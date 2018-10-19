package fr.ign.cogit.geoxygene.contrib.quality.estim.spatialcontext.grid.gridcat;

import java.util.Collection;

import fr.ign.cogit.geoxygene.feature.FT_Feature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Polygon;

/**
 * 
 * This software is released under the licence CeCILL
 * 
 * see Licence_CeCILL-C_fr.html see Licence_CeCILL-C_en.html
 * 
 * see <a href="http://www.cecill.info/">http://www.cecill.info/a>
 * 
 * 
 * @copyright IGN
 * 
 * @author JFGirres
 */
public class RoadArcDensityCriterion extends CellCriterion {

	
	public RoadArcDensityCriterion(GridCell cell, double poids, Number seuilBas,
			Number seuilHaut) {
		super(cell, poids, seuilBas, seuilHaut);
	}

	@Override
	public void setCategory() {
		// cas d'un crit�re entier
		double valeurD = getValeur().doubleValue();
		if(valeurD<getSeuilBas().doubleValue()){setClassif(1);}
		else if(valeurD>getSeuilHaut().doubleValue()){setClassif(3);}
		else{setClassif(2);}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValue() {
		// create un circle around this cell
		DirectPosition centre = new DirectPosition(getCellule().getxCentre(), 
				getCellule().getyCentre());
		GM_Polygon circle = GeometryFactory.buildCircle(centre, 
				getCellule().getGrille().getRadiusCellule(), 24);
		FT_FeatureCollection<FT_Feature> data = (FT_FeatureCollection<FT_Feature>)
			this.getCellule().getGrille().getData().get(UrbanGrid.FC_ROADS);
		
		Collection<FT_Feature> cellRoads = data.select(circle);
	
		
		this.setValeur(new Integer(cellRoads.size()));

		
//		if(cellRoads.size()==0){this.setValeur(new Integer(0));}
//
//		else{
//			double value = 0.0;
//			for(FT_Feature feature : cellRoads){
//				GM_Object inter = circle.intersection(feature.getGeom());
//				value += inter.length();
//			}
//			this.setValeur(new Integer((int) Math.round(value)));
//		}
	}

}
