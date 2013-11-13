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

package fr.ign.cogit.geoxygene.appli.gl;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.vecmath.Point2d;

import org.lwjgl.opengl.GL11;

/**
 * @author JeT Basic texture returns the coordinates equal to the provided
 *         point. It manages a texture by its filename and is a good base class
 *         for inheritance
 */
public class BasicTexture implements Texture {

  private int textureId = -1;
  private String textureFilename = null;
  private BufferedImage textureImage = null;
  private double minX = 0; // range of point coordinates in world space
  private double maxX = 1; // range of point coordinates in world space
  private double minY = 0; // range of point coordinates in world space
  private double maxY = 1; // range of point coordinates in world space

  /**
   * Constructor
   */
  public BasicTexture() {
  }

  /**
   * Constructor
   * @param textureFilename
   */
  public BasicTexture(final String textureFilename) {
    super();
    this.setTextureFilename(textureFilename);
  }

  /**
   * @return the generated texture id
   */
  protected final Integer getTextureId() {
    if (this.textureFilename == null) {
      return null;
    }
    if (this.textureId < 0) {
      this.textureId = GLTools.loadTexture(this.getTextureImage());

    }
    return this.textureId;
  }

  /**
   * @return the textureFilename
   */
  public final String getTextureFilename() {
    return this.textureFilename;
  }

  /**
   * @return the textureImage
   */
  public final BufferedImage getTextureImage() {
    if (this.textureImage == null) {
      try {
        this.textureImage = GLTools.loadImage(this.textureFilename);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return this.textureImage;
  }

  /**
   * @return the texture image width (in pixels)
   */
  @Override
  public final int getTextureWidth() {
    return this.getTextureImage().getWidth();
  }

  /**
   * @return the texture image height (in pixels)
   */
  @Override
  public final int getTextureHeight() {
    return this.getTextureImage().getHeight();
  }

  /**
   * @param textureFilename the textureFilename to set
   */
  public final void setTextureFilename(final String textureFilename) {
    this.textureFilename = textureFilename;
    this.textureId = -1;
    this.textureImage = null;
  }

  /**
   * initialize the texture rendering
   */
  @Override
  public boolean initializeRendering() {
    Integer texIndex = this.getTextureId();
    if (texIndex == null) {
      GL11.glDisable(GL_TEXTURE_2D);
      return false;
    }
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBindTexture(GL_TEXTURE_2D, texIndex);
    GL11.glColor4f(1f, 1f, 1f, 1f);
    GL11.glDepthMask(false);
    // glEnable(GL11.GL_BLEND);
    // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * fr.ign.cogit.geoxygene.appli.gl.Texture#vertexCoordinates(javax.vecmath
   * .Point2d)
   */
  @Override
  public Point2d vertexCoordinates(final Point2d p) {
    return this.vertexCoordinates(p.x, p.y);
  }

  /*
   * (non-Javadoc)
   * 
   * @see fr.ign.cogit.geoxygene.appli.gl.Texture#vertexCoordinates(double,
   * double)
   */
  @Override
  public Point2d vertexCoordinates(final double x, final double y) {
    Point2d p = new Point2d((x - this.minX) / (this.maxX - this.minX),
        (y - this.minY) / (this.maxY - this.minY));
    return p;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fr.ign.cogit.geoxygene.appli.gl.Texture#finalizeRendering()
   */
  @Override
  public void finalizeRendering() {

  }

  @Override
  public void setRange(final double xmin, final double ymin, final double xmax,
      final double ymax) {
    this.minX = xmin;
    this.maxX = xmax;
    this.minY = ymin;
    this.maxY = ymax;
  }

  /**
   * @return the minX
   */
  @Override
  public double getMinX() {
    return this.minX;
  }

  /**
   * @return the maxX
   */
  @Override
  public double getMaxX() {
    return this.maxX;
  }

  /**
   * @return the minY
   */
  @Override
  public double getMinY() {
    return this.minY;
  }

  /**
   * @return the maxY
   */
  @Override
  public double getMaxY() {
    return this.maxY;
  }

}