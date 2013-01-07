/*******************************************************************************
 * This software is released under the licence CeCILL
 *  
 *  see Licence_CeCILL-C_fr.html see Licence_CeCILL-C_en.html
 *  
 *  see <a href="http://www.cecill.info/">http://www.cecill.info/a>
 *  
 *  @copyright IGN
 ******************************************************************************/
package fr.ign.cogit.cartagen.util.multicriteriadecision.ranking.electre3;

import java.util.ArrayList;
import java.util.HashSet;

public class ELECTREIIIPreOrder {
  ////////////////////////////////////////////
  //                Fields                  //
  ////////////////////////////////////////////

  // All static fields //

  // Public fields //

  // Protected fields //

  // Package visible fields //

  // Private fields //
  private ArrayList<HashSet<ELECTREIIIAction>> preOrder;

  ////////////////////////////////////////////
  //           Static methods               //
  ////////////////////////////////////////////

  ////////////////////////////////////////////
  //           Public methods               //
  ////////////////////////////////////////////

  // Public constructors //
  public ELECTREIIIPreOrder(ArrayList<HashSet<ELECTREIIIAction>> preOrder) {
    super();
    this.preOrder = preOrder;
  }

  // Getters and setters //
  public void setPreOrder(ArrayList<HashSet<ELECTREIIIAction>> preOrder) {
    this.preOrder = preOrder;
  }

  public ArrayList<HashSet<ELECTREIIIAction>> getPreOrder() {
    return preOrder;
  }

  // Other public methods //
  public boolean outranks(ELECTREIIIAction a, ELECTREIIIAction b){
    int rankA = getActionOrder(a);
    int rankB = getActionOrder(b);
    return (rankA < rankB);
  }
  
  public int difference(ELECTREIIIAction a, ELECTREIIIAction b){
    int rankA = getActionOrder(a);
    int rankB = getActionOrder(b);
    return Math.abs(rankA - rankB);
  }

  ////////////////////////////////////////////
  //           Protected methods            //
  ////////////////////////////////////////////

  ////////////////////////////////////////////
  //         Package visible methods        //
  ////////////////////////////////////////////

  //////////////////////////////////////////
  //           Private methods            //
  //////////////////////////////////////////
  private int getActionOrder(ELECTREIIIAction a){
    for(int i=0;i<preOrder.size();i++){
      if(preOrder.get(i).contains(a)) return i+1;
    }
    return 0;
  }
}
