/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO specifications for
 * the development and deployment of geographic (GIS) applications. It is a open source
 * contribution of the COGIT laboratory at the Institut Géographique National (the French
 * National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this library (see file LICENSE if present); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */

package fr.ign.cogit.geoxygene.util.algo;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;

import fr.ign.cogit.geoxygene.datatools.Geodatabase;
import fr.ign.cogit.geoxygene.datatools.ojb.GeodatabaseOjbFactory;
import fr.ign.cogit.geoxygene.feature.FT_Feature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Envelope;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Polygon;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_Aggregate;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiSurface;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_OrientableSurface;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;
import fr.ign.cogit.geoxygene.spatial.geomroot.GM_Object;
import fr.ign.cogit.geoxygene.util.conversion.ImgUtil;
import fr.ign.cogit.geoxygene.util.conversion.JtsGeOxygene;


/**
 * Test et comparaison des methodes geometriques de differentes bibliotheques.
 * Sont prevues : Oracle, Jts, Geos.
 * On peut facilement completer le test par de nouvelles bibliotheques
 * en creant une nouvelle classe fille de GeomAlgorithms
 * et en l'appelant dans cette application.
 * Le programme genere des images et des fichiers SVG compresses.
 * Un fichier de comparaison des temps de calcul entre les differentes bibliotheques est aussi genere.
 * On charge un nombre progressif d'objets, en utilisant le rectangle englobant (enveloppe) des couches testees
 * et en faisant varier par un facteur la taille de cette enveloppe (variables a regler au debut du code).
 * A l'aide d'une boucle, on charge progressivement de plus en plus d'objets.
 * 
 * ARNAUD 12 juillet 2005 : mise en commentaire de ce qui se rapporte à Oracle
 * pour isoler la compilation. A d�commenter pour utiliser Oracle.
 *
 * @author Thierry Badard, Arnaud Braun & Christophe Pele
 * @version 1.1
 * 
 */


@SuppressWarnings("nls")
public class CompareLib  {

	private static IndentedPrintStream out;
	private static PrintStream err=System.err;

	private Geodatabase db;

	// Alias de Connection a Oracle (dans le fichier de mapping repository_database.xml)
	private String ORACLE_ALIAS = "ORACLE_ALIAS"; //$NON-NLS-1$

	// classes a charger
	private Class<? extends FT_Feature> featClass1;
	private Class<? extends FT_Feature> featClass2;

	// emprise maximale des geometries, recuperees dans les metadonnees du chargement
	private GM_Envelope baseEnvelope;

	// les bibliotheques a tester
	private GeomAlgorithms jts;
	//private GeomAlgorithms oracle;
	private GeomAlgorithms geos;

	// les resultats des operateurs geometriques
	private Object resultJts;
	private Object resultOracle;
	private Object resultGeos;



	/* ############################################################# */
	/* variable permettant de parametrer le test, a initialiser ici  */
	/* ############################################################# */

	/* liste des algos a tester */
	private String[] algorithmsName = new String[] {
			"buffer10", //$NON-NLS-1$
			"convexHull", //$NON-NLS-1$
			"centroid", //$NON-NLS-1$
			"intersection", //$NON-NLS-1$
			"union", //$NON-NLS-1$
			"difference", //$NON-NLS-1$
			"symDifference", //$NON-NLS-1$
			"length", //$NON-NLS-1$
			"area", //$NON-NLS-1$
			"distance", //$NON-NLS-1$
			"equals", //$NON-NLS-1$
			"contains", //$NON-NLS-1$
	"intersects" }; //$NON-NLS-1$

	/* Les noms de classes de feature a charger */
	private String featClassName1 = "geoschema.feature.Topo_bati_extrait5";
	private String featClassName2 = "geoschema.feature.Topo_bati_extrait5_translate";

	/* agregats geometriques issus du chargement des classes ;
	 * definir ici si c'est des GM_MultiCurve ou des GM_MultiSurface */
	private GM_Aggregate<GM_OrientableSurface> geom1 = new GM_MultiSurface<GM_OrientableSurface>();  /*new GM_MultiCurve(); */
	private GM_Aggregate<GM_OrientableSurface> geom2 = new GM_MultiSurface<GM_OrientableSurface>();  /*new GM_MultiCurve(); */

	/* Le repertoire de sauvegarde des resultats */
	private String path = "/home/users/braun/testJtsOracle";

	/* definit les bibliotheques qu'on teste */
	private boolean testJts = true;
	private boolean testOracle = true;
	private boolean testGeos = false;

	/* facteurs pour la boucle faisant varier la taille de l'enveloppe definissant les objets a charger */
	private double min = 0.2;
	private double max = 1.0;
	private double step = 0.2;

	/* indique si on sauve les fichiers SVGZ et les images au format .png*/
	private boolean saveSvg = true;
	private boolean saveImage = false;




	// ########################################################################################
	@SuppressWarnings("unchecked")
	public CompareLib()
	{
		out = new IndentedPrintStream(System.out);
		out.println("Begin!");

		out.print("connecting database ... ");
		this.db= GeodatabaseOjbFactory.newInstance(this.ORACLE_ALIAS);
		out.println("OK");

		if (this.testJts) this.jts=new JtsAlgorithms();
		//        if (testOracle) oracle=new OracleAlgorithms(db,0.0000000005);
		if (this.testGeos) this.geos=new GeosAlgorithms();

		try {
			this.featClass1=(Class<? extends FT_Feature>) Class.forName(this.featClassName1);
			this.featClass2=(Class<? extends FT_Feature>) Class.forName(this.featClassName2);
		} catch (Exception e) {
			err.println("## Classes geographiques non trouvées ##");
			System.exit(0);
		}
		out.println("test class 1 : "+this.featClass1.getName());
		out.println("test class 2 : "+this.featClass2.getName());

		out.println("Computing envelope...");

		try {
			GM_Envelope baseEnvelope1 = this.db.getMetadata(this.featClass1).getEnvelope();
			GM_Envelope baseEnvelope2 = this.db.getMetadata(this.featClass2).getEnvelope();
			// on prend l'enveloppe maximale des deux enveloppes
			baseEnvelope1.expand(baseEnvelope2);
			this.baseEnvelope = (GM_Envelope)baseEnvelope1.clone();
		} catch (Exception e) {
			err.println("## Problemes en recuperant l'emprise des couches dans les metadonnees ##");
			System.exit(0);
		}

		out.println("envelope : "+this.baseEnvelope);
		out.println("test path : "+this.path);

	}


	// ########################################################################################
	private void initFiles(String[] outDirPathTab, PrintStream[] dataOutTab) throws Exception
	{
		out.indentRight();

		int numAlgos=this.algorithmsName.length;

		for (int i=0; i<numAlgos; i++) {
			String algoName=this.algorithmsName[i];
			String outDirPath=outDirPathTab[i]=this.path+"/"+algoName+"/"; //$NON-NLS-1$ //$NON-NLS-2$
			File outDirFile=new File(outDirPath);
			outDirFile.mkdirs();
			PrintStream dataOut=dataOutTab[i]=new PrintStream(new FileOutputStream(outDirPath+"test.dat"));

			dataOut.println("# vim:ts=10");
			dataOut.println("# Les temps sont donnés en millisecondes");
			dataOut.println();
			String datLine;
			datLine=""+"factor\t"+"size\t";

			if (this.testJts) datLine=datLine+"jts\t";
			if (this.testOracle) datLine=datLine+"oracle\t";
			if (this.testGeos) datLine=datLine+"geos\t";
			if (this.testJts && this.testOracle ) datLine=datLine+"jts/oracle\t";
			if (this.testGeos && this.testOracle ) datLine=datLine+"geos/oracle\t";

			dataOut.println(datLine);
			dataOut.println();
		}
		out.indentLeft();
	}


	// ########################################################################################
	private void loadInputGeom(double factor) throws Exception
	{
		out.indentRight();

		long time;
		time=time();

		GM_Envelope envelope=(GM_Envelope)this.baseEnvelope.clone();
		envelope.expandBy(factor);
		GM_Polygon bbox=new GM_Polygon(envelope);

		FT_FeatureCollection<?> featList1=this.db.loadAllFeatures(this.featClass1, bbox);
		FT_FeatureCollection<?> featList2=this.db.loadAllFeatures(this.featClass2, bbox);

		// creation des agregats
		this.geom1.clear();
		Iterator<? extends FT_Feature> iterator = featList1.iterator();
		while (iterator.hasNext()) this.geom1.add((GM_OrientableSurface)iterator.next().getGeom());
		this.geom2.clear();
		iterator = featList2.iterator();
		while (iterator.hasNext()) this.geom2.add((GM_OrientableSurface)iterator.next().getGeom());

		out.println("Creation: "+(time()-time)/1000.+" seconds");
		out.println("Size of geom1: "+this.geom1.size());
		out.println("Size of geom2: "+this.geom2.size());

		out.println("Envelope of geom1:");
		out.indentRight();
		out.println(this.geom1.envelope().toString());
		out.indentLeft();

		out.println("Envelope of geom2:");
		out.indentRight();
		out.println(this.geom2.envelope().toString());
		out.indentLeft();

		out.indentLeft();
	}


	// ########################################################################################
	private void saveImagesCm(int nbParameters, String outDirPath, String factorString, int width, int height) throws Exception
	{
		out.indentRight();

		Color colorG1=		Color.GREEN;
		Color colorG2=		Color.BLUE;
		Color colorResult=	Color.RED;
		Color bg=			Color.WHITE;

		GM_Object[] geomsJts;
		GM_Object[] geomsOracle;
		GM_Object[] geomsGeos;

		Color[] colors;
		if (nbParameters==1) {
			geomsJts= new GM_Object[] {(GM_Object)this.resultJts,this.geom1};
			geomsOracle= new GM_Object[] {(GM_Object)this.resultOracle,this.geom1};
			geomsGeos= new GM_Object[] {(GM_Object)this.resultGeos,this.geom1};
			colors=new Color[] {colorResult,colorG1};
		} else /*if (nbParameters==2) */ {
			geomsJts= new GM_Object[] {(GM_Object)this.resultJts,this.geom1,this.geom2};
			geomsOracle= new GM_Object[] {(GM_Object)this.resultOracle,this.geom1,this.geom2};
			geomsGeos= new GM_Object[] {(GM_Object)this.resultGeos,this.geom1,this.geom2};
			colors=new Color[] {colorG1,colorG2,colorResult};
		}

		if (this.testJts) {
			String pfx1=outDirPath+"jts_"+factorString;
			String imgPath1=pfx1+".png";
			String svgzPath1=pfx1+".svg.z";
			out.println("Saving "+pfx1+"...");
			if (this.saveImage) ImgUtil.saveImage(geomsJts,imgPath1,colors,bg,width,height);
			if (this.saveSvg) ImgUtil.saveSvgz(geomsJts,svgzPath1,colors,bg,width,height);
		}

		if (this.testOracle) {
			String pfx1=outDirPath+"oracle_"+factorString;
			String imgPath1=pfx1+".png";
			String svgzPath1=pfx1+".svg.z";
			out.println("Saving "+pfx1+"...");
			if (this.saveImage) ImgUtil.saveImage(geomsOracle,imgPath1,colors,bg,width,height);
			if (this.saveSvg) ImgUtil.saveSvgz(geomsOracle,svgzPath1,colors,bg,width,height);
		}

		if (this.testGeos) {
			String pfx1=outDirPath+"geos_"+factorString;
			String imgPath1=pfx1+".png";
			String svgzPath1=pfx1+".svg.z";
			out.println("Saving "+pfx1+"...");
			if (this.saveImage) ImgUtil.saveImage(geomsGeos,imgPath1,colors,bg,width,height);
			if (this.saveSvg) ImgUtil.saveSvgz(geomsGeos,svgzPath1,colors,bg,width,height);
		}

		out.indentLeft();
	}


	// ########################################################################################
	private long launchJts(String realAlgoName, Class<?>[] algoParamTypes, Object[] algoParameters) throws Exception
	{
		try {
			out.indentRight();

			Class<JtsAlgorithms> jtsAlgoClass=JtsAlgorithms.class;
			Method jtsAlgo=jtsAlgoClass.getMethod(realAlgoName,algoParamTypes);

			out.println("JTS...");
			long time=time();
			try {
				this.resultJts=jtsAlgo.invoke(this.jts,algoParameters);
			} catch (InvocationTargetException e) {
				Throwable cause=e.getCause();
				cause.printStackTrace();
				if (cause instanceof TopologyException) {
					CoordinateSequence jtsCoord= CoordinateArraySequenceFactory.instance().
					create(new Coordinate[] {((TopologyException)cause).getCoordinate()});
					DirectPosition geOxyCoord=JtsGeOxygene.makeDirectPosition(jtsCoord);
					this.resultJts=new GM_Point(geOxyCoord);
				}
			}
			time=time()-time;
			out.println("JTS: "+(time/1000.)+" seconds");
			long jtsTime=time;

			out.indentLeft();
			return jtsTime;

		} catch (Exception e) {
			e.printStackTrace();
			return 10000000;
		}
	}


	// ########################################################################################
	/*    private long launchOracle(String realAlgoName, Class[] algoParamTypes, Object[] algoParameters)
    {
        try {
        	out.indentRight();

        	Class oracleAlgoClass=OracleAlgorithms.class;
        	Method oracleAlgo=oracleAlgoClass.getMethod(realAlgoName,algoParamTypes);

        	out.println("Oracle...");
        	long time=time();
        	resultOracle=oracleAlgo.invoke(oracle,algoParameters);
        	time=time()-time;
        	out.println("Oracle: "+(time/1000.)+" seconds");
        	long oracleTime=time;

        	out.indentLeft();

        	return oracleTime;

        } catch (Exception e) {
            e.printStackTrace();
            return 10000000;
        }
    }
	 */

	// ########################################################################################
	private long launchGeos(String realAlgoName, Class<?>[] algoParamTypes, Object[] algoParameters) throws Exception
	{
		try {
			out.indentRight();

			Class<GeosAlgorithms> geosAlgoClass=GeosAlgorithms.class;
			Method geosAlgo=geosAlgoClass.getMethod(realAlgoName,algoParamTypes);

			out.println("Geos...");
			long time=time();
			this.resultGeos=geosAlgo.invoke(this.geos,algoParameters);
			time=time()-time;
			out.println("Geos: "+(time/1000.)+" seconds");
			long geosTime=time;

			out.indentLeft();

			return geosTime;

		} catch (Exception e) {
			e.printStackTrace();
			return 10000000;
		}
	}


	// ########################################################################################
	private void launchAlgorithms(String[] outDirPathTab,	PrintStream[] dataOutTab, String factorString) throws Exception
	{
		out.indentRight();

		Class<?>[] algoParamTypes=null;
		Object[] algoParameters=null;
		Class<?> returnType=null;
		Class<?> geomAlgoClass=GeomAlgorithms.class;
		Method[] geomAlgos=geomAlgoClass.getMethods();
		int nbParameters=0;

		for (int i=0; i<this.algorithmsName.length; i++) {
			PrintStream dataOut=dataOutTab[i];
			String outDirPath=outDirPathTab[i];
			String realAlgoName=this.algorithmsName[i];

			out.println(realAlgoName);

			/*-- ...Choix des parametres ---------------------------------*/
			for (int j=0; j<geomAlgos.length; j++) {
				Method geomAlgo = geomAlgos[j];
				if (geomAlgo.getName().equals(realAlgoName)) {
					nbParameters = geomAlgo.getParameterTypes().length;
					returnType = geomAlgo.getReturnType();
					break;
				}
			}
			if (nbParameters==1)  {
				algoParamTypes = new Class[] {GM_Object.class};
				algoParameters = new Object[] {this.geom1};
			} else if (nbParameters==2){
				algoParamTypes = new Class[] {GM_Object.class, GM_Object.class};
				algoParameters = new Object[] {this.geom1, this.geom2};
			} else {
				err.println(" ## Probleme dans le choix des parametres ## ");
			}


			/*-- ...Geos -------------------------------------------------*/
			long geosTime=0;
			if (this.testGeos) geosTime=launchGeos(realAlgoName,algoParamTypes,algoParameters);

			/*-- ...Jts ----------------------------------------------------*/
			long jtsTime=0;
			if (this.testJts) jtsTime=launchJts(realAlgoName,algoParamTypes,algoParameters);

			/*-- ...Oracle -------------------------------------------------*/
			long oracleTime=0;
			//           if (testOracle) oracleTime=launchOracle(realAlgoName,algoParamTypes,algoParameters);

			/*-- Print line in testresult file ---------------------------*/
			String datLine;
			datLine=""+factorString+'\t'+this.geom1.size()+'\t';
			if (nbParameters==2) datLine += this.geom2.size()+'\t';

			if (this.testJts) datLine=datLine+jtsTime+'\t';
			if (this.testOracle) datLine=datLine+oracleTime+'\t';
			if (this.testGeos) datLine=datLine+geosTime+'\t';
			if (this.testJts && this.testOracle ) datLine=datLine+((double)jtsTime)/oracleTime+'\t';
			if (this.testGeos && this.testOracle ) datLine=datLine+((double)geosTime)/oracleTime;

			dataOut.println(datLine);

			/*-- Make images ----------------------------------------*/
			if (returnType==GM_Object.class) {
				out.indentRight();
				out.println("Saving images...");
				int width = (int) this.baseEnvelope.width()/100;
				int height = (int) this.baseEnvelope.length()/100;
				saveImagesCm(nbParameters,outDirPath,factorString,width,height);
				out.indentLeft();
			} else {
				out.indentRight();
				out.indentRight();
				if (this.testJts) out.println("result jts : "+this.resultJts);
				if (this.testOracle) out.println("result oracle : "+this.resultOracle);
				if (this.testGeos) out.println("result geos : "+this.resultGeos);
				out.indentLeft();
				out.indentLeft();
			}
		}

		out.indentLeft();
	}


	// ########################################################################################
	public void run() throws Exception
	{
		out.indentRight();

		/*-- Create output directories and testresult files --------------*/
		String[] outDirPathTab=new String[this.algorithmsName.length];
		PrintStream[] dataOutTab=new PrintStream[outDirPathTab.length];
		initFiles(outDirPathTab,dataOutTab);

		/* boucle reglant la taille des envelopes pour le chargement des objets */
		for (double factor=this.min; factor<=this.max; factor+=this.step) {
			DecimalFormat format=new DecimalFormat();
			format.setMaximumFractionDigits(2);
			String factorString=format.format(factor);
			out.println("Factor="+factorString);

			/*-- Create input geometries ---------------------------------*/
			out.println("Creating input geometries...");
			loadInputGeom(factor);

			/*-- Launch algorithms ---------------------------------------*/
			out.println("Launching algorithms...");
			launchAlgorithms(outDirPathTab,dataOutTab,factorString);
		}
		out.indentLeft();
	}


	// ########################################################################################
	public static void main(String[] args) throws Exception
	{
		CompareLib testApp= new CompareLib();
		testApp.run();
	}


	// ########################################################################################
	private long time() {return System.currentTimeMillis();}


	// ########################################################################################
	private class IndentedPrintStream extends PrintStream {
		private String indent=""; //$NON-NLS-1$

		public IndentedPrintStream(OutputStream out)
		{
			super(out);
		}

		public IndentedPrintStream(OutputStream out, boolean autoFlush)
		{
			super(out, autoFlush);
		}

		public IndentedPrintStream(
				OutputStream out,
				boolean autoFlush,
				String encoding)
		throws UnsupportedEncodingException
		{
			super(out, autoFlush, encoding);
		}

		@Override
		public void println(String x)
		{
			StringTokenizer tkz=new StringTokenizer(x, "\n"); //$NON-NLS-1$
			while (tkz.hasMoreTokens()) {
				String line=tkz.nextToken();
				super.print(this.indent);
				super.println(line);
			}
		}

		public void indentRight() {this.indent+="\t";} //$NON-NLS-1$
		public void indentLeft() {this.indent=this.indent.replaceFirst("\t$","");}  //$NON-NLS-1$//$NON-NLS-2$
	}

}
