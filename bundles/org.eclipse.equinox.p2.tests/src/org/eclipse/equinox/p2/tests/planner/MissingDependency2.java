/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MissingDependency2 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		IRequiredCapability[] reqA = new IRequiredCapability[1];
		reqA[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqA);

		//Missing optional dependency
		IRequiredCapability[] req = new IRequiredCapability[1];
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		b1 = createIU("B", Version.create("1.0.0"), req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testContradiction() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
	}
}