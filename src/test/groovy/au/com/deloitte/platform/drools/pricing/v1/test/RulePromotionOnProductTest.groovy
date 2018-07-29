package au.com.deloitte.platform.drools.pricing.v1.test

import static org.junit.Assert.*

import org.junit.Test
import org.kie.api.KieServices
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.StatelessKieSession

import au.com.deloitte.platform.drools.pricing.v1.fact.*

class RulePromotionOnProductTest {

	KieContainer kc
	StatelessKieSession kSession
	KieServices ks

	@Test
	public void testPromotionASuccess() {

		// setup
		ks = KieServices.Factory.get()
		// Build the KieContainer ( which by default instantiates the KBase using the ~/META-INF/kmodule.xml )
		kc = ks.newKieClasspathContainer()
		// Build a StatelessKieSession. 
		// NOTE: The argument must match the ksession name attribute from the ~/META-INF/kmodule.xml 
		kSession = kc.newStatelessKieSession 'defaultStatelessKieSession'
		// Build a Fact object
		def fact = new Product(
				productId: 'a8361037',
				department: 'ELECTRICAL_EQUIPMENT',
				basePrice: 100
				)

		// insert the fact into the rules engine's WorkingMemory and evaluate rules
		kSession.execute fact
		// assert that the discount was applied to the fact
		assertEquals 90, fact.discountedPrice, 0

	}
}
