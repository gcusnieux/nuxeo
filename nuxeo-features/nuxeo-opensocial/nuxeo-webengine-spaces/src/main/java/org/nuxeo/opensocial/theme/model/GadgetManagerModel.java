/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.theme.model;

import java.util.List;

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.theme.models.AbstractModel;

public class GadgetManagerModel extends AbstractModel {

  private List<String> categories;
  private List<GadgetDeclaration> gadgets;
  private boolean anonymous;

  public boolean isAnonymous() {
    return anonymous;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  public GadgetManagerModel(List<String> categories,
      List<GadgetDeclaration> gadgets) {
    this.categories = categories;
    this.gadgets = gadgets;
  }

  public List<String> getCategories() {
    return categories;
  }

  public List<GadgetDeclaration> getGadgets() {
    return gadgets;
  }

}