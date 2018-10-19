/*******************************************************************************
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO
 * specifications for the development and deployment of geographic (GIS)
 * applications. It is a open source contribution of the COGIT laboratory at the
 * Institut Géographique National (the French National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library (see file LICENSE if present); if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 *******************************************************************************/

package fr.ign.cogit.geoxygene.appli.render.texture;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IEnvelope;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.appli.gl.BinaryGradientImage;
import fr.ign.cogit.geoxygene.appli.gl.BinaryGradientImage.BinaryGradientImageParameters;
import fr.ign.cogit.geoxygene.appli.task.TaskState;
import fr.ign.cogit.geoxygene.style.texture.BinaryGradientImageDescriptor;

/**
 * @author JeT This Task generates a GradientImage and transform it into a
 *         gradient image with false colors
 */
public class GradientTextureTask extends AbstractTextureTask<BinaryGradientTexture> {

    private static final Logger logger = Logger
            .getLogger(GradientTextureTask.class.getName()); // logger
    // texture descriptor (from style)
    private BinaryGradientImageDescriptor textureDescriptor = null;
    private BinaryGradientTexture gradientTexture = null;
    private IFeatureCollection<IFeature> featureCollection = null;

    public static final double CM_PER_INCH = 2.540005;
    public static final double M_PER_INCH = CM_PER_INCH / 100.;

    /**
     * @param texture
     */
    public GradientTextureTask(URI identifier,
            BinaryGradientImageDescriptor textureDescriptor,
            IFeatureCollection<IFeature> featureCollection) {
        super("GradientTexture" + identifier.toString());
        this.textureDescriptor = textureDescriptor;
        this.gradientTexture = new BinaryGradientTexture();
        this.featureCollection = featureCollection;
        this.id = identifier;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.ign.cogit.geoxygene.appli.task.Task#isProgressable()
     */
    @Override
    public boolean isProgressable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.ign.cogit.geoxygene.appli.task.Task#isPausable()
     */
    @Override
    public boolean isPausable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.ign.cogit.geoxygene.appli.task.Task#isStoppable()
     */
    @Override
    public boolean isStoppable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        this.setState(TaskState.WAITING);
        this.setState(TaskState.INITIALIZING);
        IEnvelope envelope = this.featureCollection.getEnvelope();

        double mapScale = this.textureDescriptor.getMapScale();
        int textureWidth = (int) (envelope.width()
                * this.textureDescriptor.getTextureResolution() / (M_PER_INCH * mapScale));
        int textureHeight = (int) (envelope.length()
                * this.textureDescriptor.getTextureResolution() / (M_PER_INCH * mapScale));

        List<IPolygon> polygons = new ArrayList<IPolygon>();
        // convert the multisurface as a collection of polygons
        for (IFeature feature : this.featureCollection) {
            if (feature.getGeom() instanceof IMultiSurface<?>) {
                IMultiSurface<?> multiSurface = (IMultiSurface<?>) feature
                        .getGeom();
                for (IOrientableSurface surface : multiSurface.getList()) {
                    if (surface instanceof IPolygon) {
                        IPolygon polygon = (IPolygon) surface;
                        polygons.add(polygon);
                    } else {
                        logger.error("Distance Field Parameterizer does handle multi surfaces containing only polygons, not "
                                + surface.getClass().getSimpleName());
                    }
                }

            } else {
                System.err.println("geometry type not handled : "
                        + feature.getGeom().getClass().getSimpleName());
            }
        }

        this.setState(TaskState.RUNNING);
        try {
            BinaryGradientImageParameters params = new BinaryGradientImageParameters(
                    textureWidth, textureHeight, polygons, envelope,
                    this.textureDescriptor);
            BinaryGradientImage texImage = BinaryGradientImage
                    .generateBinaryGradientImage(params);
            if (texImage == null) {
                this.setError(new IllegalStateException(
                        "GradientTextureImage returned a null texture"));
                this.setState(TaskState.ERROR);
                return;
            }
            this.gradientTexture.setTextureImage(texImage);
            // BufferedImage image = GradientTextureImage
            // .toBufferedImageDistanceStrip(texImage, 20);
            // BufferedImage image = GradientTextureImage
            // .toBufferedImageUV(texImage);
            BufferedImage image = BinaryGradientImage.toBufferedImageDistance(
                    texImage, 
                    this.textureDescriptor.getColor1(), 
                    this.textureDescriptor.getColor2(), 
                    this.textureDescriptor.getBorderColor());

            // Flip the image vertically
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -image.getHeight(null));
            AffineTransformOp op = new AffineTransformOp(tx,
                    AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            image = op.filter(image, null);
            this.getTexture().setTextureImage(image);
            this.setState(TaskState.FINISHED);
        } catch (Exception e) {
            this.setError(e);
            this.setState(TaskState.ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public int getTextureWidth() {
        return this.getTexture().getTextureWidth();
    }

    @Override
    public int getTextureHeight() {
        return this.getTexture().getTextureHeight();
    }

    @Override
    public BinaryGradientTexture getTexture() {
        return this.gradientTexture;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "GradientTextureTask [textureDescriptor=" + this.textureDescriptor
                + ", GradientTexture=" + this.gradientTexture + ", toString()="
                + super.toString() + "]";
    }

}
