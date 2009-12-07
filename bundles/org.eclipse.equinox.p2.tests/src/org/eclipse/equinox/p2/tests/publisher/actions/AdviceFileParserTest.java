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
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.LDAPQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.publisher.eclipse.AdviceFileParser;

public class AdviceFileParserTest extends TestCase {
	public void testNoAdvice() {
		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, Collections.EMPTY_MAP);
		parser.parse();
		assertNull(parser.getAdditionalInstallableUnitDescriptions());
		assertNull(parser.getProperties());
		assertNull(parser.getProvidedCapabilities());
		assertNull(parser.getRequiredCapabilities());
		assertNull(parser.getTouchpointInstructions());
	}

	public void testAdviceVersion() {
		Map map = new HashMap();
		map.put("advice.version", "1.0");
		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();

		map.put("advice.version", "999");
		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		try {
			parser.parse();
		} catch (IllegalStateException e) {
			return;
		}
		fail("expected version parse problem");
	}

	public void testPropertyAdvice() {
		Map map = new HashMap();
		map.put("properties.0.name", "testName1");
		map.put("properties.0.value", "testValue1");
		map.put("properties.1.name", "testName2");
		map.put("properties.1.value", "testValue2");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		assertEquals("testValue1", parser.getProperties().getProperty("testName1"));
		assertEquals("testValue2", parser.getProperties().getProperty("testName2"));
	}

	public void testProvidesAdvice() {
		Map map = new HashMap();
		map.put("provides.0.namespace", "testNamespace1");
		map.put("provides.0.name", "testName1");
		map.put("provides.0.version", "1.2.3.$qualifier$");

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IProvidedCapability[] capabilities = parser.getProvidedCapabilities();
		assertEquals(1, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(Version.create("1.2.3.v20090909"), capabilities[0].getVersion());

		map.put("provides.1.namespace", "testNamespace2");
		map.put("provides.1.name", "testName2");
		map.put("provides.1.version", "$version$");

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		capabilities = parser.getProvidedCapabilities();
		assertEquals(2, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(Version.create("1.2.3"), capabilities[0].getVersion());
		assertEquals("testNamespace2", capabilities[1].getNamespace());
		assertEquals("testName2", capabilities[1].getName());
		assertEquals(Version.emptyVersion, capabilities[1].getVersion());
	}

	public void testRequiresAdvice() {
		Map map = new HashMap();
		map.put("requires.0.namespace", "testNamespace1");
		map.put("requires.0.name", "testName1");
		map.put("requires.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("requires.0.greedy", Boolean.TRUE.toString());
		map.put("requires.0.optional", Boolean.TRUE.toString());
		map.put("requires.0.multiple", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IRequirement[] reqs = parser.getRequiredCapabilities();
		assertEquals(1, reqs.length);
		assertEquals("testNamespace1", ((IRequiredCapability) reqs[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) reqs[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3.v20090909, 2)"), ((IRequiredCapability) reqs[0].getMatches()).getRange());

		map.put("requires.1.namespace", "testNamespace2");
		map.put("requires.1.name", "testName2");
		map.put("requires.1.range", "$version$");
		map.put("requires.1.greedy", Boolean.FALSE.toString());
		map.put("requires.1.optional", Boolean.FALSE.toString());
		//default 
		//		map.put("requires.1.multiple", Boolean.FALSE.toString());

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		reqs = parser.getRequiredCapabilities();
		assertEquals(2, reqs.length);
		assertEquals("testNamespace1", ((IRequiredCapability) reqs[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) reqs[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3, 2)"), ((IRequiredCapability) reqs[0].getMatches()).getRange());
		assertEquals(true, ((IRequiredCapability) reqs[0].getMatches()).isGreedy());
		assertEquals(0, reqs[0].getMin());
		assertEquals("testNamespace2", ((IRequiredCapability) reqs[1].getMatches()).getNamespace());
		assertEquals("testName2", ((IRequiredCapability) reqs[1].getMatches()).getName());
		assertEquals(new VersionRange(Version.emptyVersion.toString()), ((IRequiredCapability) reqs[1].getMatches()).getRange());
		assertEquals(false, reqs[1].isGreedy());
		assertEquals(1, reqs[1].getMin());
	}

	public void testMetaRequiresAdvice() {
		Map map = new HashMap();
		map.put("metaRequirements.0.namespace", "testNamespace1");
		map.put("metaRequirements.0.name", "testName1");
		map.put("metaRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("metaRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("metaRequirements.0.optional", Boolean.TRUE.toString());
		map.put("metaRequirements.0.multiple", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IRequirement[] reqs = parser.getMetaRequiredCapabilities();
		assertEquals(1, reqs.length);
		assertEquals("testNamespace1", ((IRequiredCapability) reqs[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) reqs[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3.v20090909, 2)"), ((IRequiredCapability) reqs[0].getMatches()).getRange());

		map.put("metaRequirements.1.namespace", "testNamespace2");
		map.put("metaRequirements.1.name", "testName2");
		map.put("metaRequirements.1.range", "$version$");
		map.put("metaRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("metaRequirements.1.optional", Boolean.FALSE.toString());
		//default 
		//		map.put("requires.1.multiple", Boolean.FALSE.toString());

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		reqs = parser.getMetaRequiredCapabilities();
		assertEquals(2, reqs.length);
		assertEquals("testNamespace1", ((IRequiredCapability) reqs[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) reqs[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3, 2)"), ((IRequiredCapability) reqs[0].getMatches()).getRange());
		assertEquals(true, ((IRequiredCapability) reqs[0].getMatches()).isGreedy());
		assertEquals(0, reqs[0].getMin());
		assertEquals("testNamespace2", ((IRequiredCapability) reqs[1].getMatches()).getNamespace());
		assertEquals("testName2", ((IRequiredCapability) reqs[1].getMatches()).getName());
		assertEquals(new VersionRange(Version.emptyVersion.toString()), ((IRequiredCapability) reqs[1].getMatches()).getRange());
		assertEquals(false, reqs[1].isGreedy());
		assertEquals(1, reqs[1].getMin());
	}

	public void testInstructionsAdvice() {
		Map map = new HashMap();
		map.put("instructions.configure", "addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);");

		map.put("instructions.unconfigure", "removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)");
		map.put("instructions.unconfigure.import", "some.removeProgramArg");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		ITouchpointInstruction configure = (ITouchpointInstruction) parser.getTouchpointInstructions().get("configure");
		assertEquals(null, configure.getImportAttribute());
		assertEquals("addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);", configure.getBody());

		ITouchpointInstruction unconfigure = (ITouchpointInstruction) parser.getTouchpointInstructions().get("unconfigure");
		assertEquals("some.removeProgramArg", unconfigure.getImportAttribute());
		assertEquals("removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)", unconfigure.getBody());
	}

	public void testAdditionalInstallableUnitDescriptionsAdvice() {
		Map map = new HashMap();
		map.put("units.0.id", "testid0");
		map.put("units.0.version", "1.2.3");

		map.put("units.1.id", "testid1");
		map.put("units.1.version", "1.2.4");
		map.put("units.1.singleton", "true");
		map.put("units.1.copyright", "testCopyright");
		map.put("units.1.copyright.location", "http://localhost/test");
		map.put("units.1.filter", "test=testFilter");
		map.put("units.1.touchpoint.id", "testTouchpointId");
		map.put("units.1.touchpoint.version", "1.2.5");
		map.put("units.1.update.id", "testid1");
		map.put("units.1.update.range", "(1,2)");
		map.put("units.1.update.severity", "2");
		map.put("units.1.update.description", "some description");
		map.put("units.1.artifacts.0.id", "testArtifact1");
		map.put("units.1.artifacts.0.version", "1.2.6");
		map.put("units.1.artifacts.0.classifier", "testClassifier1");
		map.put("units.1.artifacts.1.id", "testArtifact2");
		map.put("units.1.artifacts.1.version", "1.2.7");
		map.put("units.1.artifacts.1.classifier", "testClassifier2");
		map.put("units.1.licenses.0", "testLicense");
		map.put("units.1.licenses.0.location", "http://localhost/license");
		map.put("units.1.properties.0.name", "testName1");
		map.put("units.1.properties.0.value", "testValue1");
		map.put("units.1.properties.1.name", "testName2");
		map.put("units.1.properties.1.value", "testValue2");
		map.put("units.1.requires.0.namespace", "testNamespace1");
		map.put("units.1.requires.0.name", "testName1");
		map.put("units.1.requires.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.requires.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.requires.0.optional", Boolean.TRUE.toString());
		map.put("units.1.requires.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.requires.1.namespace", "testNamespace2");
		map.put("units.1.requires.1.name", "testName2");
		map.put("units.1.requires.1.range", "$version$");
		map.put("units.1.requires.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.requires.1.optional", Boolean.FALSE.toString());
		map.put("units.1.metaRequirements.0.namespace", "testNamespace1");
		map.put("units.1.metaRequirements.0.name", "testName1");
		map.put("units.1.metaRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.metaRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.0.optional", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.1.namespace", "testNamespace2");
		map.put("units.1.metaRequirements.1.name", "testName2");
		map.put("units.1.metaRequirements.1.range", "$version$");
		map.put("units.1.metaRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.metaRequirements.1.optional", Boolean.FALSE.toString());
		map.put("units.1.provides.0.namespace", "testNamespace1");
		map.put("units.1.provides.0.name", "testName1");
		map.put("units.1.provides.0.version", "1.2.3.$qualifier$");
		map.put("units.1.provides.1.namespace", "testNamespace2");
		map.put("units.1.provides.1.name", "testName2");
		map.put("units.1.provides.1.version", "$version$");
		map.put("units.1.instructions.configure", "addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);");
		map.put("units.1.instructions.unconfigure", "removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)");
		map.put("units.1.instructions.unconfigure.import", "some.removeProgramArg");

		map.put("units.1.hostRequirements.0.namespace", "testNamespace1");
		map.put("units.1.hostRequirements.0.name", "testName1");
		map.put("units.1.hostRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.hostRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.0.optional", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.1.namespace", "testNamespace2");
		map.put("units.1.hostRequirements.1.name", "testName2");
		map.put("units.1.hostRequirements.1.range", "$version$");
		map.put("units.1.hostRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.hostRequirements.1.optional", Boolean.FALSE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		InstallableUnitDescription[] descriptions = parser.getAdditionalInstallableUnitDescriptions();
		IInstallableUnit iu0 = MetadataFactory.createInstallableUnit(descriptions[0]);
		assertEquals("testid0", iu0.getId());
		assertEquals(Version.create("1.2.3"), iu0.getVersion());
		assertFalse(iu0.isSingleton());
		assertFalse(iu0.isFragment());
		assertEquals(0, iu0.getArtifacts().length);
		assertEquals(null, iu0.getCopyright());
		assertEquals(null, iu0.getFilter());
		assertEquals(0, iu0.getLicenses().length);
		assertEquals(0, iu0.getProperties().size());
		assertEquals(0, iu0.getRequiredCapabilities().length);
		assertEquals(0, iu0.getProvidedCapabilities().length);
		assertEquals(0, iu0.getMetaRequiredCapabilities().length);
		assertEquals(0, iu0.getTouchpointData().length);
		assertEquals(ITouchpointType.NONE, iu0.getTouchpointType());
		assertEquals(null, iu0.getUpdateDescriptor());

		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(descriptions[1]);
		assertEquals("testid1", iu1.getId());
		assertEquals(Version.create("1.2.4"), iu1.getVersion());
		assertTrue(iu1.isSingleton());
		assertEquals(2, iu1.getArtifacts().length);
		assertEquals("testArtifact1", iu1.getArtifacts()[0].getId());
		assertEquals(Version.create("1.2.6"), iu1.getArtifacts()[0].getVersion());
		assertEquals("testClassifier1", iu1.getArtifacts()[0].getClassifier());
		assertEquals("testArtifact2", iu1.getArtifacts()[1].getId());
		assertEquals(Version.create("1.2.7"), iu1.getArtifacts()[1].getVersion());
		assertEquals("testClassifier2", iu1.getArtifacts()[1].getClassifier());
		assertEquals("testCopyright", iu1.getCopyright().getBody());
		assertEquals("http://localhost/test", iu1.getCopyright().getLocation().toString());
		assertEquals("test=testFilter", ((LDAPQuery) iu1.getFilter()).getFilter());
		assertEquals("testLicense", iu1.getLicenses()[0].getBody());
		assertEquals("http://localhost/license", iu1.getLicenses()[0].getLocation().toString());
		assertEquals("testValue1", iu1.getProperty("testName1"));
		assertEquals("testValue2", iu1.getProperty("testName2"));

		IRequirement[] reqs = iu1.getRequiredCapabilities();
		assertEquals(2, reqs.length);
		assertEquals("testNamespace1", ((IRequiredCapability) reqs[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) reqs[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3, 2)"), ((IRequiredCapability) reqs[0].getMatches()).getRange());
		assertEquals(true, ((IRequiredCapability) reqs[0].getMatches()).isGreedy());
		assertEquals(0, reqs[0].getMin());
		assertEquals("testNamespace2", ((IRequiredCapability) reqs[1].getMatches()).getNamespace());
		assertEquals("testName2", ((IRequiredCapability) reqs[1].getMatches()).getName());
		assertEquals(new VersionRange(Version.emptyVersion.toString()), ((IRequiredCapability) reqs[1].getMatches()).getRange());
		assertEquals(false, reqs[1].isGreedy());
		assertEquals(1, reqs[1].getMin());

		IProvidedCapability[] provided = iu1.getProvidedCapabilities();
		assertEquals(2, provided.length);
		assertEquals("testNamespace1", provided[0].getNamespace());
		assertEquals("testName1", provided[0].getName());
		assertEquals(Version.create("1.2.3"), provided[0].getVersion());
		assertEquals("testNamespace2", provided[1].getNamespace());
		assertEquals("testName2", provided[1].getName());
		assertEquals(Version.emptyVersion, provided[1].getVersion());

		IRequirement[] metarequirements = iu1.getMetaRequiredCapabilities();
		assertEquals(2, metarequirements.length);
		assertEquals("testNamespace1", ((IRequiredCapability) metarequirements[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) metarequirements[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3, 2)"), ((IRequiredCapability) metarequirements[0].getMatches()).getRange());
		assertEquals(true, metarequirements[0].isGreedy());
		assertEquals(0, metarequirements[0].getMin());
		assertEquals("testNamespace2", ((IRequiredCapability) metarequirements[1].getMatches()).getNamespace());
		assertEquals("testName2", ((IRequiredCapability) metarequirements[1].getMatches()).getName());
		assertEquals(new VersionRange(Version.emptyVersion.toString()), ((IRequiredCapability) metarequirements[1].getMatches()).getRange());
		assertEquals(false, metarequirements[1].isGreedy());
		assertEquals(1, metarequirements[1].getMin());

		assertEquals(1, iu1.getTouchpointData().length);
		ITouchpointInstruction configure = iu1.getTouchpointData()[0].getInstruction("configure");
		assertEquals(null, configure.getImportAttribute());
		assertEquals("addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);", configure.getBody());

		ITouchpointInstruction unconfigure = iu1.getTouchpointData()[0].getInstruction("unconfigure");
		assertEquals("some.removeProgramArg", unconfigure.getImportAttribute());
		assertEquals("removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)", unconfigure.getBody());

		assertEquals(MetadataFactory.createTouchpointType("testTouchpointId", Version.create("1.2.5")), iu1.getTouchpointType());
		assertEquals("testid1", iu1.getUpdateDescriptor().getId());
		assertEquals(new VersionRange("(1,2)"), iu1.getUpdateDescriptor().getRange());
		assertEquals(2, iu1.getUpdateDescriptor().getSeverity());
		assertEquals("some description", iu1.getUpdateDescriptor().getDescription());

		assertTrue(iu1.isFragment());
		IRequirement[] hostRequired = ((IInstallableUnitFragment) iu1).getHost();
		assertEquals(2, hostRequired.length);
		assertEquals("testNamespace1", ((IRequiredCapability) hostRequired[0].getMatches()).getNamespace());
		assertEquals("testName1", ((IRequiredCapability) hostRequired[0].getMatches()).getName());
		assertEquals(new VersionRange("[1.2.3, 2)"), ((IRequiredCapability) hostRequired[0].getMatches()).getRange());
		assertEquals(true, hostRequired[0].isGreedy());
		assertEquals(0, hostRequired[0].getMin());
		assertEquals("testNamespace2", ((IRequiredCapability) hostRequired[1].getMatches()).getNamespace());
		assertEquals("testName2", ((IRequiredCapability) hostRequired[1].getMatches()).getName());
		assertEquals(new VersionRange(Version.emptyVersion.toString()), ((IRequiredCapability) hostRequired[1].getMatches()).getRange());
		assertEquals(false, hostRequired[1].isGreedy());
		assertEquals(1, hostRequired[1].getMin());
	}
}