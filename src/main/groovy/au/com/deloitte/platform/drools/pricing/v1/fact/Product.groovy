package au.com.deloitte.platform.drools.pricing.v1.fact

import groovy.transform.Canonical
import groovy.transform.ToString

/**
 * @author Dev K
 * Drools Fact definition
 *
 */
@Canonical
@ToString(includeNames=true, includeFields=true)
class Product {
	public String productId, department
	public double basePrice, discountedPrice
}