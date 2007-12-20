/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.osgi.framework.Version;

public class EclipseTouchpoint extends Touchpoint {

	private static final TouchpointType TOUCHPOINT_TYPE = new TouchpointType("eclipse", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

	private static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	private static final String ACTION_ADD_JVM_ARG = "addJvmArg"; //$NON-NLS-1$
	private static final String ACTION_ADD_PROGRAM_ARG = "addProgramArg"; //$NON-NLS-1$
	private static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$
	private static final String ACTION_INSTALL_BUNDLE = "installBundle"; //$NON-NLS-1$
	private static final String ACTION_INSTALL_FEATURE = "installFeature"; //$NON-NLS-1$
	private static final String ACTION_MARK_STARTED = "markStarted"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_JVM_ARG = "removeJvmArg"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_PROGRAM_ARG = "removeProgramArg"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_DEPENDENT_PROP = "setFwDependentProp"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_INDEPENDENT_PROP = "setFwIndependentProp"; //$NON-NLS-1$
	private static final String ACTION_UNINSTALL_BUNDLE = "uninstallBundle"; //$NON-NLS-1$
	private static final String ACTION_UNINSTALL_FEATURE = "uninstallFeature"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT = "@artifact"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	private static final String PARM_BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String PARM_FEATURE = "feature"; //$NON-NLS-1$
	private static final String PARM_FEATURE_ID = "featureId"; //$NON-NLS-1$
	private static final String PARM_FEATURE_VERSION = "featureVersion"; //$NON-NLS-1$
	private static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	private static final String PARM_IU = "iu"; //$NON-NLS-1$
	private static final String PARM_JVM_ARG = "jvmArg"; //$NON-NLS-1$
	private static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	private static final String PARM_PLATFORM_CONFIGURATION = "platformConfiguration"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_START_LEVEL = "previousStartLevel"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_STARTED = "previousStarted"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_VALUE = "previousValue"; //$NON-NLS-1$
	private static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	private static final String PARM_PROGRAM_ARG = "programArg"; //$NON-NLS-1$
	private static final String PARM_PROP_NAME = "propName"; //$NON-NLS-1$
	private static final String PARM_PROP_VALUE = "propValue"; //$NON-NLS-1$
	private static final String PARM_SET_START_LEVEL = "setStartLevel"; //$NON-NLS-1$
	private static final String PARM_START_LEVEL = "startLevel"; //$NON-NLS-1$
	private static final String PARM_STARTED = "started"; //$NON-NLS-1$
	private static final String PARM_DEFAULT_VALUE = "default"; //$NON-NLS-1$

	// TODO: phase id constants should be defined elsewhere.
	private static final String INSTALL_PHASE_ID = "install"; //$NON-NLS-1$
	private static final String UNINSTALL_PHASE_ID = "uninstall"; //$NON-NLS-1$

	// private static final String CONFIGURE_PHASE_ID = "configure"; //$NON-NLS-1$
	// private static final String UNCONFIGURE_PHASE_ID = "unconfigure"; //$NON-NLS-1$

	protected static IStatus createError(String message) {
		return createError(message, null);
	}

	protected static IStatus createError(String message, Exception e) {
		return new Status(IStatus.ERROR, Activator.ID, message, e);
	}

	// TODO: Here we may want to consult multiple caches
	IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null || toDownload.length == 0)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRepository aggregatedRepositoryView = Util.getAggregatedBundleRepository(profile);
		IArtifactRepository bundlePool = Util.getBundlePoolRepository(profile);
		List requests = new ArrayList();
		for (int i = 0; i < toDownload.length; i++) {
			IArtifactKey key = toDownload[i];
			if (!aggregatedRepositoryView.contains(key)) {
				Properties repositoryProperties = createArtifactDescriptorProperties(installableUnit);
				requests.add(Util.getArtifactRepositoryManager().createMirrorRequest(key, bundlePool, null, repositoryProperties));
			}
		}

