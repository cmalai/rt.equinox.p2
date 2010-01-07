/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.perf;

import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * 
 */
public class ProvisioningPerformanceTest extends AbstractProvisioningTest {

	protected IInstallableUnit generateIU(int i) {
		MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setId("org.eclipse.someiu" + i);
		desc.setVersion(Version.createOSGi(1, 1, i));
		return MetadataFactory.createInstallableUnit(desc);
	}

}