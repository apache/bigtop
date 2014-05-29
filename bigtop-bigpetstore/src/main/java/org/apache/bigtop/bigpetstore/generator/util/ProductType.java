package org.apache.bigtop.bigpetstore.generator.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ProductType {
  DOG, CAT, TURTLE, FISH, DUCK;

  private List<Product> products;

  public List<Product> getProducts() {
    if(products == null) {
      generateProductList();
    }
    return products;
  }

  private void generateProductList() {
    List<Product> products = new ArrayList<>();
    for(Product p : Product.values()) {
      if(p.productType == this) {
        products.add(p);
      }
    }
    this.products = Collections.unmodifiableList(products);
  }

}