		if (requests.isEmpty())
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRequest[] result = (IArtifactRequest[]) requests.toArray(new IArtifactRequest[requests.size()]);
		return result;
	}

	private Properties createArtifactDescriptorProperties(IInstallableUnit installableUnit) {
		Properties descriptorProperties = null;
		if (isZipped(installableUnit.getTouchpointData())) {
			descriptorProperties = new Properties();
			descriptorProperties.setProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());
		}
		return descriptorProperties;
	}

	boolean isZipped(TouchpointData[] data) {
		if (data == null || data.length == 0)
			return false;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstructions("zipped") != null) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = (Manipulator) touchpointParameters.get(PARM_MANIPULATOR);
		try {
			manipulator.save(false);
		} catch (RuntimeException e) {
			return createError("Error saving manipulator", e);
		} catch (IOException e) {
			return createError("Error saving manipulator", e);
		}

		if (INSTALL_PHASE_ID.equals(phaseId) || UNINSTALL_PHASE_ID.equals(phaseId)) {
			PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) touchpointParameters.get(PARM_PLATFORM_CONFIGURATION);
			try {
				URL configURL = getConfigurationURL(profile);
				configuration.save(configURL);
			} catch (CoreException ce) {
				return createError("Error constructing platform configuration url.", ce);
			} catch (IOException ie) {
				return createError("Error saving platform configuration.", ie);
			}
		}
		return null;
	}

	private URL getConfigurationURL(Profile profile) throws CoreException {
		File configDir = Util.getConfigurationFolder(profile);
		URL configURL = null;
		try {
			configURL = configDir.toURI().toURL();
		} catch (IllegalArgumentException iae) {
			throw new CoreException(createError("Configuration directory is not absolute.", iae));
		} catch (MalformedURLException mue) {
			throw new CoreException(createError("No URL protocol handler.", mue));
		}
		return configURL;
	}

	public ProvisioningAction getAction(String actionId) {
		if (actionId.equals(ACTION_COLLECT)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get(PARM_PROFILE);
					Operand operand = (Operand) parameters.get(PARM_OPERAND);
					IArtifactRequest[] requests = collect(operand.second(), profile);

					Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_REQUESTS);
					artifactRequests.add(requests);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					// nothing to do for now
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_INSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return installBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return uninstallBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_UNINSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return uninstallBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return installBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_INSTALL_FEATURE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return installFeature(parameters);
				}

				public IStatus undo(Map parameters) {
					return uninstallFeature(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_UNINSTALL_FEATURE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return uninstallFeature(parameters);
				}

				public IStatus undo(Map parameters) {
					return installFeature(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_ADD_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);
					if (programArg == null)
						return createError("The \"programArg\" parameter was not set in the \"add program args\" action.");

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

						IArtifactKey[] artifacts = iu.getArtifacts();
						if (artifacts == null || artifacts.length == 0)
							return createError("Installable unit contains no artifacts");

						IArtifactKey artifactKey = artifacts[0];

						File fileLocation = Util.getBundleFile(artifactKey, profile);
						if (fileLocation == null || !fileLocation.exists())
							return createError("The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);
					if (programArg == null)
						return createError("The \"programArg\" parameter was not set in the \"add program args\" action.");

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey[] artifacts = iu.getArtifacts();
						if (artifacts == null || artifacts.length == 0)
							return createError("Installable unit contains no artifacts");

						IArtifactKey artifactKey = artifacts[0];

						File fileLocation = Util.getBundleFile(artifactKey, profile);
						if (fileLocation == null || !fileLocation.exists())
							return createError("The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);
					if (programArg == null)
						return createError("The \"programArg\" parameter was not set in the \"remove program args\" action.");

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey[] artifacts = iu.getArtifacts();
						if (artifacts == null || artifacts.length == 0)
							return createError("Installable unit contains no artifacts");

						IArtifactKey artifactKey = artifacts[0];

						File fileLocation = Util.getBundleFile(artifactKey, profile);;
						if (fileLocation == null || !fileLocation.exists())
							return createError("The artifact for " + artifactKey + " is not available");
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);
					if (programArg == null)
						return createError("The \"programArg\" parameter was not set in the \"remove program args\" action.");

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey[] artifacts = iu.getArtifacts();
						if (artifacts == null || artifacts.length == 0)
							return createError("Installable unit contains no artifacts");

						IArtifactKey artifactKey = artifacts[0];

						File fileLocation = Util.getBundleFile(artifactKey, profile);
						if (fileLocation == null || !fileLocation.exists())
							return createError("The artifact for " + artifactKey + " is not available");
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(PARM_SET_START_LEVEL)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String startLevel = (String) parameters.get(PARM_START_LEVEL);
					if (startLevel == null)
						return createError("The \"startLevel\" parameter was not set in the \"set start level\" action.");

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_START_LEVEL, new Integer(bundles[i].getStartLevel()));
							try {
								bundles[i].setStartLevel(Integer.parseInt(startLevel));
							} catch (NumberFormatException e) {
								return createError("Error parsing start level: " + startLevel + " for bundle: " + bundles[i].getSymbolicName(), e);
							}
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Integer previousStartLevel = (Integer) getMemento().get(PARM_PREVIOUS_START_LEVEL);
							if (previousStartLevel != null)
								bundles[i].setStartLevel(previousStartLevel.intValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_MARK_STARTED)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String started = (String) parameters.get(PARM_STARTED);
					if (started == null)
						return createError("The \"started\" parameter was not set in the \"mark started\" action.");

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_STARTED, new Boolean(bundles[i].isMarkedAsStarted()));
							bundles[i].setMarkedAsStarted(Boolean.valueOf(started).booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Boolean previousStarted = (Boolean) getMemento().get(PARM_PREVIOUS_STARTED);
							if (previousStarted != null)
								bundles[i].setMarkedAsStarted(previousStarted.booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_DEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					if (propName == null)
						return createError("The \"propName\" parameter was not set in the \"set framework dependent properties\" action.");
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					if (propValue == null)
						return createError("The \"propValue\" parameter was not set in the \"set framework dependent properties\" action.");
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwDependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					if (propName == null)
						return createError("The \"propName\" parameter was not set in the \"set framework dependent properties\" action.");
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					if (previousValue == null)
						return createError("The \"propValue\" parameter was not set in the \"set framework dependent properties\" action.");
					manipulator.getConfigData().setFwDependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_INDEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					if (propName == null)
						return createError("The \"propName\" parameter was not set in the \"set framework independent properties\" action.");
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					if (propValue == null)
						return createError("The \"propValue\" parameter was not set in the \"set framework independent properties\" action.");
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwIndependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					if (propName == null)
						return createError("The \"propName\" parameter was not set in the \"set framework independent properties\" action.");
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					if (previousValue == null)
						return createError("The \"propValue\" parameter was not set in the \"set framework independent properties\" action.");
					manipulator.getConfigData().setFwIndependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_ADD_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					if (jvmArg == null)
						return createError("The \"jvmArg\" parameter was not set in the \"add jvm args\" action.");
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					if (jvmArg == null)
						return createError("The \"jvmArg\" parameter was not set in the \"add jvm args\" action.");
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					if (jvmArg == null)
						return createError("The \"jvmArg\" parameter was not set in the \"remove jvm args\" action.");
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					if (jvmArg == null)
						return createError("The \"jvmArg\" parameter was not set in the \"remove jvm args\" action.");
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		return null;
	}

	public TouchpointType getTouchpointType() {
		//TODO this data probably needs to come from the XML
		return TOUCHPOINT_TYPE;
	}

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, Util.getInstallFolder(profile));
		touchpointParameters.put(PARM_MANIPULATOR, new LazyManipulator(profile));
		try {
			URL configURL = getConfigurationURL(profile);
			URL poolURL = Util.getBundlePoolLocation(profile);
			touchpointParameters.put(PARM_PLATFORM_CONFIGURATION, new PlatformConfigurationWrapper(configURL, poolURL));
		} catch (CoreException ce) {
			touchpointParameters.put(PARM_PLATFORM_CONFIGURATION, new PlatformConfigurationWrapper(null, null));
			return createError("Error constructing platform configuration url.", ce);
		}
		return null;
	}

	IStatus installBundle(Map parameters) {

		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);
		if (bundleId == null)
			return createError("The \"bundleId\" parameter is missing from the \"install bundle\" action");

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return createError("Installable unit contains no artifacts");

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		File bundleFile = Util.getBundleFile(artifactKey, profile);
		if (bundleFile == null || !bundleFile.exists())
			return createError("The artifact " + artifactKey.toString() + " to install was not found.");

		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = Util.getManifest(iu.getTouchpointData());
		if (manifest == null)
			return createError("The manifest is missing for: " + iu.getTouchpointData());

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
	}

	protected IStatus uninstallBundle(Map parameters) {
		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);
		if (bundleId == null)
			return createError("The \"bundleId\" parameter is missing from the \"uninstall bundle\" action.");

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return createError("Installable unit contains no artifacts");

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		File bundleFile = Util.getBundleFile(artifactKey, profile);
		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = Util.getManifest(iu.getTouchpointData());
		if (manifest == null)
			return createError("The manifest is missing for: " + iu.getTouchpointData());

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().removeBundle(bundleInfo);

		return Status.OK_STATUS;
	}

	IStatus installFeature(Map parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) parameters.get(PARM_PLATFORM_CONFIGURATION);
		String feature = (String) parameters.get(PARM_FEATURE);
		String featureId = (String) parameters.get(PARM_FEATURE_ID);
		String featureVersion = (String) parameters.get(PARM_FEATURE_VERSION);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return createError("Installable unit for eclipse feature contains no artifacts");

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(feature)) {
				artifactKey = artifacts[i];
				break;
			}
		}

		if (featureId == null)
			return createError("The \"featureId\" parameter is missing from the \"install feature\" action"); //$NON-NLS-1$
		else if (PARM_DEFAULT_VALUE.equals(featureId)) {
			featureId = artifactKey.getId();
		}

		if (featureVersion == null)
			return createError("The \"featureVersion\" parameter is missing from the \"install feature\" action"); //$NON-NLS-1$
		else if (PARM_DEFAULT_VALUE.equals(featureVersion)) {
			featureVersion = artifactKey.getVersion().toString();
		}

		return configuration.addFeatureEntry(featureId, featureVersion, artifactKey.getId(), artifactKey.getVersion().toString(), /*primary*/false, /*application*/null, /*root*/null);
	}

	protected IStatus uninstallFeature(Map parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) parameters.get(PARM_PLATFORM_CONFIGURATION);
		String feature = (String) parameters.get(PARM_FEATURE);
		String featureId = (String) parameters.get(PARM_FEATURE_ID);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return createError("Installable unit for eclipse feature contains no artifacts");

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(feature)) {
				artifactKey = artifacts[i];
				break;
			}
		}

		if (featureId == null)
			return createError("The \"featureId\" parameter is missing from the \"uninstall feature\" action"); //$NON-NLS-1$
		else if (PARM_DEFAULT_VALUE.equals(featureId)) {
			featureId = artifactKey.getId();
		}

		return configuration.removeFeatureEntry(featureId);
	}
}
