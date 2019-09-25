---
author: dkhanolkar 
title: "Just enough Drools - Chapter 1"
tags: [apache, drools, rules, rules-engine, maven, groovy, pricing-engine]
---

[__Drools__](https://docs.jboss.org/drools/release/7.8.0.Final/drools-docs/html_single/index.html#_user_guide) is a fast, reliable expert system that allows evaluation of business rules and complex event processing.

Some classic use cases for using a rules engine : 

* Pricing Products / Calculating Premiums
* Message Validation
* Building application routing rules

In a nutshell, anything that falls under the **_if this then do that_** category, can be considered a Rule. A collection of such rules constitutes a Rules Engine. 

### So, wait how is this different from a bunch of methods / functions?

Here's how :

* You consciously invoke methods, not rules. E.g. `object.doSomething()` is how one would typically invoke a method. However, rules are evaluated by inserting a `Fact` (more on this later) into the engine and letting the engine evaluate the Rule [__Agenda__](https://docs.jboss.org/drools/release/7.3.0.Final/drools-docs/html_single/#_agenda). 
* One method call, results in a single execution. However, inserting a fact into the engine may execute 0 or many rules.
* Based on the pattern that matches, the same rule may fire once or several times. 
    
We'll build a simple Pricing Rules Engine to understand some basics.

I've used the `maven-archetype-quickstart` to create the project scaffolding. There's some Groovy shimmer (mainly for unit tests) but it should be self-explanatory. 
Check out the [__pom.xml__](https://github.com/devendra0008/drools-pricing-v1/blob/chapter-1/pom.xml) to see all the project dependencies.

## Lets get Drooling

### ...start by defining a rule

Drools Rules are defined in a `.drl` file, generally using the `MVEL` expression language (although `Java` is also a supported dialect). See [__here__](https://github.com/imona/tutorial/wiki/MVEL-Guide) for more on MVEL. 

There's a [__Drools Eclipse Plugin__](http://download.jboss.org/drools/release/7.3.0.Final/org.drools.updatesite/) for syntax / code completion which is quite useful (although a bit buggy at times). 

A `Rule` is mainly comprised of an `LHS` (the conditional part) and an `RHS` (code block to be executed).
There are [__keywords__](https://docs.jboss.org/drools/release/7.3.0.Final/drools-docs/html_single/#_keywords)  that you need be aware of before you start authoring these rules. 
A collection of these Rules builds what is called the **Knowledge Base**.

{% highlight groovy %}
package au.com.deloitte.platform.drools.pricing.v1.rules
import au.com.deloitte.platform.drools.pricing.v1.fact.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
/*
	Promotion On a Product Rule that takes 10% off items with a productId 'a8361037' or 'a8361038' and with department 'ELECTRICAL_EQUIPMENT'
*/
rule "promotion-on-product"
    when
    	// Left Hand Side (LHS) is the condition
        product: Product(
        	( productId == 'a8361037' || productId == 'a8361038' ),
        	department == 'ELECTRICAL_EQUIPMENT'
        )
    then
    	// Right Hand Side (RHS) is the code block to execute.
    	// create a logger
    	Logger logger = LoggerFactory.getLogger("promotion-on-product");
    	logger.info ("Fact before applying promotion [{}]", product); 
    	// Here we take 10% off the basePrice of the Product and insert the OfferPrice into the working memory. 
    	product.discountedPrice = product.basePrice - ( product.basePrice * 0.1 );
    	logger.info ("Fact after  applying promotion [{}]", product);
end
{% endhighlight %}

### ...and what is the **Knowledge Base**?
Knowledge Base is a collection of Knowledge definitions. E.g. all promotions on a product, all the routing rules etc. The engine uses this Knowledge Base to create the rule `Agenda`. 

You can access this Knowledge Base (`KBase`) using sessions (stateful or stateless).

**_Remember the`KBase` is built once for each application and accessed several times using the `KieSession` or `StatelessKieSession` object._**

### ...how do you define this **KBase**?
Using a `kmodule.xml` under `~/src/main/resources/META-INF`
Here's one. 

{% highlight xml %}
<kmodule xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xmlns="http://www.drools.org/xsd/kmodule">
  <kbase name="defaultKieBase" default="true" eventProcessingMode="cloud" equalsBehavior="identity" declarativeAgenda="disabled" scope="javax.enterprise.context.ApplicationScoped" packages="au.com.deloitte.platform.drools.pricing.v1.rules">
    <ksession name="defaultStatelessKieSession" type="stateless" default="true" clockType="realtime" beliefSystem="simple" scope="javax.enterprise.context.ApplicationScoped" />
  </kbase>
</kmodule>
{% endhighlight %}

**Note**: The `packages` attribute within the `kmodule.xml` will determine which rules will constitute the KnowledgeBase. Meaning, if the `package` attribute has a value `au.com.deloitte.platform.drools.pricing.v1.rules`, only rules that are defined under that package will be available for evaluation. 

### ...understanding the `Fact`?
A `Fact` is an instance of an entity. 
E.g. a `Product` with a `productId` `86252`. To let the rules engine evaluate a Rule Set, a Fact has to be inserted into what is called `WorkingMemory`.

Here's how you create a `Fact`
{% highlight groovy %}
def fact = new Product(
    productId: 'a8361037',
    department: 'ELECTRICAL_EQUIPMENT',
    basePrice: 100
)
{% endhighlight xml %}

### ...Sessions?
There are 2 types - 

#### Stateless :

* No inference
* Short lived
* Use cases 
    - Validation
    - Routing

#### Stateful :

* Long lived
* Allows iterative changes to the `Fact`, so new rules can be evaluated based on changes to a `Fact`. 
* Use cases 
    - Calculating complex pricing (e.g. Buy 1 book for $30 but if customer adds one more to the shopping cart, take 10% off the total price)
    - Stock market monitoring

Note: The `kmodule.xml` enlists the KieSessions that can be created. Here's an example of a StatelessKieSession 
{% highlight xml %}
<ksession name="defaultStatelessKieSession" type="stateless" default="true" clockType="realtime" beliefSystem="simple" scope="javax.enterprise.context.ApplicationScoped" />
{% endhighlight %}

Here's a simple unit test that will inject a `Product` `Fact` into the `WorkingMemory` and assert it was eligible for a discount.
{% highlight groovy %}
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
{% endhighlight %}

Drools provides a lot of conditional elements (e.g. collect()) using which you can define more complex LHS but for the sake of this demo, I've kept things simple. 

Authoring a Promotion Rule like this one looks promising but it’s certainly not scalable when you have to author a million of these promotions running concurrently in your organisation. 

...Drools Compiler to the rescue but we'll discuss that in Chapter 2. 

The source code can be found [__here__](https://github.com/devendra0008/drools-pricing-v1).

