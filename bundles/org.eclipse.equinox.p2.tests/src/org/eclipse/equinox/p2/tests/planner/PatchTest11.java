/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class PatchTest11 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnitPatch p1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", new Version("1.0.0"), new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true, true)});
		b1 = createIU("B", new Version(1, 0, 0), true);
		b2 = createIU("B", new Version(1, 2, 0), true);
		RequirementChange change = new RequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), "foo=bar", false, false, true));
		p1 = createIUPatch("P", new Version("1.0.0"), true, new RequirementChange[] {change}, new RequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, p1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallBogusInstallFilterInPatch() {
		//Verify that a1 installs properly
		//		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		//		req1.addInstallableUnits(new IInstallableUnit[] {a1});
		//		ProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		//		assertTrue(IStatus.ERROR != plan1.getStatus().getSeverity());
		//		assertNoOperand(plan1, p1);
		//		assertNoOperand(plan1, b2);
		//		assertNoOperand(plan1, p1);
		//		assertInstallOperand(plan1, a1);
		//		assertInstallOperand(plan1, b1);

		//Try to install a1 and p1 optionally
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		req2.setInstallableUnitInclusionRules(p1, PlannerHelper.createOptionalInclusionRule(p1));
		ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertNoOperand(plan2, p1);
		assertNoOperand(plan2, b2);
		assertNoOperand(plan2, p1);
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, b1);

		//Try to install a1 and p1. This should fail because the patch adds an invalid filter 
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		ProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertTrue(IStatus.ERROR == plan3.getStatus().getSeverity());

	}
}
