package fr.ign.cogit.geoxygene.util.algo.geomstructure;

import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineSegment;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.contrib.geometrie.Angle;
import fr.ign.cogit.geoxygene.contrib.geometrie.Vecteur;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineSegment;

public class Vector2D extends Vecteur {

  @Override
  public boolean equals(Object obj) {
    if (this.getX() != ((Vector2D) obj).getX()) {
      return false;
    }
    if (this.getY() != ((Vector2D) obj).getY()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return this.coord.hashCode();
  }

  @Override
  public String toString() {
    StringBuffer text = new StringBuffer("Vector : (" + this.getX() + ", "
        + this.getY());
    return text.append(")").toString();
  }

  public Vector2D(double x, double y) {
    super(x, y, 0);
  }

  public Vector2D() {
    super();
  }

  public Vector2D(IDirectPosition dp1, IDirectPosition dp2) {
    super(dp1, dp2);
  }

  /**
   * build a vector with the coordinates contained in the direct position
   * @param dp1
   */
  public Vector2D(IDirectPosition dp1) {
    super(dp1);
  }

  /**
   * build a vector from an angle and a norm
   * @param angle
   */
  public Vector2D(Angle angle, double norm) {
    super();
    double x = norm * Math.cos(angle.getValeur());
    double y = norm * Math.sin(angle.getValeur());
    this.coord = new DirectPosition(x, y);
  }
  
  public Vector2D add(Vector2D v) {
    return new Vector2D(this.ajoute(v).getCoord());
  }

  @Override
  public Vecteur ajoute(Vecteur v1) {
    return super.ajoute(v1);
  }

  public Vector2D copy() {
    return new Vector2D(this.getX(), this.getY());
  }

  public void scalarMultiplication(double lambda) {
    this.setX(this.getX() * lambda);
    this.setY(this.getY() * lambda);
  }

  @Override
  public ILineString translate(ILineString l) {
    ILineString l2 = super.translate(l);
    if (l instanceof ILineSegment) {
      return new GM_LineSegment(l2.coord());
    }
    return l2;
  }

  /**
   * Gives the angle between this and a vector v. Gives the correct sign of the
   * angle between -Pi and Pi
   * 
   * @param v the second vector of the angle
   * @return the angle in radians in [-Pi,Pi]
   * @author GTouya
   */
  public double vectorAngle(Vector2D v) {
    double angle = 0;
    angle = Math.acos(this.prodScalaire(v) / (this.norme() * v.norme()));
    if (Double.isNaN(angle)) {
      angle = 0;
    }
    double sign = this.getX() * v.getY() - this.getY() * v.getX();
    if (sign < 0.0) {
      angle = -angle;
    }
    return angle;
  }

  /**
   * Gives the angle between this and a vector v between 0 and Pi.
   * 
   * @param v the second vector of the angle
   * @return the angle in radians in [0,Pi]
   * @author GTouya
   */
  public double vectorAngle0ToPi(Vector2D v) {
    double angle = 0;
    angle = Math.acos(this.prodScalaire(v) / (this.norme() * v.norme()));
    if (Double.isNaN(angle)) {
      angle = 0;
    }
    return angle;
  }

  /**
   * Determine if the vector is the null vector (and not equal to null!!!)
   * 
   * @return
   * @author GTouya
   */
  public boolean isNull() {
    return this.equals(new Vector2D(0.0, 0.0));
  }
  
  /**
   * Gets a new vector similar to this but with a new norm value.
   *  
   * @param newNorm
   * @return
   * @author GTouya
   */
  public Vector2D changeNorm(double newNorm){
    Vector2D vect = new Vector2D(this.getX(),this.getY());
    vect.normalise();
    vect.scalarMultiplication(newNorm);
    return vect;
  }
}